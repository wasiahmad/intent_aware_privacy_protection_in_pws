/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.interfaces;

import java.util.List;

/**
 *
 * @author wua4nw
 */
public interface TreeNode {

    public TreeNode getParent();

    public void setParent(TreeNode node);

    public List<TreeNode> getChildrens();

    public void setChildrens(List<TreeNode> childrens);

    public void addChildren(TreeNode children);

    public void setNodeLevel(int level);

    public int getNodeLevel();
}
