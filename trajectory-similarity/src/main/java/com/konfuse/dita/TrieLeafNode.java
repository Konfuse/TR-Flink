package com.konfuse.dita;

import com.konfuse.strtree.MBR;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author todd
 * @Date 2020/4/17
 */
public class TrieLeafNode extends TrieNode implements Serializable {
    private List<DITATrajectory> data;

    public TrieLeafNode(int level, TrieNodeType type, MBR mbr, int currentCapacity, List<DITATrajectory> data) {
        super(level, type, mbr, currentCapacity);
        this.data = data;

    }

    public List<DITATrajectory> getData() {
        return data;
    }

    public void setData(ArrayList<DITATrajectory> data) {
        super.setCurrentCapacity(data.size());
        this.data = data;
    }

    public void setData(int index, DITATrajectory trajectorySeqNum) {
        this.data.set(index, trajectorySeqNum);
    }
}
