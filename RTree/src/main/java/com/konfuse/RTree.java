package com.konfuse;

import com.konfuse.bean.Entry;
import com.konfuse.bean.MBR;
import com.konfuse.bean.NonLeafNode;
import com.konfuse.bean.LeafNode;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author: Konfuse
 * @Date: 2019/11/26 11:07
 */
public class RTree implements Serializable {
    private NonLeafNode root;
    private int height;

    public RTree() {
    }

    public RTree(NonLeafNode root) {
        this.root = root;
        this.height = root.getHeight();
    }

    public NonLeafNode getRoot() {
        return root;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
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

    public ArrayList<MBR> getMBRs(int level) {
        NonLeafNode node = this.root;
        Queue<Entry> queue = new LinkedList<>();
        ArrayList<MBR> result = new ArrayList<MBR>();

        // If root level, then return the MBR of the whole tree.
        if (node.getHeight() == level) {
            result.add(node.getMBR());
            return result;
        } else if (node.getHeight() - 1 == level) {
            for(Entry e : node.getEntries()) {
                result.add(e.getMBR());
            }
            return result;
        }

        queue.add(node);
        while (!queue.isEmpty()) {
            node = (NonLeafNode) queue.poll();
            for (Entry child : node.getEntries()) {
                if (((NonLeafNode)child).getHeight() == level + 1) {
                    for (Entry e : ((NonLeafNode) child).getEntries()) {
                        result.add(e.getMBR());
                    }
                } else if (level + 1 < ((NonLeafNode) child).getHeight()) {
                    queue.add(child);
                }
            }
        }
        return result;
    }

    public void save(String file) throws IOException {
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
        outputStream.writeObject(this);
        outputStream.close();
    }

    public static RTree loadRTree(String file) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
        RTree tree = (RTree) inputStream.readObject();
        inputStream.close();
        return tree;
    }

    @Override
    public String toString() {
        return "RTree{" +
                "root=" + root +
                ", height=" + height +
                '}';
    }
}
