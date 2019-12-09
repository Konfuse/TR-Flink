package com.konfuse.internal;

import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.MBR;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The leaf node of r-tree containing a list of data objects extends TreeNode.
 * It has an attribute referring to the list of data records in the r-tree,
 * i.e.  entries.
 *
 * @Author: Konfuse
 * @Date: 2019/11/26 11:01
 */
public class LeafNode<T extends DataObject> extends TreeNode implements Serializable {
    private ArrayList<T> entries;
    public LeafNode() {
        super(new MBR(), 1);
        entries = new ArrayList<>();
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
                ", unionPoints=" + mbr +
                '}';
    }
}
