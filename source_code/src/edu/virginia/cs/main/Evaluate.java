package edu.virginia.cs.main;

import edu.virginia.cs.extra.ODPCategoryReader;
import edu.virginia.cs.extra.Helper;
import edu.virginia.cs.config.DeploymentConfig;
import edu.virginia.cs.config.RunTimeConfig;
import edu.virginia.cs.engine.OkapiBM25;
import edu.virginia.cs.engine.Searcher;
import edu.virginia.cs.model.ClassifyIntent;
import edu.virginia.cs.model.GenerateCoverQuery;
import edu.virginia.cs.model.TopicTree;
import edu.virginia.cs.model.TopicTreeNode;
import edu.virginia.cs.object.ResultDoc;
import edu.virginia.cs.object.Session;
import edu.virginia.cs.object.UserQuery;
import edu.virginia.cs.user.Intent;
import edu.virginia.cs.user.Profile;
import edu.virginia.cs.utility.Converter;
import java.io.IOException;
import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.FileWriter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

public class Evaluate {

    private final Searcher _searcher;
    /* Storing a specific user's queries and corresponding all clicked documents */
    private List<UserQuery> userQueries;
    /* Storing a specific user's all queries */
    private ArrayList<String> listOfUserQuery;
    /* Storing a specific user's all queries */
    private ArrayList<String> listOfCoverQuery;
    /* Object for intent classification of the user query */
    private final ClassifyIntent classifyIntent;
    /* Object to generate cover queries for a specific user query */
    private final GenerateCoverQuery gCoverQuery;
    /* User profile which is constructed and maintained in the client side */
    private Profile profile;
    /* Total MAP for 'n' users that we are evaluating, ex. in our case, n = 1000 */
    private double totalMAP = 0.0;
    /* Total number of queries evaluated for 'n' users, ex. in our case, n = 1000 */
    private double totalQueries = 0.0;
    /* Total KL-Divergence for 'n' users that we are evaluating, ex. in our case, n = 1000 */
    private double totalKL = 0;
    /* Total mutual information for 'n' users that we are evaluating, ex. in our case, n = 1000 */
    private double totalNMI = 0;
    /* Total goodness of alignment score for 'n' users that we are evaluating, ex. in our case, n = 1000 */
    private double totalMet1 = 0;
    private double totalMet2 = 0;
    private double totalMet3 = 0;

    private final NMICalculation computeNMI;
    private final cIndex metric1;
    private final tIndex_v1 metric2;
    private final tIndex_v2 metric3;

    public Evaluate() {
        this._searcher = new Searcher(DeploymentConfig.AolIndexPath);
        this._searcher.setSimilarity(new OkapiBM25());
        // setting the flag to enable personalization
        this._searcher.activatePersonalization(true);
        TopicTree tree = createTopicTree(4);
        if (tree == null) {
            System.err.println("Failed to load ODP category hierarchy");
            System.exit(1);
        }
        this.gCoverQuery = new GenerateCoverQuery(tree);
        this.classifyIntent = new ClassifyIntent();
        this.computeNMI = new NMICalculation(DeploymentConfig.AolIndexPath);
        this.metric1 = new cIndex(RunTimeConfig.IntervalInHours);
        this.metric2 = new tIndex_v1(RunTimeConfig.IntervalInHours);
        this.metric3 = new tIndex_v2(RunTimeConfig.IntervalInHours);
    }

