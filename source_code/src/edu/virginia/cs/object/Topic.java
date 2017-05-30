/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.object;

import edu.virginia.cs.config.RunTimeConfig;
import edu.virginia.cs.config.StaticData;
import edu.virginia.cs.engine.Searcher;
import edu.virginia.cs.model.TopicTreeNode;
import edu.virginia.cs.utility.SortMap;
import edu.virginia.cs.utility.TextTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author wua4nw
 */
public class Topic {

    private final int BUCKET_SIZE = 10;
    private final double LAMBDA = 0.1;
    private final double GAMMA = 0.1;

    private HashMap<String, Integer> unigramLM;
    private HashMap<String, Integer> bigramLM;
    private HashMap<String, Integer> trigramLM;
    private HashMap<String, Integer> fourgramLM;
    private final Double[] probArrayUnigram;
    private final Double[] probArrayBigram;
    private final Double[] probArrayTrigram;
    private final Double[] probArrayFourgram;

    private int totalUnigrams;
    private int totalUniqueUnigrams;

    private double maxProbUnigram;
    private double minProbUnigram;
    private double maxProbBigram;
    private double minProbBigram;
    private double maxProbTrigram;
    private double minProbTrigram;
    private double maxProbFourgram;
    private double minProbFourgram;

    private final TextTokenizer tokenizer;

    public Topic() {
        this.unigramLM = new HashMap<>();
        this.bigramLM = new HashMap<>();
        this.trigramLM = new HashMap<>();
        this.fourgramLM = new HashMap<>();
        this.probArrayUnigram = new Double[BUCKET_SIZE];
        this.probArrayBigram = new Double[BUCKET_SIZE];
        this.probArrayTrigram = new Double[BUCKET_SIZE];
        this.probArrayFourgram = new Double[BUCKET_SIZE];
        this.tokenizer = new TextTokenizer(RunTimeConfig.removeStopWordsInCQ, RunTimeConfig.doStemmingInCQ);
    }

    public void reset() {
        this.unigramLM = new HashMap<>();
        this.bigramLM = new HashMap<>();
        this.trigramLM = new HashMap<>();
        this.fourgramLM = new HashMap<>();
        totalUnigrams = 0;
        totalUniqueUnigrams = 0;
    }

    public HashMap<String, Integer> getUnigramLM() {
        return unigramLM;
    }

    public void setUnigramLM(HashMap<String, Integer> unigramLM) {
        this.unigramLM = unigramLM;
    }

    public HashMap<String, Integer> getBigramLM() {
        return bigramLM;
    }

    public void setBigramLM(HashMap<String, Integer> bigramLM) {
        this.bigramLM = bigramLM;
    }

    public HashMap<String, Integer> getTrigramLM() {
        return trigramLM;
    }

    public void setTrigramLM(HashMap<String, Integer> trigramLM) {
        this.trigramLM = trigramLM;
    }

    public HashMap<String, Integer> getFourgramLM() {
        return fourgramLM;
    }

    public void setFourgramLM(HashMap<String, Integer> fourgramLM) {
        this.fourgramLM = fourgramLM;
    }

    public int getTotalUnigrams() {
        return totalUnigrams;
    }

    public void setTotalUnigrams(int totalUnigrams) {
        this.totalUnigrams = totalUnigrams;
    }

    public int getTotalUniqueUnigrams() {
        return totalUniqueUnigrams;
    }

    public void setTotalUniqueUnigrams(int totalUniqueUnigrams) {
        this.totalUniqueUnigrams = totalUniqueUnigrams;
    }

    public double getMaxProbUnigram() {
        return maxProbUnigram;
    }

    public void setMaxProbUnigram(double maxProbUnigram) {
        this.maxProbUnigram = maxProbUnigram;
    }

    public double getMinProbUnigram() {
        return minProbUnigram;
    }

    public void setMinProbUnigram(double minProbUnigram) {
        this.minProbUnigram = minProbUnigram;
    }

    public double getMaxProbBigram() {
        return maxProbBigram;
    }

