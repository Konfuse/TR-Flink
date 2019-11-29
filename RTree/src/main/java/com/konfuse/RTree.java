package com.konfuse;

import com.konfuse.geometry.DataObject;
import com.konfuse.internal.TreeNode;
import com.konfuse.internal.MBR;
import com.konfuse.internal.NonLeafNode;
import com.konfuse.internal.LeafNode;

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
    private TreeNode root;
    private int height;

    public RTree() {
    }

    public RTree(TreeNode root) {
        this.root = root;
        this.height = root.getHeight();
    }

    public TreeNode getRoot() {
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

    public ArrayList<DataObject> rangeQuery(MBR area) {
        if (!MBR.intersects(this.root.getMBR(), area)) {
            return new ArrayList<>();
        }
        Queue<TreeNode> queue = new LinkedBlockingQueue<>();
        TreeNode node;
        queue.add(this.root);
        ArrayList<DataObject> result = new ArrayList<> ();
        while (!queue.isEmpty()) {
            node = queue.poll();
            if (node.getHeight() == 1) {
                ArrayList<DataObject> dataObjects = ((LeafNode<DataObject>) node).getEntries();
                for (DataObject dataObject : dataObjects) {
                    if (area.contains(dataObject))
                        result.add(dataObject);
                }
            } else {
                ArrayList<TreeNode> treeNodes = ((NonLeafNode) node).getChildNodes();
                for (TreeNode treeNode : treeNodes) {
                    if (MBR.intersects(treeNode.getMBR(), area)) {
                        queue.add(treeNode);
                    }
                }
            }
        }
        return result;
    }

    public ArrayList<MBR> getMBRs(int level) {
        assert level >= 1 && level <= root.getHeight();
        TreeNode node = this.root;
        Queue<TreeNode> queue = new LinkedList<>();
        ArrayList<MBR> result = new ArrayList<MBR>();

        // If root level, then return the MBR of the whole tree.
        if (node.getHeight() == level) {
            result.add(node.getMBR());
            return result;
        } else if (node.getHeight() - 1 == level) {
            NonLeafNode nonLeafNode = (NonLeafNode) node;
            for(TreeNode e : nonLeafNode.getChildNodes()) {
                result.add(e.getMBR());
            }
            return result;
        }

        queue.add(node);
        NonLeafNode nonLeafNode;
        while (!queue.isEmpty()) {
            nonLeafNode = (NonLeafNode) queue.poll();
            for (TreeNode child : nonLeafNode.getChildNodes()) {
                if (child.getHeight() == level + 1) {
                    for (TreeNode e : ((NonLeafNode) child).getChildNodes()) {
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
