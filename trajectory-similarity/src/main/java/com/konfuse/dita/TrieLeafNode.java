package com.konfuse.dita;

import com.konfuse.strtree.MBR;

import java.util.LinkedList;
import java.util.List;

/**
 * @Author todd
 * @Date 2020/4/17
 */
public class TrieLeafNode extends TrieNode {
    private int[] data;

    public TrieLeafNode(int level, TrieNodeType type, MBR mbr, int currentCapacity, List<Integer> data) {
        super(level, type, mbr, currentCapacity);
        this.data = new int[currentCapacity];
        int count = 0;
        for (Integer datum : data) {
            this.data[count] = datum;
            count++;
        }
    }

    public int[] getData() {
        return data;
    }

    public void setData(int[] data) {
        this.data = data;
    }

    public void setData(int index, int trajectorySeqNum) {
        this.data[index] = trajectorySeqNum;
    }
}
