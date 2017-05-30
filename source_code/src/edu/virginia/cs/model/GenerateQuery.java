/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.model;

import edu.virginia.cs.config.RunTimeConfig;
import edu.virginia.cs.engine.Searcher;
import edu.virginia.cs.extra.Helper;
import static edu.virginia.cs.extra.Helper.getRandom;
import edu.virginia.cs.object.Topic;
import edu.virginia.cs.object.UserQuery;
import edu.virginia.cs.user.Intent;
import edu.virginia.cs.user.Profile;
import edu.virginia.cs.utility.TextTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author wua4nw
 */
public class GenerateQuery {

    private final TextTokenizer tokenizer;
    private final TextTokenizer extraTokenizer;
    private final Searcher SEARCHER;

    public GenerateQuery(Searcher searcher, TextTokenizer _tokenizer) {
        this.SEARCHER = searcher;
        this.tokenizer = _tokenizer;
        extraTokenizer = new TextTokenizer(true, RunTimeConfig.doStemmingInCQ);
    }

    /**
     *
     * @param probability
     *
     * @return
     */
    private int getBucketNumber(double probability, Double[] probArray) {
        for (int i = 0; i < probArray.length; i++) {
            if (probability >= probArray[i]) {
                return i + 1;
            }
        }
        return probArray.length;
    }

    /**
     * Generate cover query of length 1 using unigram language model.
     *
     * @param bucket_num
     * @param cover_node
     * @param profile
     * @param currentQuery
     * @return the cover query
     */
    public UserQuery getCQfromUnigramLM(int bucket_num, TopicTreeNode cover_node, UserQuery currentQuery, Profile profile) {
        Topic coverQueryTopic = cover_node.getTopic();
        coverQueryTopic.prepare(SEARCHER, cover_node.getTopic_name(), 1);
        if (coverQueryTopic.isEmpty()) {
            return null;
        }
        ArrayList<String> possibleCoverQ = new ArrayList<>();
        Double[] probArray = coverQueryTopic.getProbArrayUnigram();

        for (String unigram : coverQueryTopic.getUnigramLM().keySet()) {
            double prob = coverQueryTopic.getProbabilityUnigram(unigram);
//            double prob = coverQueryTopic.getProbabilityUnigram(unigram, cover_node);
            int bNum = getBucketNumber(prob, probArray);
            if (bNum == bucket_num) {
                possibleCoverQ.add(unigram);
            } else if (Objects.equals(probArray[bNum - 1], probArray[bucket_num - 1])) {
                possibleCoverQ.add(unigram);
            }
        }
        coverQueryTopic.reset();

        if (possibleCoverQ.size() > 0) {
            UserQuery prevQuery = Helper.isInSessionTask(profile.getLastSession(), currentQuery);
            ArrayList<UserQuery> prevCoverQueries = null;
            if (prevQuery != null) {
                prevCoverQueries = prevQuery.getCover_queries();
            }

            for (int i = 0; i < possibleCoverQ.size(); i++) {
                int coverQNum = getRandom(0, possibleCoverQ.size());
                UserQuery cQuery = cQuery = new UserQuery(0, possibleCoverQ.get(coverQNum));
                Intent coverIntent = new Intent(cover_node.getTopic_name());
                cQuery.setQuery_intent(coverIntent);

                if (prevCoverQueries != null) {
                    boolean flag = true;
                    for (UserQuery temp : prevCoverQueries) {
                        if (Helper.checkSameTask(temp, cQuery)) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        continue;
                    }
                }

                if (RunTimeConfig.removeStopWordsInCQ) {
                    return cQuery;
                } else if (extraTokenizer.TokenizeText(cQuery.getQuery_text()).size() == 1) {
                    return cQuery;
                }
            }
        }
        return null;
    }