    public void setMaxProbBigram(double maxProbBigram) {
        this.maxProbBigram = maxProbBigram;
    }

    public double getMinProbBigram() {
        return minProbBigram;
    }

    public void setMinProbBigram(double minProbBigram) {
        this.minProbBigram = minProbBigram;
    }

    public double getMaxProbTrigram() {
        return maxProbTrigram;
    }

    public void setMaxProbTrigram(double maxProbTrigram) {
        this.maxProbTrigram = maxProbTrigram;
    }

    public double getMinProbTrigram() {
        return minProbTrigram;
    }

    public void setMinProbTrigram(double minProbTrigram) {
        this.minProbTrigram = minProbTrigram;
    }

    public double getMaxProbFourgram() {
        return maxProbFourgram;
    }

    public void setMaxProbFourgram(double maxProbFourgram) {
        this.maxProbFourgram = maxProbFourgram;
    }

    public double getMinProbFourgram() {
        return minProbFourgram;
    }

    public void setMinProbFourgram(double minProbFourgram) {
        this.minProbFourgram = minProbFourgram;
    }

    /**
     *
     * @return
     */
    public Double[] getProbArrayUnigram() {
        return probArrayUnigram;
    }

    /**
     *
     * @return
     */
    public Double[] getProbArrayBigram() {
        return probArrayBigram;
    }

    /**
     *
     * @return
     */
    public Double[] getProbArrayTrigram() {
        return probArrayTrigram;
    }

    /**
     *
     * @return
     */
    public Double[] getProbArrayFourgram() {
        return probArrayFourgram;
    }

    /**
     * Checks whether a topic is empty or not.
     *
     * @return
     */
    public boolean isEmpty() {
        return unigramLM.isEmpty();
    }

    /**
     * Computes probability from reference model for smoothing purpose.
     *
     * @param unigram
     * @return
     */
    public double getReferenceProbability(String unigram) {
        if (StaticData.SmoothingReference.containsKey(unigram)) {
            return StaticData.SmoothingReference.get(unigram);
        }
        return 0.00000001;
    }

    /**
     * Computes joint probability or conditional probability of a unigram.
     *
     * @param unigram
     * @return
     */
    public double getProbabilityUnigram(String unigram) {
        double prob;
        /* Computing probability of a unigram using linear interpolation smoothing */
        if (unigramLM.containsKey(unigram)) {
            prob = (1.0 - LAMBDA) * (unigramLM.get(unigram) / totalUnigrams) + LAMBDA * getReferenceProbability(unigram);
        } else {
            prob = LAMBDA * getReferenceProbability(unigram);
        }
        return prob;
    }

    /**
     * Computes joint probability or conditional probability of a unigram.
     *
     * @param unigram
     * @param queryTopicNode
     * @return
     */
    public double getProbabilityUnigram(String unigram, TopicTreeNode queryTopicNode) {
        double prob;
        TopicTreeNode parent = (TopicTreeNode) (queryTopicNode.getParent());
        /* Computing probability of a unigram using linear interpolation smoothing */
        if (unigramLM.containsKey(unigram)) {
            if (parent != null) {
                prob = (1.0 - LAMBDA - GAMMA) * (unigramLM.get(unigram) / totalUnigrams)
                        + LAMBDA * getReferenceProbability(unigram)
                        + GAMMA * parent.getTopic().getProbabilityUnigram(unigram, parent);
            } else {
                prob = (1.0 - LAMBDA) * (unigramLM.get(unigram) / totalUnigrams)
                        + LAMBDA * getReferenceProbability(unigram);
            }

        } else {
            prob = LAMBDA * getReferenceProbability(unigram);
        }
        return prob;
    }

    /**
     * Computes joint probability or conditional probability of a bigram.
     *
     * @param bigram
     * @param isJoint
     * @return
     */
    public double getProbabilityBigram(String bigram, boolean isJoint) {
        double prob;
        /* Computing probability of a bigram using linear interpolation smoothing */
        String[] split = bigram.split(" ");
        if (bigramLM.containsKey(bigram)) {
            prob = (1.0 - LAMBDA) * (bigramLM.get(bigram) / unigramLM.get(split[0]));
        } else {
            prob = 0.0;
        }
        prob += LAMBDA * getProbabilityUnigram(split[0]);
        if (isJoint) {
            prob *= getProbabilityUnigram(split[0]);
        }
        return prob;
    }

