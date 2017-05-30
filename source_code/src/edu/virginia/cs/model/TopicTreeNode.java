/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.model;

import edu.virginia.cs.interfaces.TreeNode;
import edu.virginia.cs.object.Topic;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author wua4nw
 */
public class TopicTreeNode implements TreeNode {

    private TreeNode parent;
    private ArrayList<TreeNode> listOfChildren;
    private Topic topic;

    private int topic_id;
    private String topic_name;
    private int level;
    private double topicPrior;

    public TopicTreeNode(String name, int id) {
        this.topic_name = name;
        this.topic_id = id;
        this.topic = new Topic();
        this.listOfChildren = new ArrayList<>();
        topicPrior = -1;
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    @Override
    public List<TreeNode> getChildrens() {
        return listOfChildren;
    }

    public List<TreeNode> getSiblings() {
        ArrayList<TreeNode> siblings = new ArrayList<>();
        for (TreeNode node : parent.getChildrens()) {
            if (((TopicTreeNode) node).getTopic_id() != this.topic_id) {
                siblings.add(node);
            }
        }
        return siblings;
    }

    @Override
    public void setChildrens(List<TreeNode> childrens) {
        this.listOfChildren = new ArrayList<>(childrens);
    }

    @Override
    public void addChildren(TreeNode children) {
        this.listOfChildren.add(children);
    }

    @Override
    public void setNodeLevel(int level) {
        this.level = level;
    }

    @Override
    public int getNodeLevel() {
        return level;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public int getTopic_id() {
        return topic_id;
    }

    public void setTopic_id(int topic_id) {
        this.topic_id = topic_id;
    }

    public String getTopic_name() {
        return topic_name;
    }

    public void setTopic_name(String topic_name) {
        this.topic_name = topic_name;
    }

    public double getTopicPrior(int totalNodes) {
        if (topicPrior == -1) {
            computeTopicPrior(totalNodes);
        }
        return topicPrior;
    }

    public void setTopicPrior(double topicPrior) {
        this.topicPrior = topicPrior;
    }

    /**
     * Computes topic prior based on the number of nodes in the subtree rooted
     * at topic t divided by total number of nodes in the topic hierarchy.
     *
     * @param totalNodes
     */
    private void computeTopicPrior(int totalNodes) {
        if (topicPrior == -1) {
            double totalNodesInSubTree = 1;
            LinkedList<TreeNode> list = new LinkedList<>();
            list.addAll(listOfChildren);
            while (!list.isEmpty()) {
                TreeNode tNode = list.poll();
                list.addAll(tNode.getChildrens());
                totalNodesInSubTree++;
            }
            topicPrior = totalNodesInSubTree / totalNodes;
        }
    }

}
