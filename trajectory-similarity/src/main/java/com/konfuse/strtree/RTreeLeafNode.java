package com.konfuse.strtree;

import com.konfuse.geometry.DataObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The leaf node of r-tree containing a list of data objects extends TreeNode.
 * It has an attribute referring to the list of data records in the r-tree,
 * i.e.  entries.
 *
 * @Author: Konfuse
 * @Date: 2019/11/26 11:01
 */
public abstract class RTreeLeafNode<T extends DataObject> extends RTreeNode implements Serializable {
    private List<T> entries;

    public RTreeLeafNode(RTreeNodeType type) {
        super(new MBR(), 1, 0,true, type);
        this.entries = new ArrayList<>();
    }

    public RTreeLeafNode(List<T> childNodes, MBR mbr, RTreeNodeType type) {
        super(mbr, 1, childNodes.size(), true, type);
        this.entries = childNodes;
    }

    public RTreeLeafNode(List<T> childNodes, MBR mbr, int height, RTreeNodeType type) {
        super(mbr, height, childNodes.size(), true, type);
        this.entries = childNodes;
    }

    public RTreeLeafNode(int capacity, RTreeNodeType type) {
        super(new MBR(), 1, true, type);
        this.entries = new ArrayList<>(capacity);
    }

    public List<T> getEntries() {
        return entries;
    }

    public void setEntries(List<T> entries) {
        this.entries = entries;
        this.setSize(entries.size());
    }

    public void setEntries(List<T> entries, MBR mbr) {
        this.entries = entries;
        this.setSize(entries.size());
        this.mbr = mbr;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("");
        for(int i =0; i<this.entries.size(); i++){
            T entry = this.entries.get(i);
            str.append(entry.toString());
        }
        return str.toString();
    }
}
