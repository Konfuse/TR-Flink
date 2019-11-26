package com.konfuse;

import com.konfuse.bean.Entry;
import com.konfuse.bean.MBR;
import com.konfuse.bean.RTreeNode;
import com.konfuse.bean.Record;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author: Konfuse
 * @Date: 2019/11/26 11:07
 */
public class RTree implements Serializable {
    private RTreeNode root;

    public RTree(RTreeNode root) {
        this.root = root;
    }

    public RTreeNode getRoot() {
        return root;
    }

    public void setRoot(RTreeNode root) {
        this.root = root;
    }

    public ArrayList<Record> search(MBR area) {
        if (!MBR.intersects(this.root.getMbr(), area)) {
            return new ArrayList<Record>();
        }
        Queue<Entry> queue = new LinkedBlockingQueue<>();
        RTreeNode node;
        queue.add(this.root);
        ArrayList<Record> result = new ArrayList<Record> ();
        while (!queue.isEmpty()) {
            node = (RTreeNode) queue.poll();
            ArrayList<Entry> entries = node.getEntries();
            for (Entry entry : entries) {
                if (MBR.intersects(entry.getMbr(), area)) {
                    if (entry instanceof Record) {
                        result.add((Record) entry);
                    } else {
                        queue.add(entry);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "RTree{" +
                "root=" + root +
                '}';
    }
}
