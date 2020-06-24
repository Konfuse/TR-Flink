package com.konfuse.dita;

import com.konfuse.strtree.MBR;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author todd
 * @Date 2020/4/17
 */
public class TrieInternalNode extends TrieNode implements Serializable {
    private List<TrieNode> childes;

    public TrieInternalNode(int level, TrieNodeType type, MBR mbr, int currentCapacity) {
        super(level, type, mbr, currentCapacity);
        this.childes = new ArrayList<>(DITAConfig.localMinNodeSize);
    }

    public TrieInternalNode(int level, TrieNodeType type, MBR mbr, int currentCapacity, List<TrieNode> childes) {
        super(level, type, mbr, currentCapacity);
        this.childes = childes;
    }

    public List<TrieNode> getChildes() {
        return childes;
    }

    public void setChildes(List<TrieNode> childes) {
        this.childes = childes;
    }

    public void addChild(TrieNode child) {
        this.childes.add(child);
    }

}