    /**
     * Returns a unigram from a given a topic and bucket number.
     *
     * @param coverQueryTopic
     * @param bucketNum
     * @return
     */
    private String getUniGramFromLM(int bucket_num, Topic coverQueryTopic, TopicTreeNode cover_node) {
        ArrayList<String> possibleCoverQ = new ArrayList<>();
        Double[] probArray = coverQueryTopic.getProbArrayUnigram();

        for (String unigram : coverQueryTopic.getUnigramLM().keySet()) {
            double prob;
            if (cover_node == null) {
                prob = coverQueryTopic.getProbabilityUnigram(unigram);
            } else {
                prob = coverQueryTopic.getProbabilityUnigram(unigram, cover_node);
            }
            int bNum = getBucketNumber(prob, probArray);
            if (bNum == bucket_num) {
                possibleCoverQ.add(unigram);
            } else if (Objects.equals(probArray[bNum - 1], probArray[bucket_num - 1])) {
                possibleCoverQ.add(unigram);
            }
        }

        if (possibleCoverQ.size() > 0) {
            int coverQNum = getRandom(0, possibleCoverQ.size());
            if (RunTimeConfig.removeStopWordsInCQ) {
                return possibleCoverQ.get(coverQNum);
            } else if (extraTokenizer.TokenizeText(possibleCoverQ.get(coverQNum)).size() == 1) {
                return possibleCoverQ.get(coverQNum);
            }
        }
        return null;
    }

    /**
     * Generate cover query of length 2 using bigram language model.
     *
     * @param bucket_num
     * @param cover_node
     * @param profile
     * @param currentQuery
     * @return the cover query
     */
    public UserQuery getCQfromBigramLM(int bucket_num, TopicTreeNode cover_node, UserQuery currentQuery, Profile profile) {
        Topic coverQueryTopic = cover_node.getTopic();
        coverQueryTopic.prepare(SEARCHER, cover_node.getTopic_name(), 2);
        if (coverQueryTopic.isEmpty()) {
            return null;
        }
        ArrayList<String> possibleCoverQ = new ArrayList<>();
        Double[] probArray = coverQueryTopic.getProbArrayBigram();

        for (String bigram : coverQueryTopic.getBigramLM().keySet()) {
            double prob = coverQueryTopic.getProbabilityBigram(bigram, true);
//            double prob = coverQueryTopic.getProbabilityBigram(bigram, true, cover_node);
            int bNum = getBucketNumber(prob, probArray);
            if (bNum == bucket_num) {
                possibleCoverQ.add(bigram);
            } else if (Objects.equals(probArray[bNum - 1], probArray[bucket_num - 1])) {
                possibleCoverQ.add(bigram);
            }
        }
        coverQueryTopic.reset();

        if (possibleCoverQ.size() > 0) {
            UserQuery prevQuery = Helper.isInSessionTask(profile.getLastSession(), currentQuery);
            ArrayList<UserQuery> prevCoverQueries = null;
            if (prevQuery != null) {
                prevCoverQueries = prevQuery.getCover_queries();
            }

            for (int i = 0; i < possibleCoverQ.size(); i++) {
                int coverQNum = getRandom(0, possibleCoverQ.size());
                UserQuery cQuery = cQuery = new UserQuery(0, possibleCoverQ.get(coverQNum));
                Intent coverIntent = new Intent(cover_node.getTopic_name());
                cQuery.setQuery_intent(coverIntent);

                if (prevCoverQueries != null) {
                    boolean flag = true;
                    for (UserQuery temp : prevCoverQueries) {
                        if (Helper.checkSameTask(temp, cQuery)) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        continue;
                    }
                }

                if (RunTimeConfig.removeStopWordsInCQ) {
                    return cQuery;
                } else if (extraTokenizer.TokenizeText(cQuery.getQuery_text()).size() == 2) {
                    return cQuery;
                }
            }
        }
        return null;
    }

