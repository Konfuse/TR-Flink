package com.konfuse.strtree;

import java.io.Serializable;

import static com.konfuse.strtree.RTreeNodeType.STR_TREE_ROOT_NODE;

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
public class RTreeNode implements Serializable {
    public MBR mbr;
    private boolean isLeaf;
    private int size;
    private int height;
    private RTreeNodeType type;

    public RTreeNode() {
        this.isLeaf = false;
        this.size = 0;
        this.type = STR_TREE_ROOT_NODE;
    }

    public RTreeNode(MBR mbr, int height, boolean isLeaf, RTreeNodeType type) {
        this.mbr = mbr;
        this.isLeaf = isLeaf;
        this.height = height;
        this.size = 0;
        this.type = type;
    }

    public RTreeNode(MBR mbr, int height, int size, boolean isLeaf, RTreeNodeType type) {
        this.mbr = mbr;
        this.isLeaf = isLeaf;
        this.size = size;
        this.height = height;
        this.type = type;
    }

    public MBR getMBR() {
        return mbr;
    }

    public void setMBR(MBR mbr) {
        this.mbr = mbr;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public MBR getMbr() {
        return mbr;
    }

    public void setMbr(MBR mbr) {
        this.mbr = mbr;
    }

    public RTreeNodeType getType() {
        return type;
    }

    public void setType(RTreeNodeType type) {
        this.type = type;
    }

    public int getTypeNum() {
        switch (getType()) {
            case STR_TREE_ROOT_NODE: {
                return (1);
            }
            case STR_TREE_POINT_LEAF_NODE: {
                return (3);
            }
            case STR_TREE_LINE_LEAF_NODE: {
                return (4);
            }
            case STR_TREE_RECTANGLE_LEAF_NODE: {
                return (5);
            }
            default: {
                return (2);
            }
        }
    }

    @Override
    public String toString() {
        return "RTreeNode{" +
                "mbr=" + mbr +
                ", isLeaf=" + isLeaf +
                ", size=" + size +
                ", height=" + height +
                ", type=" + type +
                '}';
    }
}
