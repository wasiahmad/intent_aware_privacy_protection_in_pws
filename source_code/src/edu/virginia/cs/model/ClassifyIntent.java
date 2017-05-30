/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.model;

import edu.virginia.cs.config.DeploymentConfig;
import edu.virginia.cs.engine.OkapiBM25;
import edu.virginia.cs.engine.Searcher;
import edu.virginia.cs.object.ResultDoc;
import edu.virginia.cs.object.UserQuery;
import edu.virginia.cs.user.Intent;
import edu.virginia.cs.utility.SortMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author wua4nw
 */
public class ClassifyIntent {

    private final Searcher SEARCHER;

    public ClassifyIntent() {
        this.SEARCHER = new Searcher(DeploymentConfig.OdpIndexPath);
        this.SEARCHER.setSimilarity(new OkapiBM25());
    }

    /**
     * Returns the best topic that characterize the given query.
     *
     * @param tokens
     * @param n size of the query
     * @return
     */
    private String getTopic(String queryText) {
        String bestTopic = null;
        ArrayList<ResultDoc> results = SEARCHER.search(queryText).getDocs();
        if (!results.isEmpty()) {
            HashMap<String, Double> topicMap = new HashMap<>();
            int rank = 1;
            for (ResultDoc rdoc : results) {
                double score = ((int) ((1.0 / rank) * 100)) / 100.0;
                if (topicMap.containsKey(rdoc.getTopic())) {
                    score += topicMap.get(rdoc.getTopic());
                    score = ((int) (score * 100)) / 100.0;
                    topicMap.put(rdoc.getTopic(), score);
                } else {
                    topicMap.put(rdoc.getTopic(), score);
                }
                rank++;
            }
            for (Map.Entry<String, Double> entry : SortMap.sortMapByValue(topicMap, false, 1).entrySet()) {
                bestTopic = entry.getKey();
            }
        }
        return bestTopic;
    }

    public Intent inferQueryIntent(UserQuery query) {
        String inferred_topic_name = getTopic(query.getQuery_text());
        if (inferred_topic_name == null) {
            /* if topic of the query can not be inferred */
            return null;
        }
        return new Intent(inferred_topic_name);
    }
}
