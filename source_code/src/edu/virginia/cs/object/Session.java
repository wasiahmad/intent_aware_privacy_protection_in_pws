/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.object;

import edu.virginia.cs.extra.Helper;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author wua4nw
 */
public class Session {

    private final int session_id;
    private Date start_time;
    private Date end_time;
    private ArrayList<UserQuery> user_queries;

    public Session(int id) {
        this.session_id = id;
        this.user_queries = new ArrayList<>();
    }

    public int getSession_id() {
        return session_id;
    }

    public Date getStart_time() {
        return start_time;
    }

    public void setStart_time(Date start_time) {
        this.start_time = start_time;
    }

    public Date getEnd_time() {
        return end_time;
    }

    public void setEnd_time(Date end_time) {
        this.end_time = end_time;
    }

    public void addUser_queries(UserQuery user_query) {
        this.user_queries.add(user_query);
    }

    public ArrayList<UserQuery> getUser_queries() {
        return user_queries;
    }

    public void setUser_queries(ArrayList<UserQuery> user_queries) {
        this.user_queries = user_queries;
    }

    public UserQuery checkRepeat(UserQuery query) {
        for (int i = user_queries.size() - 1; i >= 0; i--) {
            if (user_queries.get(i).getQuery_text().equals(query.getQuery_text())) {
                return user_queries.get(i);
            }
        }
        return null;
    }

    public boolean belongsTo(UserQuery query) {
        UserQuery lastQuery = user_queries.get(user_queries.size() - 1);
        return Helper.checkSameSession(lastQuery, query);
    }

}
