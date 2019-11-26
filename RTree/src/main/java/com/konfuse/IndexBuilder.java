package com.konfuse;

import com.konfuse.bean.Entry;
import com.konfuse.bean.MBR;
import com.konfuse.bean.RTreeNode;
import com.konfuse.bean.Record;

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
    private RTreeNode root;

    public IndexBuilder(int M, int m) {
        this.M = M;
        this.m = m;
        this.entryCount = 0;
        this.height = 1;
        root = new RTreeNode(M, 1);
    }

    /*
    * r is the total count of records, i.e. entries.size()
    * M is the maximum capacity of each partition
    * p is the total count of partitions, i.e. p = r / M
    * s is the due slice count of each dimension, i.e. s = Math.sqrt(r / M)
    * ctr is records traveling count
    * */
    public RTree STRPacking(Record... records){
        ArrayList<Entry> entries = new ArrayList<Entry>(Arrays.stream(records).collect(Collectors.toList()));
        this.entryCount = entries.size();
        double p = entries.size() * 1.0 / M;
        entries.sort(new MBR.MBRComparator(0, true));
        entries = STRRecursive(2, p,  entries, 1);
        root = new RTreeNode(M, this.height);
        root.entries = entries;
        root.mbr = MBR.union(root.getMBRs());
        return new RTree(root);
    }

    public ArrayList<Entry> STRRecursive(int dim, double p, ArrayList<Entry> entries, int height) {
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
                STRPackNodes(dim - 1, height, list, nextLevel);
                list.clear();
                ctr = 0;
            }
        }
        if(list.size() > 0) {
            STRPackNodes(dim - 1, height, list, nextLevel);
            list.clear();
        }
        return STRRecursive(dim, nextLevel.size() * 1.0 / M, nextLevel, height + 1);
    }

    public void STRPackNodes(int dim, int height, ArrayList<Entry> list, ArrayList<Entry> nextLevel) {
        list.sort(new MBR.MBRComparator(dim, true));
        RTreeNode rTreeNode = new RTreeNode(M, height);
        for (Entry entrySlide : list) {
            rTreeNode.entries.add(entrySlide);
            if (rTreeNode.entries.size() == M) {
                rTreeNode.mbr = MBR.union(rTreeNode.getMBRs());
                nextLevel.add(rTreeNode);
                rTreeNode = new RTreeNode(M, height);
            }
        }
        if (rTreeNode.entries.size() > 0) {
            while (rTreeNode.entries.size() < m) {
                RTreeNode swapped = (RTreeNode)nextLevel.get(nextLevel.size() - 1);
                rTreeNode.entries.add(0,swapped.entries.remove(swapped.entries.size() - 1));
            }
            rTreeNode.mbr = MBR.union(rTreeNode.getMBRs());
            nextLevel.add(rTreeNode);
        }
    }
}