    /**
     * Computes joint probability or conditional probability of a bigram.
     *
     * @param bigram
     * @param isJoint
     * @param queryTopicNode
     * @return
     */
    public double getProbabilityBigram(String bigram, boolean isJoint, TopicTreeNode queryTopicNode) {
        double prob;
        /* Computing probability of a bigram using linear interpolation smoothing */
        TopicTreeNode parent = (TopicTreeNode) (queryTopicNode.getParent());
        String[] split = bigram.split(" ");
        if (bigramLM.containsKey(bigram)) {
            if (parent != null) {
                prob = (1.0 - LAMBDA - GAMMA) * (bigramLM.get(bigram) / unigramLM.get(split[0]));
            } else {
                prob = (1.0 - LAMBDA) * (bigramLM.get(bigram) / unigramLM.get(split[0]));
            }
        } else {
            prob = 0.0;
        }
        if (parent != null) {
            prob = prob + LAMBDA * getProbabilityUnigram(split[0], queryTopicNode)
                    + GAMMA * parent.getTopic().getProbabilityBigram(bigram, isJoint, parent);
        } else {
            prob = prob + LAMBDA * getProbabilityUnigram(split[0], queryTopicNode);
        }
        if (isJoint) {
            prob *= getProbabilityUnigram(split[0], queryTopicNode);
        }
        return prob;
    }

    /**
     * Computes joint probability or conditional probability of a trigram.
     *
     * @param trigram
     * @param isJoint
     * @return
     */
    public double getProbabilityTrigram(String trigram, boolean isJoint) {
        double prob;
        /* Computing probability of a trigram using linear interpolation smoothing */
        String[] split = trigram.split(" ");
        String prevBigram = split[0] + " " + split[1];
        if (trigramLM.containsKey(trigram)) {
            prob = (1.0 - LAMBDA) * (trigramLM.get(trigram) / bigramLM.get(prevBigram));
        } else {
            prob = 0.0;
        }
        String bigram = split[1] + " " + split[2];
        prob += LAMBDA * getProbabilityBigram(bigram, false);
        if (isJoint) {
            prob *= getProbabilityBigram(prevBigram, false);
            prob *= getProbabilityUnigram(split[0]);
        }
        return prob;
    }

    /**
     * Computes joint probability or conditional probability of a trigram.
     *
     * @param trigram
     * @param isJoint
     * @param queryTopicNode
     * @return
     */
    public double getProbabilityTrigram(String trigram, boolean isJoint, TopicTreeNode queryTopicNode) {
        double prob;
        /* Computing probability of a trigram using linear interpolation smoothing */
        TopicTreeNode parent = (TopicTreeNode) (queryTopicNode.getParent());
        String[] split = trigram.split(" ");
        String prevBigram = split[0] + " " + split[1];
        if (trigramLM.containsKey(trigram)) {
            if (parent != null) {
                prob = (1.0 - LAMBDA - GAMMA) * (trigramLM.get(trigram) / bigramLM.get(prevBigram));
            } else {
                prob = (1.0 - LAMBDA) * (trigramLM.get(trigram) / bigramLM.get(prevBigram));
            }
        } else {
            prob = 0.0;
        }
        String bigram = split[1] + " " + split[2];
        if (parent != null) {
            prob = prob + LAMBDA * getProbabilityBigram(bigram, false, queryTopicNode)
                    + GAMMA * parent.getTopic().getProbabilityTrigram(trigram, isJoint, parent);
        } else {
            prob = prob + LAMBDA * getProbabilityBigram(bigram, false, queryTopicNode);
        }
        if (isJoint) {
            prob *= getProbabilityBigram(prevBigram, false, queryTopicNode);
            prob *= getProbabilityUnigram(split[0], queryTopicNode);
        }
        return prob;
    }

