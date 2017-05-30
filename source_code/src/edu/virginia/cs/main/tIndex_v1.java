/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.main;

import edu.virginia.cs.config.DeploymentConfig;
import edu.virginia.cs.config.RunTimeConfig;
import edu.virginia.cs.model.ClassifyIntent;
import edu.virginia.cs.object.Session;
import edu.virginia.cs.object.UserQuery;
import edu.virginia.cs.user.Intent;
import edu.virginia.cs.user.Profile;
import edu.virginia.cs.utility.SortMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wua4nw
 */
public class tIndex_v1 {

    private final int interval_in_hours;
    private final HashMap<String, Double> transitionProbMap;
    private String[] previous_topic_path;
    private final HashMap<String, Integer> map;
    private final ClassifyIntent classifyIntent;

    public tIndex_v1(int param) {
        this.interval_in_hours = param;
        this.transitionProbMap = new HashMap<>();
        doInitialization();

        map = new HashMap<>();
        map.put("initial_state", 0);
        map.put("up1", 1);
        map.put("up2", 2);
        map.put("down1", 3);
        map.put("down2", 4);
        map.put("same", 5);
        map.put("same_parent", 6);
        map.put("same_grandparent", 7);
        map.put("others", 8);

        classifyIntent = new ClassifyIntent();
    }

