/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.model;

import edu.virginia.cs.config.DeploymentConfig;
import edu.virginia.cs.config.RunTimeConfig;
import edu.virginia.cs.engine.OkapiBM25;
import edu.virginia.cs.engine.Searcher;
import edu.virginia.cs.extra.Helper;
import static edu.virginia.cs.extra.Helper.getRandom;
import edu.virginia.cs.interfaces.TreeNode;
import edu.virginia.cs.object.Topic;
import edu.virginia.cs.object.UserQuery;
import edu.virginia.cs.user.Intent;
import edu.virginia.cs.user.Profile;
import edu.virginia.cs.utility.TextTokenizer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author wua4nw
 */
public class GenerateCoverQuery {

    private final TopicTree topicTree;
    private final TextTokenizer tokenizer;
    private final Searcher SEARCHER;
    private final GenerateQuery gQuery;

    public GenerateCoverQuery(TopicTree tree) {
        this.topicTree = tree;
        this.SEARCHER = new Searcher(DeploymentConfig.OdpIndexPath);
        this.SEARCHER.setSimilarity(new OkapiBM25());
        this.tokenizer = new TextTokenizer(RunTimeConfig.removeStopWordsInCQ, RunTimeConfig.doStemmingInCQ);
        this.gQuery = new GenerateQuery(SEARCHER, tokenizer);
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
     * Generate a list of integers where the first value is the query length,
     * second value is the inferred topic number and the last one is the bucket
     * number from which the query belongs.
     *
     * @param queryTopicNode node of the topic tree from where query belongs to
     * @param query true user query
     * @return list of integers
     */
    private int getBucketNumber(TopicTreeNode queryTopicNode, UserQuery query) {
        Topic topic = queryTopicNode.getTopic();
        List<String> tokens = tokenizer.TokenizeText(query.getQuery_text());
        String modifiedQuery = "";
        for (String token : tokens) {
            modifiedQuery += token + " ";
        }
        modifiedQuery = modifiedQuery.trim();
        int query_length = tokens.size();
        query.setQuery_length(query_length);
        int bucket_num;

        topic.prepare(SEARCHER, queryTopicNode.getTopic_name(), query_length);
        switch (query_length) {
            case 1: {
                Double[] probArray = topic.getProbArrayUnigram();
                double prob = topic.getProbabilityUnigram(modifiedQuery);
//                double prob = topic.getProbabilityUnigram(modifiedQuery, queryTopicNode);
                bucket_num = getBucketNumber(prob, probArray);
                break;
            }
            case 2: {
                Double[] probArray = topic.getProbArrayBigram();
                double prob = topic.getProbabilityBigram(modifiedQuery, true);
//                double prob = topic.getProbabilityBigram(modifiedQuery, true, queryTopicNode);
                bucket_num = getBucketNumber(prob, probArray);
                break;
            }
            case 3: {
                Double[] probArray = topic.getProbArrayTrigram();
                double prob = topic.getProbabilityTrigram(modifiedQuery, true);
//                double prob = topic.getProbabilityTrigram(modifiedQuery, true, queryTopicNode);
                bucket_num = getBucketNumber(prob, probArray);
                break;
            }
            case 4: {
                Double[] probArray = topic.getProbArrayFourgram();
                double prob = topic.getProbabilityFourgram(modifiedQuery, true);
//                double prob = topic.getProbabilityFourgram(modifiedQuery, true, queryTopicNode);
                bucket_num = getBucketNumber(prob, probArray);
                break;
            }
            default: {
                Double[] probArray = topic.getProbArrayFourgram();
                double prob = topic.getProbabilityNgram(modifiedQuery, query_length);
//                double prob = topic.getProbabilityNgram(modifiedQuery, query_length, queryTopicNode);
                bucket_num = getBucketNumber(prob, probArray);
                break;
            }
        }

        topic.reset();
        return bucket_num;
    }

    /**
     * Computes and return a cover query topic.
     *
     * @param level
     * @param queryTopic
     * @param fromSibling
     * @return the cover query topic
     */
    private TopicTreeNode getCoverQueryTopic(TopicTreeNode queryTopic, boolean fromSibling) {
        ArrayList<TreeNode> listNodes = new ArrayList<>();
        if (fromSibling) {
            /**
             * Selecting a cover query topic only from sibling topics.
             */
            listNodes.addAll(queryTopic.getSiblings());
        } else {
            /**
             * Selecting a cover query topic from same level but from
             * non-sibling topics.
             */
            for (TreeNode node : topicTree.getNodesOfLevel(queryTopic.getNodeLevel())) {
                if (((TopicTreeNode) node.getParent()).getTopic_id() != ((TopicTreeNode) queryTopic.getParent()).getTopic_id()) {
                    listNodes.add(node);
                }
            }
        }
        if (listNodes.isEmpty()) {
            return null;
        }
        int topic = getRandom(0, listNodes.size());
        return (TopicTreeNode) listNodes.get(topic);
    }

    /**
     * Check if the user query is sequentially edited or not.
     *
     * @param previousQuTopic
     * @param currentQuTopic
     * @return
     */
    private int isSequentiallyEdited(UserQuery previousQuery, UserQuery currentQuery) {
        /**
         * Check if current query intent is the parent of previous query intent,
         * then sequential editing is true.
         */
        if (currentQuery.getQuery_intent().isParent(previousQuery.getQuery_intent())) {
            return 1;
        }
        /**
         * Check if current query intent is the child of previous query intent,
         * then sequential editing is true.
         */
        if (previousQuery.getQuery_intent().isParent(currentQuery.getQuery_intent())) {
            return -1;
        }
        /**
         * Current query topic is neither parent nor child of the previous query
         * topic, so return 0.
         */
        return 0;
    }

    private UserQuery generateCoverQuery(int buck_num, TopicTreeNode cover_node, UserQuery currentQuery, Profile profile) {
        int coverQuLen = Helper.getPoisson(currentQuery.getQuery_length());
        if (coverQuLen == 0) {
            return null;
        }
        UserQuery coverQuery;
        switch (coverQuLen) {
            case 1:
                coverQuery = gQuery.getCQfromUnigramLM(buck_num, cover_node, currentQuery, profile);
                break;
            case 2:
                coverQuery = gQuery.getCQfromBigramLM(buck_num, cover_node, currentQuery, profile);
                break;
            case 3:
                coverQuery = gQuery.getCQfromTrigramLM(buck_num, cover_node, currentQuery, profile);
                break;
            case 4:
                coverQuery = gQuery.getCQfromFourgramLM(buck_num, cover_node, currentQuery, profile);
                break;
            default:
                coverQuery = gQuery.getCQfromNgramLM(coverQuLen, buck_num, cover_node, currentQuery, profile);
                break;
        }
        return coverQuery;
    }

    public ArrayList<UserQuery> generateCoverQueries(Profile profile, UserQuery query) {
        ArrayList<UserQuery> coverQueries = new ArrayList<>();
        /* step 1. get the topic of user query */
        String topic_name = query.getQuery_intent().getName();
        TopicTreeNode topicNode = (TopicTreeNode) topicTree.getTreeNode(topic_name);

        /* step 2. infer the bucket number for the user query */
        if (topicNode == null) {
            System.err.println("Fatal Exception: Topic (" + topic_name + ") not found. Exiting...");
            System.exit(1);
        }
        int bucket_num = getBucketNumber(topicNode, query);

        /* step 3. check if the current query is sequentially edited */
        UserQuery lastSubmittedQuery = profile.getLastSubmittedQuery();
        int isSeqEdited = 0;
        if (lastSubmittedQuery != null) {
            isSeqEdited = isSequentiallyEdited(lastSubmittedQuery, query);
        }

        /* step 4. create required number of cover queries */
        int count = 0;
        ArrayList<UserQuery> previousCoverQueries = null;
        if (lastSubmittedQuery != null && isSeqEdited != 0) {
            previousCoverQueries = lastSubmittedQuery.getCover_queries();
        }

        boolean fromSiblingPossible = true;
        int numberOfAttempts = 0;
        while (true) {
            UserQuery coverQuery = null;
            if (isSeqEdited == 0) {
                /* Current query is not sequentially edited. */
                if (count < RunTimeConfig.NumberOfCoverQuery / 2 && fromSiblingPossible) {
                    /* Generating cover query from sibling topics. */
                    TopicTreeNode cover_node = getCoverQueryTopic(topicNode, true);
                    if (cover_node == null) {
                        fromSiblingPossible = false;
                    } else {
                        coverQuery = generateCoverQuery(bucket_num, cover_node, query, profile);
                    }
                } else {
                    /* Generating cover query from same level of original query but not from sibling topics. */
                    TopicTreeNode cover_node = getCoverQueryTopic(topicNode, false);
                    if (cover_node != null) {
                        coverQuery = generateCoverQuery(bucket_num, cover_node, query, profile);
                    }
                }

            } else {
                if (previousCoverQueries == null || count >= previousCoverQueries.size()) {
                    /* cover queries can't be generated based on previous cover queries */
                    isSeqEdited = 0;
                    continue;
                }
                /**
                 * Current query is sequentially edited, so cover queries will
                 * be generated based on previous cover query topics.
                 */
                if (isSeqEdited == 1) {
                    /**
                     * Cover query should be generated from parent topic of the
                     * previous cover query.
                     */
                    String parent_name = ((Intent) previousCoverQueries.get(count).getQuery_intent().getParent()).getName();
                    TopicTreeNode cover_node = (TopicTreeNode) topicTree.getTreeNode(parent_name);
                    coverQuery = generateCoverQuery(bucket_num, cover_node, query, profile);
                } else {
                    /**
                     * Cover query should be generated from child topic of the
                     * previous cover query.
                     */
                    List<TreeNode> childrens = previousCoverQueries.get(count).getQuery_intent().getChildrens();
                    if (!childrens.isEmpty()) {
                        int rand = -1;
                        for (int x = 0; x < childrens.size(); x++) {
                            rand = Helper.getRandom(0, childrens.size());
                            Topic topic = ((TopicTreeNode) childrens.get(rand)).getTopic();
                            if (!topic.isEmpty()) {
                                break;
                            } else {
                                rand = -1;
                            }
                        }
                        if (rand == -1) {
                            /* cover queries can't be generated from child topic of the previous query */
                            isSeqEdited = 0;
                        } else {
                            TopicTreeNode cover_node = ((TopicTreeNode) childrens.get(rand));
                            coverQuery = generateCoverQuery(bucket_num, cover_node, query, profile);
                        }
                    } else {
                        /* cover queries can't be generated from child topic of the previous query */
                        isSeqEdited = 0;
                    }
                }
            }

            if (coverQuery != null) {
                coverQuery.setQuery_time(query.getQuery_time());
                coverQueries.add(coverQuery);
                count++;
            }

            if (count == RunTimeConfig.NumberOfCoverQuery && isSeqEdited == 0) {
                break;
            } else if (isSeqEdited != 0 && previousCoverQueries != null && count == previousCoverQueries.size()) {
                break;
            } else if (numberOfAttempts == (RunTimeConfig.NumberOfCoverQuery * 10)) {
                break;
            }
            numberOfAttempts++;
        }

        return coverQueries;
    }
}
