package com.konfuse.dita;

import com.konfuse.strtree.MBR;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @Author todd
 * @Date 2020/4/17
 */
public class TrieInternalNode extends TrieNode implements Serializable {
    private ArrayList<TrieNode> childes;

    public TrieInternalNode(int level, TrieNodeType type, MBR mbr, int currentCapacity) {
        super(level, type, mbr, currentCapacity);
        this.childes = new ArrayList<>(DITAConfig.localMinNodeSize);
    }

    public ArrayList<TrieNode> getChildes() {
        return childes;
    }

    public void setChildes(ArrayList<TrieNode> childes) {
        this.childes = childes;
    }

    public void addChild(TrieNode child) {
        this.childes.add(child);
    }

}
