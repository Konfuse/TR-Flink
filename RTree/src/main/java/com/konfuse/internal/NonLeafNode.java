package com.konfuse.internal;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The non-leaf node that is the internal node in the r-tree extends TreeNode.
 * It has an attribute referring to the list of internal tree nodes in the r-tree,
 * i.e.  childNodes.
 * @Author: Konfuse
 * @Date: 2019/11/26 11:10
 */
public class NonLeafNode extends TreeNode implements Serializable {
    private ArrayList<TreeNode> childNodes;

    public NonLeafNode(int M, int height) {
        super(new MBR(), height);
        this.height = height;
        this.childNodes = new ArrayList<TreeNode>(M);
    }

    public ArrayList<TreeNode> getChildNodes() {
        return childNodes;
    }

    public void setChildNodes(ArrayList<TreeNode> childNodes) {
        this.childNodes = childNodes;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public MBR[] getMBRs(){
        MBR[] mbrs = new MBR[childNodes.size()];
        int i = 0;
        for(TreeNode treeNode : childNodes){
            mbrs[i] = treeNode.mbr;
            i++;
        }
        return mbrs;
    }
}