    /**
     * Returns a bigram from a given a topic and bucket number which contains
     * the unigram provided.
     *
     * @param coverQueryTopic
     * @param bucketNum
     * @param unigram
     * @return
     */
    private String getCQfromBigramLM(int bucket_num, Topic coverQueryTopic, String unigram, TopicTreeNode cover_node) {
        ArrayList<String> possibleCoverQ = new ArrayList<>();
        Double[] probArray = coverQueryTopic.getProbArrayBigram();

        for (String bigram : coverQueryTopic.getBigramLM().keySet()) {
            double prob;
            if (cover_node == null) {
                prob = coverQueryTopic.getProbabilityBigram(bigram, true);
            } else {
                prob = coverQueryTopic.getProbabilityBigram(bigram, true, cover_node);
            }
            String[] split = bigram.split(" ");
            String prevUnigram = split[0];
            if (prevUnigram.equals(unigram)) {
                possibleCoverQ.add(bigram);
                int bNum = getBucketNumber(prob, probArray);
                if (bNum == bucket_num) {
                    possibleCoverQ.add(bigram);
                } else if (Objects.equals(probArray[bNum - 1], probArray[bucket_num - 1])) {
                    possibleCoverQ.add(bigram);
                }
            }
        }

        if (possibleCoverQ.size() > 0) {
            int coverQNum = getRandom(0, possibleCoverQ.size());
            if (RunTimeConfig.removeStopWordsInCQ) {
                return possibleCoverQ.get(coverQNum);
            } else if (extraTokenizer.TokenizeText(possibleCoverQ.get(coverQNum)).size() == 2) {
                return possibleCoverQ.get(coverQNum);
            }
        }
        return null;
    }

    /**
     * Generate cover query of length 3 using trigram language model.
     *
     * @param bucket_num
     * @param cover_node
     * @param profile
     * @param currentQuery
     * @return the cover query
     */
    public UserQuery getCQfromTrigramLM(int bucket_num, TopicTreeNode cover_node, UserQuery currentQuery, Profile profile) {
        Topic coverQueryTopic = cover_node.getTopic();
        coverQueryTopic.prepare(SEARCHER, cover_node.getTopic_name(), 3);
        if (coverQueryTopic.isEmpty()) {
            return null;
        }
        ArrayList<String> possibleCoverQ = new ArrayList<>();
        Double[] probArray = coverQueryTopic.getProbArrayTrigram();

        for (String trigram : coverQueryTopic.getTrigramLM().keySet()) {
            double prob = coverQueryTopic.getProbabilityTrigram(trigram, true);
//            double prob = coverQueryTopic.getProbabilityTrigram(trigram, true, cover_node);
            int bNum = getBucketNumber(prob, probArray);
            if (bNum == bucket_num) {
                possibleCoverQ.add(trigram);
            } else if (Objects.equals(probArray[bNum - 1], probArray[bucket_num - 1])) {
                possibleCoverQ.add(trigram);
            }
        }
        coverQueryTopic.reset();

        if (possibleCoverQ.size() > 0) {
            UserQuery prevQuery = Helper.isInSessionTask(profile.getLastSession(), currentQuery);
            ArrayList<UserQuery> prevCoverQueries = null;
            if (prevQuery != null) {
                prevCoverQueries = prevQuery.getCover_queries();
            }

            for (int i = 0; i < possibleCoverQ.size(); i++) {
                int coverQNum = getRandom(0, possibleCoverQ.size());
                UserQuery cQuery = cQuery = new UserQuery(0, possibleCoverQ.get(coverQNum));
                Intent coverIntent = new Intent(cover_node.getTopic_name());
                cQuery.setQuery_intent(coverIntent);

                if (prevCoverQueries != null) {
                    boolean flag = true;
                    for (UserQuery temp : prevCoverQueries) {
                        if (Helper.checkSameTask(temp, cQuery)) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        continue;
                    }
                }
                return cQuery;
            }
        }
        return null;
    }

