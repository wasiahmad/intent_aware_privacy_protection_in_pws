/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.main;

import edu.virginia.cs.extra.Helper;
import edu.virginia.cs.config.DeploymentConfig;
import edu.virginia.cs.config.RunTimeConfig;
import edu.virginia.cs.config.StaticData;
import edu.virginia.cs.model.TopicTree;
import edu.virginia.cs.model.TopicTreeNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 *
 * @author Wasi
 */
public class MultiThread {

    public static void main(String[] args) throws Exception {
        long startTime = System.nanoTime();
        MultiThread ml = new MultiThread();
        ml.loadParameters();
        ml.doInitialization();
        ml.createThreads();
        long endTime = System.nanoTime();
        double output = (endTime - startTime) / (60.0 * 1000000000);
        System.out.println("Total execution time = " + Math.round(output * 10000) / 10000.00 + " minutes.");
    }

    /**
     * Load all parameters.
     */
    private void loadParameters() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File("settings.txt")));
            RunTimeConfig.NumberOfCoverQuery = Integer.parseInt(br.readLine().replace("number of cover queries =", "").trim());
            RunTimeConfig.ClientSideRanking = br.readLine().replace("client side re-ranking =", "").trim().equals("on");
            RunTimeConfig.NumberOfThreads = Integer.parseInt(br.readLine().replace("number of threads =", "").trim());
            RunTimeConfig.IntervalInHours = Integer.parseInt(br.readLine().replace("interval in hours =", "").trim());
            RunTimeConfig.TotalDocInWeb = Integer.parseInt(br.readLine().replace("total documents in AOL index =", "").trim());
            RunTimeConfig.removeStopWordsInCQ = br.readLine().replace("remove stopwords from cover query =", "").trim().equals("yes");
            RunTimeConfig.doStemmingInCQ = br.readLine().replace("generate stemmed cover query =", "").trim().equals("yes");

            DeploymentConfig.AolIndexPath = br.readLine().replace("lucene AOL index directory =", "").trim();
            DeploymentConfig.OdpIndexPath = br.readLine().replace("lucene ODP index directory =", "").trim();
            DeploymentConfig.UserSearchLogPath = br.readLine().replace("users search log directory =", "").trim();
            DeploymentConfig.ReferenceModelPath = br.readLine().replace("reference model file =", "").trim();
            DeploymentConfig.AolDocFreqRecord = br.readLine().replace("AOL document frequency record =", "").trim();
            DeploymentConfig.OdpHierarchyRecord = br.readLine().replace("ODP hierarchy file =", "").trim();
            DeploymentConfig.BackgroundKnowledge = br.readLine().replace("background knowledge file =", "").trim();
            DeploymentConfig.TransitionMatrix = br.readLine().replace("transition matrix file =", "").trim();
            DeploymentConfig.TransitionProbability = br.readLine().replace("transition probability file =", "").trim();
            DeploymentConfig.OutDirectory = br.readLine().replace("output directory =", "").trim();
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(MultiThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Do initialization.
     */
    private void doInitialization() {
        File file = new File(DeploymentConfig.OutDirectory);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    /**
     * The main method that creates and starts threads.
     *
     * @param count number of threads need to be created and started.
     * @return
     */
    private void createThreads() throws InterruptedException {
        try {
            MyThread[] myT = new MyThread[RunTimeConfig.NumberOfThreads];
            /* Loading the reference model, idf record and all user ids */
            ArrayList<String> allUserId = Helper.getAllUserId(DeploymentConfig.UserSearchLogPath, -1);
            StaticData.loadRefModel(DeploymentConfig.ReferenceModelPath);
            StaticData.loadIDFRecord(DeploymentConfig.AolDocFreqRecord);

            int limit = allUserId.size() / RunTimeConfig.NumberOfThreads;
            for (int i = 0; i < RunTimeConfig.NumberOfThreads; i++) {
                int start = i * limit;
                ArrayList<String> list;
                if (i == RunTimeConfig.NumberOfThreads - 1) {
                    list = new ArrayList<>(allUserId.subList(start, allUserId.size()));
                } else {
                    list = new ArrayList<>(allUserId.subList(start, start + limit));
                }
                myT[i] = new MyThread(list, "thread_" + i);
                myT[i].start();
            }
            for (int i = 0; i < RunTimeConfig.NumberOfThreads; i++) {
                myT[i].getThread().join();
            }

            /* When all threads finished its execution, generate final result */
            double totalKLDivergence = 0.0;
            double totalMI = 0.0;
            double totalMAP = 0.0;
            double totalMet1 = 0.0;
            double totalMet2 = 0.0;
            double totalMet3 = 0.0;
            int totalUsers = 0;
            double totalQueries = 0;
            for (int i = 0; i < RunTimeConfig.NumberOfThreads; i++) {
                String[] result = myT[i].getResult().split("\t");
                totalUsers += Integer.parseInt(result[0]);
                totalQueries += Double.parseDouble(result[1]);
                totalMAP += Double.valueOf(result[2]);
                totalKLDivergence += Double.valueOf(result[3]);
                totalMI += Double.valueOf(result[4]);
                totalMet1 += Double.valueOf(result[5]);
                totalMet2 += Double.valueOf(result[6]);
                totalMet3 += Double.valueOf(result[7]);
            }
            double finalKL = totalKLDivergence / totalUsers;
            double finalMI = totalMI / totalUsers;
            double finalMAP = totalMAP / totalQueries;
            double finalMetric1 = totalMet1 / totalUsers;
            double finalMetric2 = totalMet2 / totalUsers;
            double finalMetric3 = totalMet3 / totalUsers;
            FileWriter fw = new FileWriter(DeploymentConfig.OutDirectory + "final_output.txt");
            fw.write("**************Parameter Settings**************\n");
            fw.write("Number of cover queries = " + RunTimeConfig.NumberOfCoverQuery + "\n");
            fw.write("**********************************************\n");
            fw.write("Total Number of users = " + totalUsers + "\n");
            fw.write("Total Number of queries tested = " + totalQueries + "\n");
            fw.write("Averge MAP = " + finalMAP + "\n");
            fw.write("Average KL-Divergence = " + finalKL + "\n");
            fw.write("Average Mutual Information = " + finalMI + "\n");
            fw.write("Average cIndex = " + finalMetric1 + "\n");
            fw.write("Average tIndex (using second order transition) = " + finalMetric2 + "\n");
            fw.write("Average tIndex (using first order transition) = " + finalMetric3 + "\n");
            fw.close();
        } catch (InterruptedException | NumberFormatException | IOException ex) {
            Logger.getLogger(MultiThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class MyThread implements Runnable {

    private Thread t = null;
    private final ArrayList<String> userIds;
    private final String threadId;
    private String result;

    public MyThread(ArrayList<String> listUsers, String id) {
        this.userIds = listUsers;
        this.threadId = id;
    }

    /**
     * Overriding the run method of the Thread class.
     */
    @Override
    public void run() {
        try {
            Evaluate evaluate = new Evaluate();
            result = evaluate.startEvaluation(userIds, threadId);
        } catch (Throwable ex) {
            Logger.getLogger(MyThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Method to start the thread.
     */
    public void start() {
        if (t == null) {
            t = new Thread(this);
            t.start();
        }
    }

    public String getResult() {
        return result;
    }

    /**
     * Method to return the thread object.
     *
     * @return thread object
     */
    public Thread getThread() {
        return t;
    }
}
