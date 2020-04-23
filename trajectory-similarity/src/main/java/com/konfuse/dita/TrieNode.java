package com.konfuse.dita;

import com.konfuse.strtree.MBR;

import java.util.InvalidPropertiesFormatException;

/**
 * @Auther todd
 * @Date 2020/4/17
 */
public class TrieNode {
    private int level;
    private int currentCapacity;
    private TrieNodeType type;
    private MBR mbr;

    public TrieNode(int level, TrieNodeType type, MBR mbr) {
        this.level = level;
        this.currentCapacity = 0;
        this.type = type;
        this.mbr = mbr;
    }

    public TrieNode(int level, TrieNodeType type, MBR mbr, int currentCapacity) {
        this.level = level;
        this.currentCapacity = currentCapacity;
        this.type = type;
        this.mbr = mbr;
    }

    public int getLevel() {
        return level;
    }

    public TrieNodeType getType() {
        return type;
    }

    public MBR getMbr() {
        return mbr;
    }

    public int getCurrentCapacity() {
        return currentCapacity;
    }

    public void setMbr(MBR mbr) {
        this.mbr = mbr;
    }

    public void setCurrentCapacity(int currentCapacity) {
        this.currentCapacity = currentCapacity;
    }


    public boolean isLeaf() {
        return type == TrieNodeType.TRIE_LEAF_NODE;
    }

    public boolean isInternalNode() {
        return type == TrieNodeType.TRIE_INTERNAL_NODE;
    }


    short getTypeNum() throws InvalidPropertiesFormatException {
        switch (getType()) {
            case TRIE_LEAF_NODE: {
                return (1);
            }
            case TRIE_INTERNAL_NODE: {
                return (2);
            }
            case TRIE_ROOT_NODE: {
                return (3);
            }
            default: {
                throw new InvalidPropertiesFormatException("Unknown " +
                        "node value read; file possibly corrupt?");
            }
        }
    }

    @Override
    public String toString() {
        return "TrieNode{" +
                "level=" + level +
                ", type=" + type +
                ", mbr=" + mbr +
                '}';
    }
}
