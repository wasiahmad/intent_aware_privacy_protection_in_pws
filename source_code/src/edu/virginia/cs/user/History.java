/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.user;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author wua4nw
 */
public class History {

    private final HashMap<String, Integer> selectedQueryTerms;
    private final HashMap<String, Integer> selectedDocTerms;

    public History() {
        this.selectedQueryTerms = new HashMap<>();
        this.selectedDocTerms = new HashMap<>();
    }

    public HashMap<String, Integer> getSelectedQueryTerms() {
        return selectedQueryTerms;
    }

    public HashMap<String, Integer> getSelectedDocTerms() {
        return selectedDocTerms;
    }

    public void addElementInQueryTerms(String key, int value) {
        if (selectedQueryTerms == null) {
            throw new NullPointerException("Query term map of history is not initialized");
        } else if (selectedQueryTerms.containsKey(key)) {
            selectedQueryTerms.put(key, selectedQueryTerms.get(key) + value);
        } else {
            selectedQueryTerms.put(key, value);
        }
    }

    public void addElementInDocTerms(String key, int value) {
        if (selectedDocTerms == null) {
            throw new NullPointerException("Document term map of history is not initialized");
        } else if (selectedDocTerms.containsKey(key)) {
            selectedDocTerms.put(key, selectedDocTerms.get(key) + value);
        } else {
            selectedDocTerms.put(key, value);
        }
    }

    public HashMap<String, Integer> getCompleteHistory() {
        HashMap<String, Integer> completeHistory = new HashMap<>();
        for (Map.Entry<String, Integer> entry : selectedQueryTerms.entrySet()) {
            if (completeHistory.containsKey(entry.getKey())) {
                completeHistory.put(entry.getKey(), completeHistory.get(entry.getKey()) + entry.getValue());
            } else {
                completeHistory.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, Integer> entry : selectedDocTerms.entrySet()) {
            if (completeHistory.containsKey(entry.getKey())) {
                completeHistory.put(entry.getKey(), completeHistory.get(entry.getKey()) + entry.getValue());
            } else {
                completeHistory.put(entry.getKey(), entry.getValue());
            }
        }
        return completeHistory;
    }

}
