/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.user;

import edu.virginia.cs.config.StaticData;
import edu.virginia.cs.interfaces.TreeNode;
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
public class Intent implements TreeNode {

    private TreeNode parent;
    private List<TreeNode> childrens;
    private int level;
    private final History history;

    private final TextTokenizer _tokenizer;

    private double totalQueryLength;
    private double totalQuery;
    private int totalTokens;

    private final String intentName;

    public Intent(String name) {
        history = new History();
        this.childrens = new ArrayList<>();

        this._tokenizer = new TextTokenizer(true, true);
        this.intentName = name;

        totalTokens = 0;
        totalQuery = 0;
        totalQueryLength = 0;
    }

    public String getName() {
        return this.intentName;
    }

    @Override
    public TreeNode getParent() {
        return this.parent;
    }

    @Override
    public void setParent(TreeNode node) {
        this.parent = node;
    }

    public boolean isParent(TreeNode node) {
        Intent intent = (Intent) node.getParent();
        if (intent == null) {
            return false;
        }
        return this.intentName.equals(((Intent) node.getParent()).getName());
    }

    @Override
    public List<TreeNode> getChildrens() {
        return this.childrens;
    }

    @Override
    public void setChildrens(List<TreeNode> childrens) {
        this.childrens = new ArrayList<>(childrens);
    }

    @Override
    public void addChildren(TreeNode children) {
        if (this.childrens != null) {
            this.childrens.add(children);
        } else {
            throw new NullPointerException("children is not initialized");
        }
    }

    @Override
    public void setNodeLevel(int level) {
        this.level = level;
    }

    @Override
    public int getNodeLevel() {
        return level;
    }

    public int getTotalTokenCount() {
        return totalTokens;
    }

    public History getHistory() {
        return history;
    }

    /**
     * Returns average length of all queries submitted by the user. Default
     * value is 3.
     *
     * @return average query length
     */
    public double getAvgQueryLength() {
        if (totalQuery == 0) {
            return 3;
        }
        return totalQueryLength / totalQuery;
    }

    /**
     * Update user profile by the user submitted query.
     *
     * @param queryText
     */
    public void updateUsingSubmittedQuery(String queryText) {
        List<String> qParts = _tokenizer.TokenizeText(queryText);
        totalQuery++;
        totalQueryLength = totalQueryLength + qParts.size();
        for (String qPart : qParts) {
            if (qPart.isEmpty()) {
                continue;
            }
            totalTokens++;
            history.addElementInQueryTerms(qPart, 1);
        }
    }

    public HashMap<String, Integer> getRecord(String text, boolean fromDoc) {
        List<String> tokens = _tokenizer.TokenizeText(text);
        HashMap<String, Integer> record = new HashMap<>();
        if (fromDoc) {
            /* To update user profile, select the top k tokens using tf-idf weight */
            int k = (int) (tokens.size() * 0.25);
            HashMap<String, Integer> retVal = selectTopKtokens(tokens, k);
            for (Map.Entry<String, Integer> entry : retVal.entrySet()) {
                if (record.containsKey(entry.getKey())) {
                    record.put(entry.getKey(), record.get(entry.getKey()) + entry.getValue());
                } else {
                    record.put(entry.getKey(), entry.getValue());
//                    record.put(entry.getKey(), 1);
                }
            }
        } else {
            for (String token : tokens) {
                if (token.isEmpty()) {
                    continue;
                }
                if (record.containsKey(token)) {
                    record.put(token, record.get(token) + 1);
                } else {
                    record.put(token, 1);
                }
            }
        }
        return record;
    }

    /**
     * Update user profile by the content of the user clicked document.
     *
     * @param content
     */
    public void updateUsingClickedDoc(String content) {
        List<String> tokens = _tokenizer.TokenizeText(content);
        /* To update user profile, select the top k tokens using tf-idf weight */
        int k = (int) (tokens.size() * 0.25);
        HashMap<String, Integer> retVal = selectTopKtokens(tokens, k);
        for (Map.Entry<String, Integer> entry : retVal.entrySet()) {
            // totalTokens += entry.getValue();
            // history.addElementInDocTerms(entry.getKey(), entry.getValue());
            history.addElementInDocTerms(entry.getKey(), 1);
            totalTokens++;
        }
    }

    /**
     * Method that returns the top k tokens from a list of tokens. Tokens are
     * ranked based on their tf-idf value.
     *
     * @param tokenList list of tokens
     * @param k return only the top k elements
     * @return top k tokens with their term frequency
     */
    private HashMap<String, Integer> selectTopKtokens(List<String> tokenList, int k) {
        HashMap<String, Integer> retValue = new HashMap<>();
        HashMap<String, Integer> tempMap = new HashMap<>();
        /* Stores tf-idf weight of all tokens */
        HashMap<String, Float> unsortedMap = new HashMap<>();
        for (String token : tokenList) {
            Integer n = tempMap.get(token);
            n = (n == null) ? 1 : ++n;
            tempMap.put(token, n);
        }
        for (Map.Entry<String, Integer> entry : tempMap.entrySet()) {
            Double idf = StaticData.IDFRecord.get(entry.getKey());
            if (idf == null) {
                idf = 0.0;
            }
            double tfIdfWeight = entry.getValue() * idf;
            unsortedMap.put(entry.getKey(), (float) tfIdfWeight);
        }
        Map<String, Float> temp = SortMap.sortMapByValue(unsortedMap, false, k);
        for (Map.Entry<String, Float> entry : temp.entrySet()) {
            retValue.put(entry.getKey(), tempMap.get(entry.getKey()));
        }
        return retValue;
    }

}
