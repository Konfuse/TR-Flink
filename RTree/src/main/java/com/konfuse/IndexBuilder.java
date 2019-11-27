package com.konfuse;

import com.konfuse.bean.Entry;
import com.konfuse.bean.MBR;
import com.konfuse.bean.NonLeafNode;
import com.konfuse.bean.LeafNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @Author: Konfuse
 * @Date: 2019/11/26 11:27
 */
public class IndexBuilder implements Serializable {
    private int M = 40;
    private int m = 16;
    private int entryCount;
    private int height;
    private NonLeafNode root;

    public IndexBuilder() {
    }

    public IndexBuilder(int M, int m) {
        this.M = M;
        this.m = m;
        this.entryCount = 0;
        this.height = 1;
        this.root = new NonLeafNode(M, 1);
    }

    /*
    * r is the total count of records, i.e. entries.size()
    * M is the maximum capacity of each partition
    * p is the total count of partitions, i.e. p = r / M
    * s is the due slice count of each dimension, i.e. s = Math.sqrt(r / M)
    * ctr is records traveling count
    * */
    public RTree STRPacking(LeafNode... leafNodes){
        ArrayList<Entry> entries = new ArrayList<Entry>(Arrays.stream(leafNodes).collect(Collectors.toList()));
        this.entryCount = entries.size();

        // calculate partition num
        double p = entries.size() * 1.0 / M;

        // start build r-tree structure bottom-to-up recursively
        entries.sort(new MBR.MBRComparator(1, true));
        entries = STRRecursive(p,  entries, 1);

        this.root = new NonLeafNode(M, this.height);
        this.root.setEntries(entries);
        this.root.setMBR(MBR.union(root.getMBRs()));
        return new RTree(root);
    }

    public ArrayList<Entry> STRRecursive(double p, ArrayList<Entry> entries, int height) {
        if (entries.size() <= M) {
            this.height = height;
            return entries;
        }
        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(p) / 2));
        ArrayList<Entry> list = new ArrayList<Entry>();
        ArrayList<Entry> nextLevel = new ArrayList<Entry>();
        int ctr = 0;
        for (Entry entry : entries) {
            list.add(entry);
            ++ctr;
            if (ctr == s * M) {
                //get a slide in list, and divide it into nodes
                STRPackNodes(height, list, nextLevel);
                list.clear();
                ctr = 0;
            }
        }
        if(list.size() > 0) {
            STRPackNodes(height, list, nextLevel);
            list.clear();
        }
        return STRRecursive(nextLevel.size() * 1.0 / M, nextLevel, height + 1);
    }

    public void STRPackNodes(int height, ArrayList<Entry> list, ArrayList<Entry> nextLevel) {
        list.sort(new MBR.MBRComparator(2, true));
        NonLeafNode nonLeafNode = new NonLeafNode(M, height);
        for (Entry entrySlide : list) {
            nonLeafNode.getEntries().add(entrySlide);
            if (nonLeafNode.getEntries().size() == M) {
                nonLeafNode.setMBR(MBR.union(nonLeafNode.getMBRs()));
                nextLevel.add(nonLeafNode);
                nonLeafNode = new NonLeafNode(M, height);
            }
        }
        if (nonLeafNode.getEntries().size() > 0) {
            if (nonLeafNode.getEntries().size() < m) {
                NonLeafNode swapped = (NonLeafNode) nextLevel.get(nextLevel.size() - 1);
                ArrayList<Entry> lastNodeEntries = nonLeafNode.getEntries();
                ArrayList<Entry> swappedEntries = swapped.getEntries();
                while (nonLeafNode.getEntries().size() < m) {
                    lastNodeEntries.add(0, swappedEntries.remove(swappedEntries.size() - 1));
                }
            }
            nonLeafNode.setMBR(MBR.union(nonLeafNode.getMBRs()));
            nextLevel.add(nonLeafNode);
        }
    }
}
