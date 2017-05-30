/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.extra;

import edu.virginia.cs.config.StaticData;
import edu.virginia.cs.object.ResultDoc;
import edu.virginia.cs.utility.Converter;
import edu.virginia.cs.utility.SortMap;
import edu.virginia.cs.utility.TextTokenizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

/**
 *
 * @author wua4nw
 */
public class Personalization {

    private static final TextTokenizer TOKENIZER = new TextTokenizer(true, true);

    public static ResultDoc[] personalizeResults(IndexSearcher indexSearcher, String field, HashMap<String, Integer> history, ScoreDoc[] relDocs) {
        ResultDoc[] origDocs = Converter.convertToResultDoc(relDocs, indexSearcher, field);
        ResultDoc[] reRankedDocs = new ResultDoc[origDocs.length];

        HashMap<String, Float> mapDocToScore = new HashMap<>();
        HashMap<String, Integer> uniqueDocTerms;

        for (int i = 0; i < origDocs.length; i++) {
            /**
             * Extract the unique tokens from a relevant document returned by
             * the lucene index searcher.
             */
            uniqueDocTerms = new HashMap<>();
            List<String> tokens = TOKENIZER.TokenizeText(origDocs[i].getContent());
            // computing term frequency of all the unique terms found in the document
            for (String tok : tokens) {
                if (uniqueDocTerms.containsKey(tok)) {
                    uniqueDocTerms.put(tok, uniqueDocTerms.get(tok) + 1);
                } else {
                    uniqueDocTerms.put(tok, 1);
                }
            }

            /* Score after personalizing result */
            float score = 0;
            /* Smoothing paramter for linear interpolation */
            float lambda = 0.1f;

            /**
             * Computing score for a returned document based on user profile
             * maintained by the server side.
             */
            for (Map.Entry<String, Integer> entry : history.entrySet()) {
                if (!uniqueDocTerms.containsKey(entry.getKey())) {
                    continue;
                }
                Double tokenProb = (uniqueDocTerms.get(entry.getKey()) * 1.0) / tokens.size();
                Double refProb = StaticData.SmoothingReference.get(entry.getKey());
                if (refProb == null) {
                    refProb = 0.0;
                }
                /* Smoothing using linear interpolation */
                Double smoothedTokenProb = (1 - lambda) * tokenProb + lambda * refProb;
                smoothedTokenProb = smoothedTokenProb / (lambda * refProb);
                score = score + (entry.getValue() * (float) Math.log(smoothedTokenProb));
            }
            mapDocToScore.put(String.valueOf(i), score);
            /**
             * Storing the score of documents computed through server side user
             * profile.
             */
            origDocs[i].setPersonalizationScore(score);
        }
        Map<String, Float> tempMap = SortMap.sortMapByValue(mapDocToScore, false);
        int i = 0;
        for (Map.Entry<String, Float> entry : tempMap.entrySet()) {
            float score = 0;
            // Giving 25% weight to personalization and 75% to OkapiBM25.
            if (entry.getValue() == 0) {
                score = 0.5f * (1.0f / (Integer.parseInt(entry.getKey()) + 1));
            } else {
                score = 0.5f * (1.0f / (i + 1)) + 0.5f * (1.0f / (Integer.parseInt(entry.getKey()) + 1));
            }
            mapDocToScore.put(entry.getKey(), score);
            i++;
        }
        Map<String, Float> resultedMap = SortMap.sortMapByValue(mapDocToScore, false);
        i = 0;
        for (Map.Entry<String, Float> entry : resultedMap.entrySet()) {
            reRankedDocs[i] = origDocs[Integer.parseInt(entry.getKey())];
            i++;
        }

        return reRankedDocs;
    }

}
