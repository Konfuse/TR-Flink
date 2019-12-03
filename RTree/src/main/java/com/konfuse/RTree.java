package com.konfuse;

import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.Point;
import com.konfuse.internal.TreeNode;
import com.konfuse.internal.MBR;
import com.konfuse.internal.NonLeafNode;
import com.konfuse.internal.LeafNode;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author: Konfuse
 * @Date: 2019/11/26 11:07
 */
public class RTree implements Serializable {
    private TreeNode root;
    private int height;
    private int maxNodeNb;
    private int minNodeNb;

    public RTree() {
    }

    public RTree(TreeNode root, int M, int m) {
        this.root = root;
        this.height = root.getHeight();
        this.maxNodeNb = M;
        this.minNodeNb = m;
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

    public int getMaxNodeNb() {
        return maxNodeNb;
    }

    public int getMinNodeNb() {
        return minNodeNb;
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

    public ArrayList<DataObject> circleRangeQuery(final Point queryPoint, double radius) {
        ArrayList<DataObject> result = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(this.root);
        while (!queue.isEmpty()) {
            TreeNode curNode = queue.poll();
            if(curNode.getMBR().intersects(queryPoint, radius)) {
                if(curNode.getHeight() == 1) {
                    ArrayList<DataObject> entries = ((LeafNode<DataObject>) curNode).getEntries();
                    for (DataObject dataObject: entries) {
                        if(dataObject.calDistance(queryPoint) <= radius * radius){
                            result.add(dataObject);
                        }
                    }
                }
                else {
                    ArrayList<TreeNode> childNodes = ((NonLeafNode) curNode).getChildNodes();
                    queue.addAll(childNodes);
                }
            }
        }
        return result;
    }

    public ArrayList<DataObject> knnQuery(final Point queryPoint, int k) {
        ArrayList<Double> distances = this.knnDistance(queryPoint, k);
        double refined_bound = distances.get(distances.size() - 1);
        ArrayList<DataObject> rangeDataObjects = circleRangeQuery(queryPoint, refined_bound);
        rangeDataObjects.sort((o1, o2) -> {
            Double d1 = o1.calDistance(queryPoint);
            Double d2 = o2.calDistance(queryPoint);
            return d1.compareTo(d2);
        });
        ArrayList<DataObject> result = new ArrayList<>(k);
        for (int i = 0; i < rangeDataObjects.size(); i++) {
            if (i == k)
                break;
            result.add(rangeDataObjects.get(i));
        }
        return result;
    }

    public ArrayList<Double> knnDistance(final Point queryPoint, int k) {
        ArrayList<Double> result = new ArrayList<>();
        int count = 0;
        PriorityQueue<TreeNode> queue = new PriorityQueue<>(1, (o1, o2) -> {
            Double distance1 = o1.getMBR().calculateDistance(queryPoint);
            Double distance2 = o2.getMBR().calculateDistance(queryPoint);
            return distance1.compareTo(distance2);
        });

        queue.add(this.root);
        while(!queue.isEmpty()) {
            TreeNode curNode = queue.poll();
            if(curNode.getHeight() == 1) {
                ArrayList<DataObject> dataObjects = ((LeafNode<DataObject>) curNode).getEntries();
                for (DataObject dataObject : dataObjects) {
                    result.add(dataObject.calDistance(queryPoint));
                }
                count += dataObjects.size();
            }
            else {
                ArrayList<TreeNode> childNodes = ((NonLeafNode) curNode).getChildNodes();
                queue.addAll(childNodes);
            }

            if(count >= k){
                Collections.sort(result);
                ArrayList<Double> list = new ArrayList<>(k);
                for (int i = 0; i < result.size(); i++) {
                    if (i == k)
                        break;
                    list.add(result.get(i));
                }
                return result;
            }
        }
        return result;
    }

    public ArrayList<DataObject> getDataObjects() {
        ArrayList<TreeNode> leafNodes = getLeafNodes();
        ArrayList<DataObject> results = new ArrayList<>(leafNodes.size() * maxNodeNb);
        for (TreeNode leafNode : leafNodes) {
            results.addAll(((LeafNode) leafNode).getEntries());
        }
        return results;
    }

    private ArrayList<TreeNode> getLeafNodes(){
        return getTreeNode(1);
    }

    private ArrayList<TreeNode> getTreeNode(int level) {
        assert level >= 1 && level <= root.getHeight();
        TreeNode node = this.root;
        Queue<TreeNode> queue = new LinkedList<>();
        ArrayList<TreeNode> result = new ArrayList<>();

        // If root level, then return the MBR of the whole tree.
        if (node.getHeight() == level) {
            result.add(node);
            return result;
        } else if (node.getHeight() - 1 == level) {
            NonLeafNode nonLeafNode = (NonLeafNode) node;
            result.addAll(nonLeafNode.getChildNodes());
            return result;
        }

        queue.add(node);
        NonLeafNode nonLeafNode;
        while (!queue.isEmpty()) {
            nonLeafNode = (NonLeafNode) queue.poll();
            for (TreeNode child : nonLeafNode.getChildNodes()) {
                if (child.getHeight() == level + 1) {
                    result.addAll(((NonLeafNode) child).getChildNodes());
                } else if (level + 1 < child.getHeight()) {
                    queue.add(child);
                }
            }
        }
        return result;
    }

    public ArrayList<MBR> getLevelMBRs(int level) {
        ArrayList<TreeNode> treeNodes = getTreeNode(level);
        ArrayList<MBR> results = new ArrayList<>(treeNodes.size());
        for (TreeNode treeNode : treeNodes) {
            results.add(treeNode.getMBR());
        }
        return results;
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
