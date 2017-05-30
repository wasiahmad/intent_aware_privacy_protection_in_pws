/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.main;

import edu.virginia.cs.config.RunTimeConfig;
import edu.virginia.cs.engine.Searcher;
import edu.virginia.cs.extra.Constants;
import edu.virginia.cs.utility.TextTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wua4nw
 */
public class NMICalculation {

    /* data structure for mutual information measurement */
    private final TextTokenizer tokenizer;
    private final Searcher _searcher;

    public NMICalculation(String indexPath) {
        tokenizer = new TextTokenizer(true, true);
        _searcher = new Searcher(indexPath);
    }

    /**
     * Computing probability for a query.
     *
     * @param query
     * @return
     */
    private double getProbability(String query) {
        double probQuery = 0;
        try {
            if (query.isEmpty()) {
                return 0;
            }
            List<String> tokens = tokenizer.TokenizeText(query);
            if (!tokens.isEmpty()) {
                int docFreqCount = getDocFrequency(tokens);
                probQuery = (docFreqCount * 1.0) / RunTimeConfig.TotalDocInWeb;
            }
        } catch (Exception ex) {
            Logger.getLogger(NMICalculation.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(query);
        }
        return probQuery;
    }

    /**
     *
     * @param param1
     * @param param2
     * @return
     */
    private int getDocFrequency(List<String> tokens) {
        return _searcher.search(tokens, Constants.DEFAULT_FIELD);
    }

    /**
     * Computing normalized mutual information between set of true queries and
     * cover queries.
     *
     * @param origQuery
     * @param coverQuery
     * @return
     */
    public double calculateNMI(ArrayList<String> origQuery, ArrayList<String> coverQuery) {
        double NMI = 0;
        HashMap<String, Double> Py = new HashMap<>();

        /* computing P(y) */
        for (String qr : coverQuery) {
            double prob = getProbability(qr);
            Py.put(qr, prob);
        }

        /* computing mutual information */
        double nominator = 0;
        double denominator = 0;
        for (String origQuery1 : origQuery) {
            /* computing P(x) */
            double px = getProbability(origQuery1);
            for (String coverQuery1 : coverQuery) {
                String combinedQuery = origQuery1 + " " + coverQuery1;
                /* computing P(x, y) */
                double pxy = getProbability(combinedQuery);
                double py = Py.get(coverQuery1);
                if (pxy > 0 && px > 0 && py > 0) {
                    nominator += pxy * Math.log(px * py);
                    denominator += pxy * Math.log(pxy);
                }
            }
        }

        /* normalized variant of mutual information */
        NMI = (nominator / denominator) - 1;
        return NMI;
    }
}
