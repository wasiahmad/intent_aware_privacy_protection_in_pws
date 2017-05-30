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
public class tIndex_v2 {

    private final int interval_in_hours;
    private final HashMap<String, Double> transitionProbMap;
    private final ClassifyIntent classifyIntent;

    public tIndex_v2(int param) {
        this.interval_in_hours = param;
        this.transitionProbMap = new HashMap<>();
        doInitialization();
        classifyIntent = new ClassifyIntent();
    }

    private void doInitialization() {
        BufferedReader br = null;
        try {
            /* load background knowledge from file */
            br = new BufferedReader(new FileReader(new File(DeploymentConfig.TransitionProbability)));
            String line;
            double totalQuery = 0;
            boolean flag = false;
            while ((line = br.readLine()) != null) {
                if (!flag) {
                    flag = true;
                    totalQuery = Double.valueOf(line);
                } else {
                    String[] split = line.split("\t");
                    double prob = Double.valueOf(split[1]) / totalQuery;
                    transitionProbMap.put(split[0], prob);
                }
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
                }
                search_sessions.add(session);
            } else {
                lastSessionEndTime = session.getStart_time();
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
            if (session.getUser_queries().size() < 2) {
                /* Score can't be computed if session contains less than 2 user queries. */
                continue;
            }

            double true_score = 0;
            ArrayList<Double> cover_scores = new ArrayList<>();
            int num_transitions = 0;

            Intent lastQueryIntent = null;
            ArrayList<Intent> previous_cover_intents = new ArrayList<>();
            for (UserQuery query : session.getUser_queries()) {
                if (query.getCover_queries().size() != RunTimeConfig.NumberOfCoverQuery) {
                    continue;
                }

                ArrayList<UserQuery> coverQueries = query.getCover_queries();

                Intent currentQueryIntent = classifyIntent.inferQueryIntent(query);
                if (currentQueryIntent == null) {
                    continue;
                }

                if (lastQueryIntent == null || previous_cover_intents.isEmpty()) {
                    for (int index = 0; index < coverQueries.size(); index++) {
                        Intent tempQIntent = classifyIntent.inferQueryIntent(coverQueries.get(index));
                        if (tempQIntent == null) {
                            previous_cover_intents = new ArrayList<>();
                            break;
                        }
                        previous_cover_intents.add(tempQIntent);
                    }
                    lastQueryIntent = currentQueryIntent;
                    continue;
                }

                double transition_prob = getTransitionProbability(currentQueryIntent, lastQueryIntent);
                true_score += Math.log(transition_prob);

                for (int index = 0; index < coverQueries.size(); index++) {
                    Intent coverQIntent = classifyIntent.inferQueryIntent(coverQueries.get(index));
                    if (coverQIntent == null) {
                        continue;
                    }

                    double cover_transition_prob = getTransitionProbability(coverQIntent, previous_cover_intents.get(index));
                    if (cover_scores.size() <= index) {
                        cover_scores.add(Math.log(cover_transition_prob));
                    } else {
                        double cover_score = cover_scores.get(index);
                        cover_score += Math.log(cover_transition_prob);
                        cover_scores.set(index, cover_score);
                    }
                }

                lastQueryIntent = currentQueryIntent;
                num_transitions++;
            }

            if (num_transitions != 0 && !cover_scores.isEmpty()) {
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

    private double getTransitionProbability(Intent currentIntent, Intent previousIntent) {
        String[] currTopic = currentIntent.getName().split("/");
        String[] prevTopic = previousIntent.getName().split("/");

        if (Arrays.equals(currTopic, prevTopic)) {
            return transitionProbMap.get("same");
        } else if (currTopic.length == prevTopic.length) { // same level, 3 cases possible
            if (currTopic.length > 1 && currTopic[currTopic.length - 2].equals(prevTopic[currTopic.length - 2])) { // share same parent
                return transitionProbMap.get("same_parent");
            } else if (currTopic.length > 2 && currTopic[currTopic.length - 3].equals(prevTopic[currTopic.length - 3])) { // share same grand parent
                return transitionProbMap.get("same_grandparent");
            } else { // others
                return transitionProbMap.get("others");
            }
        } else if (currTopic.length == prevTopic.length - 1 && Arrays.equals(currTopic, Arrays.copyOfRange(prevTopic, 0, prevTopic.length - 1))) { // up 1 step
            return transitionProbMap.get("up1");
        } else if (currTopic.length == prevTopic.length - 2 && Arrays.equals(currTopic, Arrays.copyOfRange(prevTopic, 0, prevTopic.length - 2))) { // up 2 step
            return transitionProbMap.get("up2");
        } else if (currTopic.length - 1 == prevTopic.length && Arrays.equals(prevTopic, Arrays.copyOfRange(currTopic, 0, currTopic.length - 1))) { // down 1 step
            return transitionProbMap.get("down1");
        } else if (currTopic.length - 2 == prevTopic.length && Arrays.equals(prevTopic, Arrays.copyOfRange(currTopic, 0, currTopic.length - 2))) { // down 2 step
            return transitionProbMap.get("down2");
        } else { // others
            return transitionProbMap.get("others");
        }
    }
}
