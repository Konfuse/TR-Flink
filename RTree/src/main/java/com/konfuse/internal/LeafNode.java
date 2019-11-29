package com.konfuse.internal;

import com.konfuse.geometry.DataObject;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @Author: Konfuse
 * @Date: 2019/11/26 11:01
 */
public class LeafNode<T extends DataObject> extends TreeNode implements Serializable {
    private ArrayList<T> entries;
    public LeafNode(int M) {
        super(new MBR(), 1);
        entries = new ArrayList<>(M);
    }

    public LeafNode(ArrayList<T> entries, MBR mbr) {
        super(mbr, 1);
        this.entries = entries;
    }

    public ArrayList<T> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<T> entries) {
        this.entries = entries;
    }

    @Override
    public String toString() {
        return "LeafNode{" +
                "dataObjects=" + entries +
                ", height=" + height +
                ", mbr=" + mbr +
                '}';
    }
}
