/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.object;

import java.util.ArrayList;

/**
 *
 * @author wua4nw
 */
public class Task {

    private final int task_id;
    private ArrayList<UserQuery> user_queries;

    public Task(int id) {
        this.task_id = id;
        this.user_queries = new ArrayList<>();
    }

    public int getTask_id() {
        return task_id;
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

}
