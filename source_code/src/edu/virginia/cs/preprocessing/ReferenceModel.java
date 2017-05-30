/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.preprocessing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author wasi
 */
public class ReferenceModel {

    private final HashMap<String, Integer> refModel;
    private int totalTokenInCorpus;

    public ReferenceModel() {
        refModel = new HashMap<>();
        totalTokenInCorpus = 0;
    }

    /**
     * Loads all dictionary words and their term frequency.
     *
     * @param filePath
     * @throws IOException
     */
    public void loadDictionaryWords(String filePath) throws IOException {
        if (filePath != null) {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            totalTokenInCorpus += Integer.parseInt(br.readLine());
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    Integer n = refModel.get(parts[0]);
                    if (n == null) {
                        n = Integer.parseInt(parts[1]);
                    } else {
                        n = n + Integer.parseInt(parts[1]);
                    }
                    refModel.put(parts[0], n);
                } else {
                    System.out.println("Dictionary entry is not in right format in: " + line);
                }
            }
            br.close();
        } else {
            System.out.println("No user found with the file name: " + filePath);
        }
    }

    /**
     * Method that creates the reference model. This model is created over
     * crawled AOL data and DMOZ data.
     *
     * @param filePath1
     * @param filePath2
     * @throws java.io.IOException
     */
    public void createReferenceModel(String filePath1, String filePath2)
            throws IOException {
        loadDictionaryWords(filePath1);
        loadDictionaryWords(filePath2);
        //build reference model
        FileWriter fw = new FileWriter("./data/Reference-Model");
        for (String name : refModel.keySet()) {
            Integer value = refModel.get(name);
            Double tokenProb = (value * 1.0) / totalTokenInCorpus;
            fw.write(name + "\t" + tokenProb + "\n");
            fw.flush();
        }
        fw.close();
    }

    /**
     * Method that generates the reference model using all user's search log, it
     * needs to be executed once only. Reference model is stored, so that it can
     * be used for future use.
     *
     * @param args
     * @throws java.lang.Throwable
     */
    public static void main(String[] args) throws Throwable {
        ReferenceModel refUserModel = new ReferenceModel();
        refUserModel.createReferenceModel("./data/AOL-Dictionary-TF", "./data/ODP-Dictionary");
    }
}
