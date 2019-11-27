package com.konfuse;

import com.konfuse.bean.Entry;
import com.konfuse.bean.MBR;
import com.konfuse.bean.NonLeafNode;
import com.konfuse.bean.LeafNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author: Konfuse
 * @Date: 2019/11/26 11:07
 */
public class RTree implements Serializable {
    private NonLeafNode root;

    public RTree(NonLeafNode root) {
        this.root = root;
    }

    public NonLeafNode getRoot() {
        return root;
    }

    public void setRoot(NonLeafNode root) {
        this.root = root;
    }

    public ArrayList<LeafNode> search(MBR area) {
        if (!MBR.intersects(this.root.getMBR(), area)) {
            return new ArrayList<LeafNode>();
        }
        Queue<Entry> queue = new LinkedBlockingQueue<>();
        NonLeafNode node;
        queue.add(this.root);
        ArrayList<LeafNode> result = new ArrayList<LeafNode> ();
        while (!queue.isEmpty()) {
            node = (NonLeafNode) queue.poll();
            ArrayList<Entry> entries = node.getEntries();
            for (Entry entry : entries) {
                if (MBR.intersects(entry.getMBR(), area)) {
                    if (entry instanceof LeafNode) {
                        result.add((LeafNode) entry);
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