    /**
     * Computes joint probability or conditional probability of a fourgram.
     *
     * @param fourgram
     * @param isJoint
     * @return if isJoint is true, returns joint probability, otherwise
     * conditional probability
     */
    public double getProbabilityFourgram(String fourgram, boolean isJoint) {
        double prob;
        /* Computing probability of a fourgram using linear interpolation smoothing */
        String[] split = fourgram.split(" ");
        String prevTrigram = split[0] + " " + split[1] + " " + split[2];
        if (fourgramLM.containsKey(fourgram)) {
            prob = (1.0 - LAMBDA) * (fourgramLM.get(fourgram) / trigramLM.get(prevTrigram));
        } else {
            prob = 0.0;
        }
        String trigram = split[1] + " " + split[2] + " " + split[3];
        prob += LAMBDA * getProbabilityTrigram(trigram, false);
        if (isJoint) {
            prob *= getProbabilityBigram(prevTrigram, false);
            String bigram = split[0] + " " + split[1];
            prob *= getProbabilityBigram(bigram, false);
            prob *= getProbabilityUnigram(split[0]);
        }
        return prob;
    }

    /**
     * Computes joint probability or conditional probability of a fourgram.
     *
     * @param fourgram
     * @param isJoint
     * @param queryTopicNode
     * @return if isJoint is true, returns joint probability, otherwise
     * conditional probability
     */
    public double getProbabilityFourgram(String fourgram, boolean isJoint, TopicTreeNode queryTopicNode) {
        double prob;
        /* Computing probability of a fourgram using linear interpolation smoothing */
        TopicTreeNode parent = (TopicTreeNode) (queryTopicNode.getParent());
        String[] split = fourgram.split(" ");
        String prevTrigram = split[0] + " " + split[1] + " " + split[2];
        if (fourgramLM.containsKey(fourgram)) {
            if (parent != null) {
                prob = (1.0 - LAMBDA - GAMMA) * (fourgramLM.get(fourgram) / trigramLM.get(prevTrigram));
            } else {
                prob = (1.0 - LAMBDA) * (fourgramLM.get(fourgram) / trigramLM.get(prevTrigram));
            }
        } else {
            prob = 0.0;
        }
        String trigram = split[1] + " " + split[2] + " " + split[3];
        if (parent != null) {
            prob = prob + LAMBDA * getProbabilityTrigram(trigram, false, queryTopicNode)
                    + GAMMA * parent.getTopic().getProbabilityFourgram(fourgram, isJoint, parent);
        } else {
            prob = prob + LAMBDA * getProbabilityTrigram(trigram, false, queryTopicNode);
        }
        if (isJoint) {
            prob *= getProbabilityBigram(prevTrigram, false, queryTopicNode);
            String bigram = split[0] + " " + split[1];
            prob *= getProbabilityBigram(bigram, false, queryTopicNode);
            prob *= getProbabilityUnigram(split[0], queryTopicNode);
        }
        return prob;
    }

    /**
     * Computes joint probability of a n-gram where n>4. Suppose n=6, then joint
     * probability formula is, P(w6 w5 w4 w3 w2 w1) = P(w6 | w5 w4 w3) * P(w5 |
     * w4 w3 w2) * P(w4 | w3 w2 w1) * P(w3 | w2 w1) * P(w2 | w1) * P(w1)
     *
     * @param ngram
     * @param n
     * @return
     */
    public double getProbabilityNgram(String ngram, int n) {
        double prob = 1.0;
        /* Computing probability of a n-gram using linear interpolation smoothing */
        String[] split = ngram.split(" ");
        for (int i = split.length - 1; i >= 0; i--) {
            if (i >= 3) {
                String fourgram = split[i - 3] + " " + split[i - 2] + " " + split[i - 1] + " " + split[i];
                prob *= getProbabilityFourgram(fourgram, false);
            } else if (i == 2) {
                String trigram = split[i - 2] + " " + split[i - 1] + " " + split[i];
                prob *= getProbabilityBigram(trigram, false);
            } else if (i == 1) {
                String bigram = split[i - 1] + " " + split[i];
                prob *= getProbabilityBigram(bigram, false);
            } else {
                prob *= getProbabilityUnigram(split[0]);
            }
        }
        return prob;
    }

