/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.interfaces;

import java.util.HashMap;

/**
 *
 * @author wua4nw
 */
public interface Tree {

    public HashMap<String, TreeNode> getNodeMap();

    public void addNode(String nodePath, TreeNode node);

    public void setNodes(HashMap<String, TreeNode> nodeMap);
}
