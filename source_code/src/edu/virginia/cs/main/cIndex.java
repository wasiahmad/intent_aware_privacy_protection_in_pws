/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.main;

import edu.virginia.cs.config.DeploymentConfig;
import edu.virginia.cs.object.UserQuery;
import edu.virginia.cs.user.Profile;
import edu.virginia.cs.utility.SortMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
public class cIndex {

    private final int interval_in_hours;
    private final HashMap<String, Double> queryIntentMapping;
    private double totalQueries;
    private final double smoothing_factor = 0.001;

    public cIndex(int param) {
        this.interval_in_hours = param;
        this.queryIntentMapping = new HashMap<>();
        this.totalQueries = 0;
        doInitialization();
    }

    private void doInitialization() {
        BufferedReader br = null;
        try {
            /* load background knowledge from file */
            br = new BufferedReader(new FileReader(new File(DeploymentConfig.BackgroundKnowledge)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\t");
                int count = Integer.valueOf(split[1]);
                totalQueries += count;
                queryIntentMapping.put(split[0], count * smoothing_factor);
            }
            totalQueries = smoothing_factor * totalQueries;
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(cIndex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(cIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    public double evaluateComponents(Profile userProf) {
        long difference_in_hours = 0;
        Date lastQueryTime = null;
        ArrayList<UserQuery> queries = new ArrayList<>();
        ArrayList<UserQuery> coverQueries = new ArrayList<>();
        double totalRankingScore = 0;
        int totalScoreCount = 0;
        for (UserQuery query : userProf.getSubmittedQueries()) {
            if (lastQueryTime != null) {
                difference_in_hours = getDateDiff(lastQueryTime, query.getQuery_time(), TimeUnit.HOURS);
                if (difference_in_hours > interval_in_hours) {
                    /* compute goodness of alignment */
                    if (!queries.isEmpty() && !coverQueries.isEmpty()) {
                        double rankingScore = computeRanking(queries, coverQueries);
                        totalRankingScore += rankingScore;
                        totalScoreCount++;
                    }
                    /* start of a new interval, reset everything */
                    lastQueryTime = query.getQuery_time();
                    queries = new ArrayList<>();
                    coverQueries = new ArrayList<>();
                }
                queries.add(query);
                coverQueries.addAll(query.getCover_queries());
            } else {
                lastQueryTime = query.getQuery_time();
            }
        }

        if (totalScoreCount > 0) {
            return totalRankingScore / totalScoreCount;
        } else {
            return totalRankingScore;
        }
    }

    private double computeRanking(ArrayList<UserQuery> userQueries, ArrayList<UserQuery> coverQueries) {
        double rankingScore = 0.0;
        HashMap<String, Double> scoreMap = new HashMap<>();

        ArrayList<HashMap<String, Integer>> trueComponents = getConnectedComponents(userQueries);
        int index = 0;
        for (HashMap<String, Integer> component : trueComponents) {
            double score = scoreComponent(component);
            scoreMap.put("true_component_" + index, score);
            index++;
        }

        ArrayList<HashMap<String, Integer>> coverComponents = getConnectedComponents(coverQueries);
        index = 0;
        for (HashMap<String, Integer> component : coverComponents) {
            double score = scoreComponent(component);
            scoreMap.put("cover_component_" + index, score);
            index++;
        }
        Map<String, Double> sortedScoreMap = SortMap.sortMapByValue(scoreMap, false);

        /* compute ranking score */
        int maxScore = 0;
        for (int i = 0; i < trueComponents.size(); i++) {
            maxScore += coverComponents.size() - i;
        }
        int minScore = 0;
        for (int i = 0; i < trueComponents.size(); i++) {
            minScore -= i;
        }

        int coverComponentFound = 0;
        int trueComponentFound = 0;
        for (Map.Entry<String, Double> entry : sortedScoreMap.entrySet()) {
            if (entry.getKey().contains("cover_component")) {
                coverComponentFound++;
            } else if (entry.getKey().contains("true_component")) {
                rankingScore += coverComponentFound - trueComponentFound;
                trueComponentFound++;
            }
        }

        if (maxScore != 0 || minScore != 0) {
            rankingScore -= minScore;
            rankingScore = rankingScore / (maxScore - minScore);
        }

        /* update query intent mapping */
        for (HashMap<String, Integer> component : trueComponents) {
            updateQueryIntentMap(component);
        }
        for (HashMap<String, Integer> component : coverComponents) {
            updateQueryIntentMap(component);
        }
        return rankingScore;
    }

    private double scoreComponent(HashMap<String, Integer> component) {
        double score = 0.0;
        double totalQueryCount = 0;
        for (Map.Entry<String, Integer> member : component.entrySet()) {
            totalQueryCount += member.getValue();
        }
        for (Map.Entry<String, Integer> member : component.entrySet()) {
            if (queryIntentMapping.containsKey(member.getKey())) {
                double freq = queryIntentMapping.get(member.getKey());
                double probability1 = freq / totalQueries;
                double entropy1 = -probability1 * Math.log(probability1);
                double probability2 = (freq + member.getValue()) / (totalQueries + totalQueryCount);
                double entropy2 = -probability2 * Math.log(probability2);
                score += (entropy2 - entropy1);
            }
        }
        /* normalize the score by component size */
        score = score / component.size();
        return score;
    }

    private void updateQueryIntentMap(HashMap<String, Integer> component) {
        for (Map.Entry<String, Integer> member : component.entrySet()) {
            if (queryIntentMapping.containsKey(member.getKey())) {
                queryIntentMapping.put(member.getKey(), queryIntentMapping.get(member.getKey()) + member.getValue());
                totalQueries += member.getValue();
            }
        }
    }

    private ArrayList<HashMap<String, Integer>> getConnectedComponents(ArrayList<UserQuery> userQueries) {
        ArrayList<HashMap<String, Integer>> connectedComponents = new ArrayList<>();
        for (UserQuery query : userQueries) {
            boolean isMember = false;
            for (HashMap<String, Integer> component : connectedComponents) {
                if (component.containsKey(query.getQuery_intent().getName())) {
                    component.put(query.getQuery_intent().getName(), component.get(query.getQuery_intent().getName()) + 1);
                    isMember = true;
                    break;
                } else {
                    isMember = checkMembership(component, query.getQuery_intent().getName());
                    if (isMember) {
                        component.put(query.getQuery_intent().getName(), 1);
                        break;
                    }
                }
            }
            if (!isMember) {
                HashMap<String, Integer> component = new HashMap<>();
                component.put(query.getQuery_intent().getName(), 1);
                connectedComponents.add(component);
            }
        }
        return connectedComponents;
    }

    private boolean checkMembership(HashMap<String, Integer> component, String element) {
        for (Map.Entry<String, Integer> member : component.entrySet()) {
            int length1 = member.getKey().split("/").length;
            int length2 = element.split("/").length;
            if (length1 - length2 == 1 && member.getKey().contains(element)) {
                return true;
            } else if (length2 - length1 == 1 && element.contains(member.getKey())) {
                return true;
            }
        }
        return false;
    }

}