    /**
     * Computes joint probability of a n-gram where n>4. Suppose n=6, then joint
     * probability formula is, P(w6 w5 w4 w3 w2 w1) = P(w6 | w5 w4 w3) * P(w5 |
     * w4 w3 w2) * P(w4 | w3 w2 w1) * P(w3 | w2 w1) * P(w2 | w1) * P(w1)
     *
     * @param ngram
     * @param n
     * @param queryTopicNode
     * @return
     */
    public double getProbabilityNgram(String ngram, int n, TopicTreeNode queryTopicNode) {
        double prob = 1.0;
        /* Computing probability of a n-gram using linear interpolation smoothing */
        String[] split = ngram.split(" ");
        for (int i = split.length - 1; i >= 0; i--) {
            if (i >= 3) {
                String fourgram = split[i - 3] + " " + split[i - 2] + " " + split[i - 1] + " " + split[i];
                prob *= getProbabilityFourgram(fourgram, false, queryTopicNode);
            } else if (i == 2) {
                String trigram = split[i - 2] + " " + split[i - 1] + " " + split[i];
                prob *= getProbabilityBigram(trigram, false, queryTopicNode);
            } else if (i == 1) {
                String bigram = split[i - 1] + " " + split[i];
                prob *= getProbabilityBigram(bigram, false, queryTopicNode);
            } else {
                prob *= getProbabilityUnigram(split[0], queryTopicNode);
            }
        }
        return prob;
    }

    /**
     * Set the Maximum and Minimum probability of the language model.
     *
     * @param param either unigram or bigram or trigram or fourgram
     */
    public void setMaxMinProb(String param) {
        double max = -1.0;
        double min = -1.0;
        HashMap<String, Double> tempMap = new HashMap<>();
        if (param.equals("unigram")) {
            if (unigramLM.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : unigramLM.entrySet()) {
                double prob = getProbabilityUnigram(entry.getKey());
                tempMap.put(entry.getKey(), prob);
                if (max < prob) {
                    max = prob;
                }
                if (min > prob) {
                    min = prob;
                }
            }
            maxProbUnigram = max;
            minProbUnigram = min;
            List<Map.Entry<String, Double>> list = SortMap.sortMapGetList(tempMap, false);
            int size = list.size() % BUCKET_SIZE == 0 ? list.size() / BUCKET_SIZE : (list.size() / BUCKET_SIZE) + 1;
            for (int i = 1; i <= BUCKET_SIZE; i++) {
                int index = i * size;
                if (index >= list.size()) {
                    index = list.size() - 1;
                    probArrayUnigram[i - 1] = list.get(index).getValue();
                } else {
                    probArrayUnigram[i - 1] = list.get(index).getValue();
                }
            }
        } else if (param.equals("bigram")) {
            if (bigramLM.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : bigramLM.entrySet()) {
                double prob = getProbabilityBigram(entry.getKey(), true);
                tempMap.put(entry.getKey(), prob);
                if (max < prob) {
                    max = prob;
                }
                if (min > prob) {
                    min = prob;
                }
            }
            maxProbBigram = max;
            minProbBigram = min;
            List<Map.Entry<String, Double>> list = SortMap.sortMapGetList(tempMap, false);
            int size = list.size() % BUCKET_SIZE == 0 ? list.size() / BUCKET_SIZE : (list.size() / BUCKET_SIZE) + 1;
            for (int i = 1; i <= BUCKET_SIZE; i++) {
                int index = i * size;
                if (index >= list.size()) {
                    index = list.size() - 1;
                    probArrayBigram[i - 1] = list.get(index).getValue();
                } else {
                    probArrayBigram[i - 1] = list.get(index).getValue();
                }
            }
        } else if (param.equals("trigram")) {
            if (trigramLM.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : trigramLM.entrySet()) {
                double prob = getProbabilityTrigram(entry.getKey(), true);
                tempMap.put(entry.getKey(), prob);
                if (max < prob) {
                    max = prob;
                }
                if (min > prob) {
                    min = prob;
                }
            }
            maxProbTrigram = max;
            minProbTrigram = min;
            List<Map.Entry<String, Double>> list = SortMap.sortMapGetList(tempMap, false);
            int size = list.size() % BUCKET_SIZE == 0 ? list.size() / BUCKET_SIZE : (list.size() / BUCKET_SIZE) + 1;
            for (int i = 1; i <= BUCKET_SIZE; i++) {
                int index = i * size;
                if (index >= list.size()) {
                    index = list.size() - 1;
                    probArrayTrigram[i - 1] = list.get(index).getValue();
                } else {
                    probArrayTrigram[i - 1] = list.get(index).getValue();
                }
            }
        } else if (param.equals("fourgram")) {
            if (fourgramLM.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : fourgramLM.entrySet()) {
                double prob = getProbabilityFourgram(entry.getKey(), true);
                tempMap.put(entry.getKey(), prob);
                if (max < prob) {
                    max = prob;
                }
                if (min > prob) {
                    min = prob;
                }
            }
            maxProbFourgram = max;
            minProbFourgram = min;
            List<Map.Entry<String, Double>> list = SortMap.sortMapGetList(tempMap, false);
            int size = list.size() % BUCKET_SIZE == 0 ? list.size() / BUCKET_SIZE : (list.size() / BUCKET_SIZE) + 1;
            for (int i = 1; i <= BUCKET_SIZE; i++) {
                int index = i * size;
                if (index >= list.size()) {
                    index = list.size() - 1;
                    probArrayFourgram[i - 1] = list.get(index).getValue();
                } else {
                    probArrayFourgram[i - 1] = list.get(index).getValue();
                }
            }
        } else {
            System.err.println("Unknown Parameter...!");
        }
    }