    private void doInitialization() {
        BufferedReader br = null;
        try {
            /* load background knowledge from file */
            br = new BufferedReader(new FileReader(new File(DeploymentConfig.TransitionMatrix)));
            String line;
            boolean flag = true;
            double totalQuery = 0;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    flag = true;
                    totalQuery = 0;
                    continue;
                }
                if (flag) {
                    totalQuery = Double.valueOf(line);
                    line = br.readLine();
                    flag = false;
                }
                String[] split = line.split("\\s+");
                double prob = Double.valueOf(split[3]) / totalQuery;
                transitionProbMap.put(split[0] + "/" + split[2], prob);
            }
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(tIndex_v1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(tIndex_v1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    public double evaluateTransitions(Profile userProf) {
        long difference_in_hours = 0;
        Date lastSessionEndTime = null;
        ArrayList<Session> search_sessions = new ArrayList<>();
        double totalRankingScore = 0;
        int totalScoreCount = 0;
        for (Session session : userProf.getSessions()) {
            if (lastSessionEndTime != null) {
                difference_in_hours = getDateDiff(lastSessionEndTime, session.getStart_time(), TimeUnit.HOURS);
                if (difference_in_hours > interval_in_hours) {
                    /* compute goodness of alignment */
                    if (!search_sessions.isEmpty()) {
                        double rankingScore = computeRanking(search_sessions);
                        totalRankingScore += rankingScore;
                        totalScoreCount++;
                    }
                    /* start of a new interval, reset everything */
                    lastSessionEndTime = session.getStart_time();
                    search_sessions = new ArrayList<>();
                    previous_topic_path = new String[]{"Top"};
                }
                search_sessions.add(session);
            } else {
                lastSessionEndTime = session.getStart_time();
                previous_topic_path = new String[]{"Top"};
            }
        }

        if (totalScoreCount > 0) {
            return totalRankingScore / totalScoreCount;
        } else {
            return totalRankingScore;
        }
    }

    private double computeRanking(ArrayList<Session> search_sessions) {
        double rankingScore = 0.0;
        HashMap<String, Double> scoreMap = score(search_sessions);
        Map<String, Double> sortedScoreMap = SortMap.sortMapByValue(scoreMap, false);

        int coverSeqFound = 0;
        int trueSeqFound = 0;
        for (Map.Entry<String, Double> entry : sortedScoreMap.entrySet()) {
            if (entry.getKey().contains("cover_sequence")) {
                coverSeqFound++;
            } else if (entry.getKey().contains("true_sequence")) {
                rankingScore += coverSeqFound - trueSeqFound;
                trueSeqFound++;
            }
        }

        /* compute ranking score */
        int maxScore = 0;
        for (int i = 0; i < trueSeqFound; i++) {
            maxScore += coverSeqFound - i;
        }
        int minScore = 0;
        for (int i = 0; i < trueSeqFound; i++) {
            minScore -= i;
        }

        if (maxScore != 0 || minScore != 0) {
            rankingScore -= minScore;
            rankingScore = rankingScore / (maxScore - minScore);
        }

        return rankingScore;
    }

    private HashMap<String, Double> score(ArrayList<Session> search_sessions) {
        HashMap<String, Double> scoreMap = new HashMap<>();
        int session_index = 0;
        for (Session session : search_sessions) {
            String previous_transition_status = "initial_state";
            ArrayList<String> previous_cover_status = new ArrayList<>();

            double true_score = 0;
            ArrayList<Double> cover_scores = new ArrayList<>();
            int num_transitions = 0;
            for (UserQuery query : session.getUser_queries()) {
                String transition_status = getTransitionStatus(query);
                if (transition_status == null) {
                    continue;
                }
                if (query.getCover_queries().size() != RunTimeConfig.NumberOfCoverQuery) {
                    continue;
                }

                Double refProb = transitionProbMap.get(previous_transition_status + "/" + transition_status);
                if (refProb == null) {
                    continue;
                } else if (refProb != 0) {
                    true_score += Math.log(refProb);
                }

                ArrayList<String> current_cover_status = new ArrayList<>();
                int index = 0;
                for (UserQuery coverQuery : query.getCover_queries()) {
                    String cover_status = getTransitionStatus(coverQuery);
                    if (cover_status == null) {
                        continue;
                    }

                    if (previous_cover_status.isEmpty()) {
                        if (cover_scores.size() <= index) {
                            Double temp = transitionProbMap.get("initial_state" + "/" + cover_status);
                            if (temp == null) {
                                System.out.println("initial_state" + "/" + cover_status);
                                System.exit(1);
                            }
                            cover_scores.add(Math.log(temp));
                        } else {
                            double cover_score = cover_scores.get(index);
                            cover_score += Math.log(transitionProbMap.get("initial_state" + "/" + cover_status));
                            cover_scores.set(index, cover_score);
                        }
                    } else if (previous_cover_status.size() > index) {
                        if (cover_scores.size() <= index) {
                            cover_scores.add(Math.log(transitionProbMap.get(previous_cover_status.get(index) + "/" + cover_status)));
                        } else {
                            double cover_score = cover_scores.get(index);
                            cover_score += Math.log(transitionProbMap.get(previous_cover_status.get(index) + "/" + cover_status));
                            cover_scores.set(index, cover_score);
                        }
                    }

                    current_cover_status.add(cover_status);
                    index++;
                }

                previous_transition_status = transition_status;
                previous_cover_status = new ArrayList<>(current_cover_status);
                num_transitions++;
            }

            if (num_transitions != 0) {
                scoreMap.put("true_sequence_" + session_index, true_score);
                for (int i = 0; i < cover_scores.size(); i++) {
                    if (scoreMap.containsKey("cover_sequence_" + session_index + "_" + i)) {
                        scoreMap.put("cover_sequence_" + session_index + "_" + i, scoreMap.get("cover_sequence_" + session_index + "_" + i) + (cover_scores.get(i) / num_transitions));
                    } else {
                        scoreMap.put("cover_sequence_" + session_index + "_" + i, (cover_scores.get(i) / num_transitions));
                    }
                }
            }
            session_index++;
        }
        return scoreMap;
    }

    private String getTransitionStatus(UserQuery query) {
        if (query.getQuery_intent() == null) {
            return null;
        }
        Intent tempI = classifyIntent.inferQueryIntent(query);
        if (tempI == null) {
            return null;
        }

        String[] split = tempI.getName().split("/");
        String transition_status;

        if (Arrays.equals(split, previous_topic_path)) {
            transition_status = "same";
        } else if (split.length == previous_topic_path.length) { // same level, 3 cases possible
            if (split.length > 1 && split[split.length - 2].equals(previous_topic_path[split.length - 2])) { // share same parent
                transition_status = "same_parent";
            } else if (split.length > 2 && split[split.length - 3].equals(previous_topic_path[split.length - 3])) { // share same grand parent
                transition_status = "same_grandparent";
            } else { // others
                transition_status = "others";
            }
        } else if (split.length == previous_topic_path.length - 1 && Arrays.equals(split, Arrays.copyOfRange(previous_topic_path, 0, previous_topic_path.length - 1))) { // up 1 step
            transition_status = "up1";
        } else if (split.length == previous_topic_path.length - 2 && Arrays.equals(split, Arrays.copyOfRange(previous_topic_path, 0, previous_topic_path.length - 2))) { // up 2 step
            transition_status = "up2";
        } else if (split.length - 1 == previous_topic_path.length && Arrays.equals(previous_topic_path, Arrays.copyOfRange(split, 0, split.length - 1))) { // down 1 step
            transition_status = "down1";
        } else if (split.length - 2 == previous_topic_path.length && Arrays.equals(previous_topic_path, Arrays.copyOfRange(split, 0, split.length - 2))) { // down 2 step
            transition_status = "down2";
        } else { // others
            transition_status = "others";
        }

        previous_topic_path = split;
        return transition_status;
    }

}