    /**
     * Returns a trigram from a given a topic and bucket number which contains
     * the bigram provided.
     *
     * @param coverQueryTopic
     * @param bucketNum
     * @param bigram
     * @return
     */
    private String getTriGramFromLM(int bucket_num, Topic coverQueryTopic, String bigram, TopicTreeNode cover_node) {
        ArrayList<String> possibleCoverQ = new ArrayList<>();
        Double[] probArray = coverQueryTopic.getProbArrayTrigram();

        for (String trigram : coverQueryTopic.getTrigramLM().keySet()) {
            double prob;
            if (cover_node == null) {
                prob = coverQueryTopic.getProbabilityTrigram(trigram, true);
            } else {
                prob = coverQueryTopic.getProbabilityTrigram(trigram, true, cover_node);
            }
            String[] split = trigram.split(" ");
            String prevBigram = split[0] + " " + split[1];
            if (prevBigram.equals(bigram)) {
                int bNum = getBucketNumber(prob, probArray);
                if (bNum == bucket_num) {
                    possibleCoverQ.add(trigram);
                } else if (Objects.equals(probArray[bNum - 1], probArray[bucket_num - 1])) {
                    possibleCoverQ.add(trigram);
                }
            }
        }

        if (possibleCoverQ.size() > 0) {
            int coverQNum = getRandom(0, possibleCoverQ.size());
            return possibleCoverQ.get(coverQNum);
        }
        return null;
    }

    /**
     * Generate cover query of length 4 using fourgram language model.
     *
     * @param bucket_num
     * @param cover_node
     * @param profile
     * @param currentQuery
     * @return the cover query
     */
    public UserQuery getCQfromFourgramLM(int bucket_num, TopicTreeNode cover_node, UserQuery currentQuery, Profile profile) {
        Topic coverQueryTopic = cover_node.getTopic();
        coverQueryTopic.prepare(SEARCHER, cover_node.getTopic_name(), 4);
        if (coverQueryTopic.isEmpty()) {
            return null;
        }
        ArrayList<String> possibleCoverQ = new ArrayList<>();
        Double[] probArray = coverQueryTopic.getProbArrayFourgram();

        for (String trigram : coverQueryTopic.getFourgramLM().keySet()) {
            double prob = coverQueryTopic.getProbabilityFourgram(trigram, true);
//            double prob = coverQueryTopic.getProbabilityFourgram(trigram, true, cover_node);
            int bNum = getBucketNumber(prob, probArray);
            if (bNum == bucket_num) {
                possibleCoverQ.add(trigram);
            } else if (Objects.equals(probArray[bNum - 1], probArray[bucket_num - 1])) {
                possibleCoverQ.add(trigram);
            }
        }
        coverQueryTopic.reset();

        if (possibleCoverQ.size() > 0) {
            UserQuery prevQuery = Helper.isInSessionTask(profile.getLastSession(), currentQuery);
            ArrayList<UserQuery> prevCoverQueries = null;
            if (prevQuery != null) {
                prevCoverQueries = prevQuery.getCover_queries();
            }

            for (int i = 0; i < possibleCoverQ.size(); i++) {
                int coverQNum = getRandom(0, possibleCoverQ.size());
                UserQuery cQuery = cQuery = new UserQuery(0, possibleCoverQ.get(coverQNum));
                Intent coverIntent = new Intent(cover_node.getTopic_name());
                cQuery.setQuery_intent(coverIntent);

                if (prevCoverQueries != null) {
                    boolean flag = true;
                    for (UserQuery temp : prevCoverQueries) {
                        if (Helper.checkSameTask(temp, cQuery)) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        continue;
                    }
                }
                return cQuery;
            }
        }
        return null;
    }

    /**
     * Returns a fourgram from a given a topic and bucket number which contains
     * the trigram provided.
     *
     * @param coverQueryTopic
     * @param bucketNum
     * @param trigram
     * @return
     */
    private String getFourGramFromLM(int bucket_num, Topic coverQueryTopic, String trigram, TopicTreeNode cover_node) {
        ArrayList<String> possibleCoverQ = new ArrayList<>();
        Double[] probArray = coverQueryTopic.getProbArrayFourgram();

        for (String fourgram : coverQueryTopic.getFourgramLM().keySet()) {
            double prob;
            if (cover_node == null) {
                prob = coverQueryTopic.getProbabilityFourgram(fourgram, true);
            } else {
                prob = coverQueryTopic.getProbabilityFourgram(fourgram, true, cover_node);
            }

            String[] split = fourgram.split(" ");
            String prevTrigram = split[0] + " " + split[1] + " " + split[2];
            if (prevTrigram.equals(trigram)) {
                int bNum = getBucketNumber(prob, probArray);
                if (bNum == bucket_num) {
                    possibleCoverQ.add(trigram);
                } else if (Objects.equals(probArray[bNum - 1], probArray[bucket_num - 1])) {
                    possibleCoverQ.add(trigram);
                }
            }
        }

        if (possibleCoverQ.size() > 0) {
            int coverQNum = getRandom(0, possibleCoverQ.size());
            return possibleCoverQ.get(coverQNum);
        }
        return null;
    }