    /**
     * Set the Maximum and Minimum probability of the language model.
     *
     * @param param either unigram or bigram or trigram or fourgram
     * @param queryTopicNode
     */
    public void setMaxMinProb(String param, TopicTreeNode queryTopicNode) {
        double max = -1.0;
        double min = -1.0;
        HashMap<String, Double> tempMap = new HashMap<>();
        if (param.equals("unigram")) {
            if (unigramLM.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : unigramLM.entrySet()) {
                double prob = getProbabilityUnigram(entry.getKey(), queryTopicNode);
                tempMap.put(entry.getKey(), prob);
                if (max < prob) {
                    max = prob;
                }
                if (min > prob) {
                    min = prob;
                }
            }
            maxProbUnigram = max;
            minProbUnigram = min;
            List<Map.Entry<String, Double>> list = SortMap.sortMapGetList(tempMap, false);
            int size = list.size() % BUCKET_SIZE == 0 ? list.size() / BUCKET_SIZE : (list.size() / BUCKET_SIZE) + 1;
            for (int i = 1; i <= BUCKET_SIZE; i++) {
                int index = i * size;
                if (index >= list.size()) {
                    index = list.size() - 1;
                    probArrayUnigram[i - 1] = list.get(index).getValue();
                } else {
                    probArrayUnigram[i - 1] = list.get(index).getValue();
                }
            }
        } else if (param.equals("bigram")) {
            if (bigramLM.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : bigramLM.entrySet()) {
                double prob = getProbabilityBigram(entry.getKey(), true, queryTopicNode);
                tempMap.put(entry.getKey(), prob);
                if (max < prob) {
                    max = prob;
                }
                if (min > prob) {
                    min = prob;
                }
            }
            maxProbBigram = max;
            minProbBigram = min;
            List<Map.Entry<String, Double>> list = SortMap.sortMapGetList(tempMap, false);
            int size = list.size() % BUCKET_SIZE == 0 ? list.size() / BUCKET_SIZE : (list.size() / BUCKET_SIZE) + 1;
            for (int i = 1; i <= BUCKET_SIZE; i++) {
                int index = i * size;
                if (index >= list.size()) {
                    index = list.size() - 1;
                    probArrayBigram[i - 1] = list.get(index).getValue();
                } else {
                    probArrayBigram[i - 1] = list.get(index).getValue();
                }
            }
        } else if (param.equals("trigram")) {
            if (trigramLM.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : trigramLM.entrySet()) {
                double prob = getProbabilityTrigram(entry.getKey(), true, queryTopicNode);
                tempMap.put(entry.getKey(), prob);
                if (max < prob) {
                    max = prob;
                }
                if (min > prob) {
                    min = prob;
                }
            }
            maxProbTrigram = max;
            minProbTrigram = min;
            List<Map.Entry<String, Double>> list = SortMap.sortMapGetList(tempMap, false);
            int size = list.size() % BUCKET_SIZE == 0 ? list.size() / BUCKET_SIZE : (list.size() / BUCKET_SIZE) + 1;
            for (int i = 1; i <= BUCKET_SIZE; i++) {
                int index = i * size;
                if (index >= list.size()) {
                    index = list.size() - 1;
                    probArrayTrigram[i - 1] = list.get(index).getValue();
                } else {
                    probArrayTrigram[i - 1] = list.get(index).getValue();
                }
            }
        } else if (param.equals("fourgram")) {
            if (fourgramLM.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Integer> entry : fourgramLM.entrySet()) {
                double prob = getProbabilityFourgram(entry.getKey(), true, queryTopicNode);
                tempMap.put(entry.getKey(), prob);
                if (max < prob) {
                    max = prob;
                }
                if (min > prob) {
                    min = prob;
                }
            }
            maxProbFourgram = max;
            minProbFourgram = min;
            List<Map.Entry<String, Double>> list = SortMap.sortMapGetList(tempMap, false);
            int size = list.size() % BUCKET_SIZE == 0 ? list.size() / BUCKET_SIZE : (list.size() / BUCKET_SIZE) + 1;
            for (int i = 1; i <= BUCKET_SIZE; i++) {
                int index = i * size;
                if (index >= list.size()) {
                    index = list.size() - 1;
                    probArrayFourgram[i - 1] = list.get(index).getValue();
                } else {
                    probArrayFourgram[i - 1] = list.get(index).getValue();
                }
            }
        } else {
            System.err.println("Unknown Parameter...!");
        }
    }

