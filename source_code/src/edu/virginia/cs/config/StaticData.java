/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.config;

import edu.virginia.cs.io.FileIO;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author wua4nw
 */
public class StaticData {

    public static HashMap<String, Double> SmoothingReference;
    public static HashMap<String, Double> IDFRecord;

    /**
     * Method to load the reference model which is generated previously.
     *
     * @param filename
     */
    public static void loadRefModel(String filename) {
        SmoothingReference = new HashMap<>();
        ArrayList<String> lines = FileIO.LoadFile(filename, -1);
        for (String line : lines) {
            line = line.trim();
            String[] words = line.split("\t");
            if (words.length == 2) {
                SmoothingReference.put(words[0], Double.valueOf(words[1]));
            } else {
                System.err.println("Error in " + filename + " format!");
            }
        }
    }

    /**
     * Load IDF of AOL dictionary.
     *
     * @param filename
     */
    public static void loadIDFRecord(String filename) {
        IDFRecord = new HashMap<>();
        ArrayList<String> lines = FileIO.LoadFile(filename, -1);
        double totalCount = Double.valueOf(lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            String[] split = lines.get(i).split("\t");
            double docFreq = 1 + Math.log10(totalCount / Double.valueOf(split[1]));
            IDFRecord.put(split[0], docFreq);
        }
    }

}
