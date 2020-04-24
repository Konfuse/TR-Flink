package com.konfuse.strtree;

import java.io.Serializable;

/**
 * A basic node in r-tree
 * Leaf Node and NonLeaf Node that make up r-tree should extends this class
 * It contains two basic attributes:
 * height: refers to the height of the current node. If leaf nodes, the height is 1.
 * mbr: refers to the mbr of all the data objects in the current node
 *
 * @Author: Konfuse
 * @Date: 2019/11/26 10:49
 */
public abstract class TreeNode implements Serializable {
    int height;
    MBR mbr;

    TreeNode(MBR mbr, int height) {
        this.mbr = mbr;
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public MBR getMBR() {
        return mbr;
    }

    public void setMBR(MBR mbr) {
        this.mbr = mbr;
    }
}