    private boolean checkForNull(Double[] param) {
        boolean nonNullElemExist = false;
        for (Double d : param) {
            if (d != null) {
                nonNullElemExist = true;
                break;
            }
        }
        return nonNullElemExist;
    }

    public boolean prepare(Searcher _searcher, String topic_name, int option) {
        ArrayList<String> documents = _searcher.search(topic_name, "topic", "modified_content");
        if (!documents.isEmpty()) {
            for (String content : documents) {
                analyzeDocument(content, option);
            }
            switch (option) {
                case 1:
                    if (!checkForNull(probArrayUnigram)) {
                        setMaxMinProb("unigram");
                    }
                    break;
                case 2:
                    if (!checkForNull(probArrayUnigram)) {
                        setMaxMinProb("unigram");
                    }
                    if (!checkForNull(probArrayBigram)) {
                        setMaxMinProb("bigram");
                    }
                    break;
                case 3:
                    if (!checkForNull(probArrayUnigram)) {
                        setMaxMinProb("unigram");
                    }
                    if (!checkForNull(probArrayBigram)) {
                        setMaxMinProb("bigram");
                    }
                    if (!checkForNull(probArrayTrigram)) {
                        setMaxMinProb("trigram");
                    }
                    break;
                default:
                    if (!checkForNull(probArrayUnigram)) {
                        setMaxMinProb("unigram");
                    }
                    if (!checkForNull(probArrayBigram)) {
                        setMaxMinProb("bigram");
                    }
                    if (!checkForNull(probArrayTrigram)) {
                        setMaxMinProb("trigram");
                    }
                    if (!checkForNull(probArrayFourgram)) {
                        setMaxMinProb("fourgram");
                    }
                    break;
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean prepare(Searcher _searcher, String topic_name, int option, TopicTreeNode queryTopicNode) {
        ArrayList<String> documents = _searcher.search(topic_name, "topic", "modified_content");
        if (!documents.isEmpty()) {
            for (String content : documents) {
                analyzeDocument(content, option);
            }
            switch (option) {
                case 1:
                    if (!checkForNull(probArrayUnigram)) {
                        setMaxMinProb("unigram", queryTopicNode);
                    }
                    break;
                case 2:
                    if (!checkForNull(probArrayUnigram)) {
                        setMaxMinProb("unigram", queryTopicNode);
                    }
                    if (!checkForNull(probArrayBigram)) {
                        setMaxMinProb("bigram", queryTopicNode);
                    }
                    break;
                case 3:
                    if (!checkForNull(probArrayUnigram)) {
                        setMaxMinProb("unigram", queryTopicNode);
                    }
                    if (!checkForNull(probArrayBigram)) {
                        setMaxMinProb("bigram", queryTopicNode);
                    }
                    if (!checkForNull(probArrayTrigram)) {
                        setMaxMinProb("trigram", queryTopicNode);
                    }
                    break;
                default:
                    if (!checkForNull(probArrayUnigram)) {
                        setMaxMinProb("unigram", queryTopicNode);
                    }
                    if (!checkForNull(probArrayBigram)) {
                        setMaxMinProb("bigram", queryTopicNode);
                    }
                    if (!checkForNull(probArrayTrigram)) {
                        setMaxMinProb("trigram", queryTopicNode);
                    }
                    if (!checkForNull(probArrayFourgram)) {
                        setMaxMinProb("fourgram", queryTopicNode);
                    }
                    break;
            }
            return true;
        } else {
            return false;
        }
    }

    private void analyzeDocument(String document, int option) {
        String previousTrigram = ""; // for four-grams
        String previousBigram = ""; // for trigrams
        String previousUnigram = ""; // for bigrams
        List<String> tokens = tokenizer.TokenizeText(document);
        for (String token : tokens) {
            if (!token.isEmpty()) {
                if (unigramLM.containsKey(token)) {
                    unigramLM.put(token, unigramLM.get(token) + 1);
                } else {
                    unigramLM.put(token, 1);
                    totalUniqueUnigrams++;
                }
                totalUnigrams++;
                if (option == 1) {
                    continue;
                }
                // generating bigrams
                if (!previousUnigram.isEmpty()) {
                    String bigram = previousUnigram + " " + token;
                    if (bigramLM.containsKey(bigram)) {
                        bigramLM.put(bigram, bigramLM.get(bigram) + 1);
                    } else {
                        bigramLM.put(bigram, 1);
                    }
                }
                if (option == 2) {
                    previousUnigram = token;
                    continue;
                }
                // generating trigrams
                if (!previousBigram.isEmpty()) {
                    String trigram = previousBigram + " " + token;
                    if (trigramLM.containsKey(trigram)) {
                        trigramLM.put(trigram, trigramLM.get(trigram) + 1);
                    } else {
                        trigramLM.put(trigram, 1);
                    }
                }
                if (option == 3) {
                    if (!previousUnigram.isEmpty()) {
                        previousBigram = previousUnigram + " " + token;
                    }
                    previousUnigram = token;
                    continue;
                }
                // generating four-grams
                if (!previousTrigram.isEmpty()) {
                    String fourgram = previousTrigram + " " + token;
                    if (fourgramLM.containsKey(fourgram)) {
                        fourgramLM.put(fourgram, fourgramLM.get(fourgram) + 1);
                    } else {
                        fourgramLM.put(fourgram, 1);
                    }
                }
                if (!previousBigram.isEmpty()) {
                    previousTrigram = previousBigram + " " + token;
                }
                if (!previousUnigram.isEmpty()) {
                    previousBigram = previousUnigram + " " + token;
                }
                previousUnigram = token;
            }
        }
    }

}
