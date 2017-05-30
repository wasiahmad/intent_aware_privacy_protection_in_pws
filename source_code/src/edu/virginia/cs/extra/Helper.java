/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.extra;

import edu.virginia.cs.config.StaticData;
import edu.virginia.cs.object.ResultDoc;
import edu.virginia.cs.object.Session;
import edu.virginia.cs.object.UserQuery;
import edu.virginia.cs.utility.SortMap;
import edu.virginia.cs.utility.TextTokenizer;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wua4nw
 */
public class Helper {

    private static final TextTokenizer TOKENIZER = new TextTokenizer(true, true);

    /**
     * Generate a random number within a range.
     *
     * @param min
     * @param max
     * @return
     */
    public static int getRandom(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

    /**
     * Generate random number using poisson distribution.
     *
     * @param lambda average query length
     * @return
     */
    public static int getPoisson(double lambda) {
        int n = 1;
        double prob = 1.0;
        Random r = new Random();

        while (true) {
            prob *= r.nextDouble();
            if (prob < Math.exp(-lambda)) {
                break;
            }
            n += 1;
        }
        return n - 1;
    }

    /**
     *
     *
     * @param previousQuery
     * @param currentQuery
     * @return
     */
    public static boolean checkSameSession(UserQuery previousQuery, UserQuery currentQuery) {
        /**
         * Measuring the time difference between current query and last
         * submitted query in minutes.
         */
        double diffMinutes = 0;
        try {
            //in milliseconds
            long diff = currentQuery.getQuery_time().getTime() - previousQuery.getQuery_time().getTime();
            diffMinutes = diff / (60.0 * 1000);
        } catch (Exception ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        }
        /**
         * If time difference is less than 60 minutes between current query and
         * previous query, they belong to the same session .
         */
        return diffMinutes < 60;
    }

    public static boolean checkSameTask(UserQuery previousQuery, UserQuery currentQuery) {
        /**
         * Measuring the similarity between current query and last submitted
         * query in terms of similar tokens they have.
         */
        HashSet<String> currentQuTokens = new HashSet<>(TOKENIZER.TokenizeText(currentQuery.getQuery_text()));
        HashSet<String> prevQuTokens = new HashSet<>(TOKENIZER.TokenizeText(previousQuery.getQuery_text()));
        boolean isSimilar = false;
        int count = (currentQuTokens.size() > prevQuTokens.size() ? prevQuTokens.size() : currentQuTokens.size()) / 2;
        HashSet<String> intersection = new HashSet<>(currentQuTokens);
        intersection.retainAll(prevQuTokens);
        if (intersection.size() >= count) {
            isSimilar = true;
        }
        /**
         * If both queries have 50% similarity in terms of having similar
         * tokens, they belong to the same task .
         */
        return isSimilar;
    }

    public static UserQuery isInSessionTask(Session session, UserQuery currentQuery) {
        for (UserQuery query : session.getUser_queries()) {
            if (checkSameTask(query, currentQuery)) {
                return query;
            }
        }
        return null;
    }

    private static HashMap<String, Integer> computeTermFreq(List<String> tokens) {
        HashMap<String, Integer> termFreqMap = new HashMap<>();
        // computing term frequency of all the unique terms found in the document
        for (String tok : tokens) {
            if (termFreqMap.containsKey(tok)) {
                termFreqMap.put(tok, termFreqMap.get(tok) + 1);
            } else {
                termFreqMap.put(tok, 1);
            }
        }
        return termFreqMap;
    }

    private static ArrayList<ResultDoc> reRank(HashMap<String, Float> docScoreMap, ArrayList<ResultDoc> relDocs) {
        /**
         * Client side re-ranking using true user profile.
         */
        Map<String, Float> resultedMap = SortMap.sortMapByValue(docScoreMap, false);

        /**
         * Re-rank the documents by giving weight to the search engine rank and
         * the client side rank.
         */
        int i = 0;
        for (Map.Entry<String, Float> entry : resultedMap.entrySet()) {
            float score;
            // Giving 30% weight to client-side re-ranking and 70% to server-side ranking.
            if (entry.getValue() == 0) {
                score = 0.7f * (1.0f / (Integer.parseInt(entry.getKey()) + 1));
            } else {
                score = 0.3f * (1.0f / (i + 1)) + 0.7f * (1.0f / (Integer.parseInt(entry.getKey()) + 1));
            }
            docScoreMap.put(entry.getKey(), score);
            i++;
        }

        // sort the documents in descending order according to the new score assigned
        Map<String, Float> result = SortMap.sortMapByValue(docScoreMap, false);
        ArrayList<ResultDoc> retValue = new ArrayList<>();
        for (Map.Entry<String, Float> entry : result.entrySet()) {
            retValue.add(relDocs.get(Integer.parseInt(entry.getKey())));
        }

        return retValue;
    }

    /**
     * Method that re-ranks the result in the client side.
     *
     * @param history
     * @param relDocs all the relevant documents returned by the search engine
     * @return re-ranked resulting documents
     */
    public static ArrayList<ResultDoc> reRankResults(HashMap<String, Integer> history, ArrayList<ResultDoc> relDocs) {
        HashMap<String, Float> docScoreMap = new HashMap<>();
        HashMap<String, Integer> uniqueDocTerms;

        for (int i = 0; i < relDocs.size(); i++) {
            List<String> tokens = TOKENIZER.TokenizeText(relDocs.get(i).getContent());
            uniqueDocTerms = computeTermFreq(tokens);

            float docScore = 0;
            // smoothing parameter for linear interpolation
            float lambda = 0.1f;
            for (Map.Entry<String, Integer> entry : history.entrySet()) {
                if (!uniqueDocTerms.containsKey(entry.getKey())) {
                    continue;
                }
                // maximum likelihood calculation
                Double tokenProb = (uniqueDocTerms.get(entry.getKey()) * 1.0) / tokens.size();
                // probability from reference model for smoothing purpose
                Double refProb = StaticData.SmoothingReference.get(entry.getKey());
                if (refProb == null) {
                    refProb = 0.0;
                }
                // smoothing token probability using linear interpolation
                Double smoothedTokenProb = (1 - lambda) * tokenProb + lambda * refProb;
                smoothedTokenProb = smoothedTokenProb / (lambda * refProb);
                docScore = docScore + (entry.getValue() * (float) Math.log(smoothedTokenProb));
            }
            docScoreMap.put(String.valueOf(i), docScore);
        }

        // return re-ranked documents
        return reRank(docScoreMap, relDocs);
    }

    /**
     * Method that generate the id of all users for evaluation.
     *
     * @param folder folder path where all user search log resides
     * @param count
     * @return list of all user id
     */
    public static ArrayList<String> getAllUserId(String folder, int count) {
        ArrayList<String> allUserIds = new ArrayList<>();
        File dir = new File(folder);
        int userCount = 0;
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                String fileName = f.getName();
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                allUserIds.add(fileName);
                userCount++;
            }
            if (userCount == count) {
                break;
            }
        }
        return allUserIds;
    }

}