    /**
     * Loads language models up to level 'param' from all language models of
     * DMOZ categories.
     *
     * @param filename
     * @param depth depth of the hierarchy
     * @return list of language models
     */
    private TopicTree createTopicTree(int depth) {
        TopicTree topicTree = null;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            ArrayList<String> topics = null;
            try {
                FileInputStream fis = new FileInputStream(DeploymentConfig.OdpHierarchyRecord);
                ODPCategoryReader odpCatReader = new ODPCategoryReader(depth);
                saxParser.parse(fis, odpCatReader);
                topics = odpCatReader.getTopics();
            } catch (SAXException | IOException ex) {
                Logger.getLogger(MultiThread.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (topics == null) {
                return topicTree;
            }

            topicTree = new TopicTree();
            int node_id = 0;

            for (String currentTopic : topics) {
                String[] split = currentTopic.split("/");
                int level = split.length - 1;

                TopicTreeNode node = new TopicTreeNode(currentTopic, node_id);
                node.setNodeLevel(level);

                if (split.length >= 2) {
                    String parent = currentTopic.substring(0, currentTopic.lastIndexOf("/"));
                    if (topicTree.exists(parent)) {
                        node.setParent(topicTree.getTreeNode(parent));
                        topicTree.getTreeNode(parent).addChildren(node);
                    } else {
                        System.err.println("Problem while loading ODP topic hierarchy...");
                        System.exit(1);
                    }
                } else {
                    node.setParent(null);
                }

                topicTree.addNode(currentTopic, node);
                node_id++;
            }

        } catch (ParserConfigurationException | SAXException ex) {
            Logger.getLogger(MultiThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        return topicTree;
    }

    /**
     * Main method that executes the entire pipeline.
     *
     * @param allUserId
     * @param threadId
     * @return
     * @throws java.lang.Throwable
     */
    public String startEvaluation(ArrayList<String> allUserId, String threadId) throws Throwable {
        int countUsers = 0;
        FileWriter writer = new FileWriter(DeploymentConfig.OutDirectory + threadId + ".txt");
        for (String userId : allUserId) {
            countUsers++;
            // initializing user profile of the server side and setting the reference model
            _searcher.initializeUserProfile(userId);

            // required for calculating mutual information between user queris and cover queries
            listOfUserQuery = new ArrayList<>();
            listOfCoverQuery = new ArrayList<>();

            // generate the clicked urls for evaluation
            loadUserJudgements(userId);
            // initialization for client side user profile
            profile = new Profile(userId);

            double meanAvgPrec = 0.0;
            // Number of queries evaluated
            int numQueries = 0;

            /**
             * query contains query plus the timestamp when the query was
             * submitted.
             */
            for (UserQuery query : userQueries) {
                /**
                 * If a user query has at least one corresponding clicked
                 * document, then we evaluate it, otherwise not.
                 *
                 */
                if (!query.getRelevant_documents().isEmpty()) {
                    UserQuery lastSubmittedQuery = profile.getLastSubmittedQuery();
                    boolean isSame = false;
                    if (lastSubmittedQuery != null) {
                        isSame = Helper.checkSameSession(lastSubmittedQuery, query);
                    }

                    // current query and previous query (if any) are from different session
                    if (!isSame) {
                        if (lastSubmittedQuery != null) {
                            // set end time of previous session
                            profile.getLastSession().setEnd_time(lastSubmittedQuery.getQuery_time());
                        }
                        // start of a new user session
                        Session session = new Session(profile.getSessions().size());
                        session.setStart_time(query.getQuery_time());
                        profile.addSession(session);
                    }

                    listOfUserQuery.add(query.getQuery_text());
                    // computing average precision for a query
                    double avgPrec = AvgPrec(query);
                    meanAvgPrec += avgPrec;
                    ++numQueries;
                }
            }

            // totalMAP = sum of all MAP computed for queries of 'n' users
            totalMAP += meanAvgPrec;
            // totalQueries = total number of queries for 'n' users
            totalQueries += numQueries;
            // compute MAP for the current user
            double MAP = meanAvgPrec / numQueries;

            double klDivergence = 0;
            double mutualInfo = 0;
            double met1 = 0;
            double met2 = 0;
            double met3 = 0;

            if (RunTimeConfig.NumberOfCoverQuery != 0) {
                // computing KL-Divergence from true user profile to noisy user profile.
                klDivergence = (double) _searcher.getUserProfile().calculateKLDivergence(profile);
                // totalKL = sum of all LL-Divergence computed for 'n' users
                totalKL += klDivergence;

                // compute mutual information for the current user
                mutualInfo = computeNMI.calculateNMI(listOfUserQuery, listOfCoverQuery);
                // totalMI = sum of all MI computed for 'n' users
                totalNMI += mutualInfo;

                met1 = metric1.evaluateComponents(profile);
                totalMet1 += met1;

                met2 = metric2.evaluateTransitions(profile);
                totalMet2 += met2;

                met3 = metric3.evaluateTransitions(profile);
                totalMet3 += met3;
            }
            writer.write(countUsers + "\t" + Integer.parseInt(userId) + "\t" + MAP + "\t" + klDivergence + "\t" + mutualInfo + "\t" + met1 + "\t" + met2 + "\t" + met3 + "\n");
            writer.flush();
            System.out.printf("%-8d\t%-8d\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\n", countUsers, Integer.parseInt(userId), MAP, klDivergence, mutualInfo, met1, met2, met3);
        }

        double avgKL = 0;
        double avgMI = 0;
        double avgMet1 = 0;
        double avgMet2 = 0;
        double avgMet3 = 0;
        double finalMAP = totalMAP / totalQueries;
        if (countUsers > 0) {
            avgKL = totalKL / countUsers;
            avgMI = totalNMI / countUsers;
            avgMet1 = totalMet1 / countUsers;
            avgMet2 = totalMet2 / countUsers;
            avgMet3 = totalMet3 / countUsers;
        }

        writer.write("\n************Result after full pipeline execution for n users**************" + "\n");
        writer.write("\nTotal number of users : " + countUsers + "\n");
        writer.write("Total number of quries tested : " + totalQueries + "\n");
        writer.write("MAP : " + finalMAP + "\n");
        writer.write("Average KL : " + avgKL + "\n");
        writer.write("Average MI : " + avgMI + "\n");
        writer.write("Average Plausibility Ranking : " + avgMet1 + "\n");
        writer.write("Average Plausibility Ranking (with transition) : " + avgMet2 + "\n");
        writer.write("Average Plausibility Ranking (with transition) : " + avgMet3 + "\n");
        writer.flush();
        writer.close();

        String retValue = countUsers + "\t" + totalQueries + "\t" + totalMAP + "\t" + totalKL + "\t" + totalNMI + "\t" + totalMet1 + "\t" + totalMet2 + "\t" + totalMet3;
        return retValue;
    }

    /**
     * Method that computes average precision of a user submitted query.
     *
     * @param query user's original query
     * @param clickedDocs clicked documents for the true user query
     * @return average precision
     */
    private double AvgPrec(UserQuery query) throws Throwable {
        // generating the cover queries
        double avgp = 0.0;
        Intent queryIntent = classifyIntent.inferQueryIntent(query);
        if (queryIntent == null) {
            /* couldn't classify user query intent, so can't submit it */
            return avgp;
        }
        query.setQuery_intent(queryIntent);

        if (RunTimeConfig.NumberOfCoverQuery == 0) {
            // if no cover query is required, just submit the original query
            avgp = submitOriginalQuery(query);
        } else {
            ArrayList<UserQuery> coverQueries;
            /**
             * If the user is repeating a query in the same session, same set of
             * cover queries will be submitted to the search engine.
             */
            UserQuery repeatQuery = profile.getLastSession().checkRepeat(query);
            if (repeatQuery == null) {
                coverQueries = gCoverQuery.generateCoverQueries(profile, query);
            } else {
                /* User has repeated a query in the same session */
                coverQueries = repeatQuery.getCover_queries();
            }
            /**
             * if for some reason cover queries are not generated properly, no
             * query will be submitted to the search engine.
             */
            if (coverQueries == null || coverQueries.isEmpty()) {
                return avgp;
            }

            //System.out.println("True Query: " + query.getQuery_text());
            //for (UserQuery q : coverQueries) {
            //System.out.println(q.getQuery_text());
            //}
            //System.out.println();
            int randNum = (int) (Math.random() * coverQueries.size());
            for (int k = 0; k < coverQueries.size(); k++) {
                listOfCoverQuery.add(coverQueries.get(k).getQuery_text());
                // submitting cover query to the search engine
                ArrayList<ResultDoc> searchResults = _searcher.search(coverQueries.get(k)).getDocs();
                // generating fake clicks for the cover queries, one click per cover query
                if (!searchResults.isEmpty()) {
                    int rand = (int) (Math.random() * searchResults.size());
                    ResultDoc rdoc = searchResults.get(rand);
                    rdoc.setClicked();
                    coverQueries.get(k).addRelevant_document(rdoc);
                    // user clicks a document
                    _searcher.clickDocument(coverQueries.get(k), rdoc);
                }
                // submitting the original user query to the search engine
                if (k == randNum) {
                    avgp = submitOriginalQuery(query);
                }
                query.addCover_query(coverQueries.get(k));
            }

        }
        return avgp;
    }

    /**
     * Submit the original query to search engine and computes average precision
     * of the search results.
     *
     * @param query
     * @param clickedDocs
     * @return
     * @throws IOException
     */
    private double submitOriginalQuery(UserQuery query) throws IOException {
        double avgp = 0.0;
//        System.out.println("Query: " + query.getQuery_text());
//        System.out.println("Complete Profile: " + _searcher.getUserProfile().getCompleteHistory().toString());
        ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
        if (results.isEmpty()) {
            return avgp;
        }
        // re-rank the results based on the user profile kept in client side
        if (RunTimeConfig.ClientSideRanking) {
            results = Helper.reRankResults(profile.getCompleteHistory(), results);
//            results = Helper.reRankResults(profile.getBranchHistory(query.getQuery_intent()), results);
//            results = Helper.reRankResults(profile.getSessionHistory(query.getQuery_intent()), results);
        }
        int i = 1;
        double numRel = 0;
        for (ResultDoc rdoc : results) {
            ResultDoc relDoc = query.relDoc_Contains(rdoc);
            if (relDoc != null) {
                relDoc.setClicked();
                relDoc.setContent(rdoc.getContent());
                numRel++;
                avgp = avgp + (numRel / i);
//                System.out.println(i + " -- " + relDoc.getUrl());
//                System.out.println("Document Score: " + rdoc.getBM25Score() + " [BM25], " + rdoc.getPersonalizationScore() + " [Persona. Score]");
                // update user profile kept in the server side
                _searcher.clickDocument(query, rdoc);
            }
            ++i;
        }
//        System.out.println("##############################################");
        avgp = avgp / query.getRelevant_documents().size();
        // updating user profile kept in client side
        query.setQuery_intent(profile.addIntent(query.getQuery_intent()));
        boolean success = profile.addQuery(query);
        if (!success) {
            System.err.println("Failed to update user profile and now exiting...");
            System.exit(1);
        }
        profile.getLastSession().addUser_queries(query);
        return avgp;
    }

    /**
     * Method that generates a mapping between each user query and corresponding
     * clicked documents.
     *
     * @param userId
     * @throws java.lang.Throwable
     */
    private void loadUserJudgements(String userId) {
        userQueries = new ArrayList<>();
        String judgeFile = DeploymentConfig.UserSearchLogPath + userId + ".txt";
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(judgeFile));
            String line;
            boolean isQuery = false;
            UserQuery uq = null;
            int query_id = 0;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    isQuery = false;
                    continue;
                }
                if (!isQuery) {
                    isQuery = true;
                    String[] terms = line.split("\t");
                    uq = new UserQuery(query_id, terms[0]);
                    uq.setQuery_time(Converter.convertStringToDate(terms[1]));
                    userQueries.add(uq);
                    query_id++;
                } else {
                    ResultDoc doc = new ResultDoc();
                    doc.setUrl(line);
                    if (uq != null) {
                        uq.addRelevant_document(doc);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Evaluate.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
