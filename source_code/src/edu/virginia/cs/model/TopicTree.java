/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.model;

import edu.virginia.cs.interfaces.Tree;
import edu.virginia.cs.interfaces.TreeNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author wua4nw
 */
public class TopicTree implements Tree {

    private HashMap<String, TreeNode> nodeMap;
    private final HashMap<Integer, List<TreeNode>> levelWiseNodeMap;

    public TopicTree() {
        this.nodeMap = new HashMap<>();
        this.levelWiseNodeMap = new HashMap<>();
    }

    public TopicTree(TopicTree tree) {
        this.nodeMap = new HashMap<>();
        this.nodeMap.putAll(tree.getNodeMap());
        this.levelWiseNodeMap = new HashMap<>();
        this.levelWiseNodeMap.putAll(tree.getLevelWiseNodeMap());
    }

    @Override
    public void setNodes(HashMap<String, TreeNode> nodeMap) {
        this.nodeMap = nodeMap;
    }

    public TreeNode getTreeNode(String name) {
        return this.nodeMap.get(name);
    }

    @Override
    public HashMap<String, TreeNode> getNodeMap() {
        return this.nodeMap;
    }

    public boolean exists(String node) {
        return nodeMap.containsKey(node);
    }

    @Override
    public void addNode(String nodePath, TreeNode node) {
        this.nodeMap.put(nodePath, node);
        if (levelWiseNodeMap.containsKey(node.getNodeLevel())) {
            levelWiseNodeMap.get(node.getNodeLevel()).add(node);
        } else {
            List<TreeNode> nodeList = new ArrayList<>();
            nodeList.add(node);
            levelWiseNodeMap.put(node.getNodeLevel(), nodeList);
        }
    }

    public List<TreeNode> getNodesOfLevel(int level) {
        return levelWiseNodeMap.get(level);
    }

    public HashMap<Integer, List<TreeNode>> getLevelWiseNodeMap() {
        return this.levelWiseNodeMap;
    }

}
