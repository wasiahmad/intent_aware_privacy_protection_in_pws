/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.object;

import edu.virginia.cs.user.Intent;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author wua4nw
 */
public class UserQuery {

    private int query_id;
    private String query_text;
    private Date query_time;
    private int query_length;
    private ArrayList<ResultDoc> relevant_documents;
    private ArrayList<UserQuery> cover_queries;
    private Intent query_intent;

    public UserQuery(int query_id, String query_text) {
        this.query_id = query_id;
        this.query_text = query_text;
        this.relevant_documents = new ArrayList<>();
        this.cover_queries = new ArrayList<>();
    }

    public int getQuery_id() {
        return query_id;
    }

    public void setQuery_id(int query_id) {
        this.query_id = query_id;
    }

    public String getQuery_text() {
        return query_text;
    }

    public void setQuery_text(String query_text) {
        this.query_text = query_text;
    }

    public Date getQuery_time() {
        return query_time;
    }

    public void setQuery_time(Date query_time) {
        this.query_time = query_time;
    }

    public int getQuery_length() {
        return query_length;
    }

    public void setQuery_length(int query_length) {
        this.query_length = query_length;
    }

    public ArrayList<ResultDoc> getRelevant_documents() {
        return relevant_documents;
    }

    public ResultDoc relDoc_Contains(ResultDoc rdoc) {
        for (ResultDoc clicked_doc : relevant_documents) {
            if (clicked_doc.getUrl().equals(rdoc.getUrl())) {
                return clicked_doc;
            }
        }
        return null;
    }

    public void setRelevant_documents(ArrayList<ResultDoc> clicked_documents) {
        this.relevant_documents = clicked_documents;
    }

    public void addRelevant_document(ResultDoc relDoc) {
        this.relevant_documents.add(relDoc);
    }

    public Intent getQuery_intent() {
        return query_intent;
    }

    public void setQuery_intent(Intent query_intent) {
        this.query_intent = query_intent;
    }

    public ArrayList<UserQuery> getCover_queries() {
        return cover_queries;
    }

    public void setCover_queries(ArrayList<UserQuery> cover_queries) {
        this.cover_queries = new ArrayList<>(cover_queries);
    }

    public void addCover_query(UserQuery cover_query) {
        this.cover_queries.add(cover_query);
    }

}