    /**
     * Generate cover query of length greater than 4 using a special procedure.
     *
     * @param query_length
     * @param bucket_num
     * @param cover_node
     * @param profile
     * @param currentQuery
     * @return the cover query
     */
    public UserQuery getCQfromNgramLM(int query_length, int bucket_num, TopicTreeNode cover_node, UserQuery currentQuery, Profile profile) {
        Topic coverQueryTopic = cover_node.getTopic();
        coverQueryTopic.prepare(SEARCHER, cover_node.getTopic_name(), -1);
        if (coverQueryTopic.isEmpty()) {
            return null;
        }

        ArrayList<String> cQuery = new ArrayList<>();
        for (int k = 0; k < query_length; k++) {
            String tempFourgram;
            if (cQuery.size() >= 3) {
                String trigram = "";
                for (int x = cQuery.size() - 3; x < cQuery.size(); x++) {
                    trigram += cQuery.get(x) + " ";
                }
                trigram = trigram.trim();
                tempFourgram = getFourGramFromLM(bucket_num, coverQueryTopic, trigram, null);
//                tempFourgram = getFourGramFromLM(bucket_num, coverQueryTopic, trigram, cover_node);
            } else {
                tempFourgram = null;
            }
            if (tempFourgram == null) {
                String tempTrigram;
                if (cQuery.size() >= 2) {
                    int l = cQuery.size() - 1;
                    String bigram = cQuery.get(l - 1) + " " + cQuery.get(l);
                    tempTrigram = getTriGramFromLM(bucket_num, coverQueryTopic, bigram, null);
//                    tempTrigram = getTriGramFromLM(bucket_num, coverQueryTopic, bigram, cover_node);
                } else {
                    tempTrigram = null;
                }
                if (tempTrigram == null) {
                    String tempBigram;
                    if (cQuery.size() >= 1) {
                        String unigram = cQuery.get(cQuery.size() - 1);
                        tempBigram = getCQfromBigramLM(bucket_num, coverQueryTopic, unigram, null);
//                        tempBigram = getCQfromBigramLM(bucket_num, coverQueryTopic, unigram, cover_node);
                    } else {
                        tempBigram = null;
                    }
                    if (tempBigram == null) {
                        String tempUnigram = getUniGramFromLM(bucket_num, coverQueryTopic, null);
//                        String tempUnigram = getUniGramFromLM(bucket_num, coverQueryTopic, cover_node);
                        if (tempUnigram != null) {
                            cQuery.add(tempUnigram);
                        }
                    } else {
                        List<String> tokens = tokenizer.TokenizeText(tempBigram);
                        if (tokens.size() == 2) {
                            cQuery.add(tokens.get(1));
                        }
                    }
                } else {
                    List<String> tokens = tokenizer.TokenizeText(tempTrigram);
                    if (tokens.size() == 3) {
                        cQuery.add(tokens.get(2));
                    }
                }
            } else {
                List<String> tokens = tokenizer.TokenizeText(tempFourgram);
                if (tokens.size() == 4) {
                    cQuery.add(tokens.get(3));
                }
            }
        }
        coverQueryTopic.reset();
        if (cQuery.size() < query_length) {
            return null;
        } else {
            String coverQ = "";
            for (String term : cQuery) {
                coverQ += term + " ";
            }
            coverQ = coverQ.trim();

            UserQuery query = new UserQuery(0, coverQ);
            Intent coverIntent = new Intent(cover_node.getTopic_name());
            query.setQuery_intent(coverIntent);

            return query;
        }
    }

}
