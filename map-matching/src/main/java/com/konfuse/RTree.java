package com.konfuse;

import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.Point;
import com.konfuse.internal.LeafNode;
import com.konfuse.internal.MBR;
import com.konfuse.internal.NonLeafNode;
import com.konfuse.internal.TreeNode;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Point to the actual r-tree. Four important attributes:
 * root: the root of r-tree
 * height: r-tree's height
 * maxNodeNb: the maximum children size of nodes
 * minNodeNb: the minimum children size of nodes
 *
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

    /**
     * Get the data objects inside the query area.
     * @param area query area.
     * @return DataObject list result of box range query
     */
    public ArrayList<DataObject> boxRangeQuery(MBR area) {
        // if area does not intersects the root bound, return null directly
        if (!MBR.intersects(this.root.getMBR(), area)) {
            return new ArrayList<>();
        }

        // fetch data objects by bfs
        Queue<TreeNode> queue = new LinkedBlockingQueue<>();
        TreeNode node;
        queue.add(this.root);
        ArrayList<DataObject> result = new ArrayList<> ();
        while (!queue.isEmpty()) {
            node = queue.poll();
            // if leaf node, judge every data objects
            if (node.getHeight() == 1) {
                ArrayList<DataObject> dataObjects = ((LeafNode<DataObject>) node).getEntries();
                for (DataObject dataObject : dataObjects) {
                    if (area.contains(dataObject))
                        result.add(dataObject);
                }
            } else {
                // if non-leaf node, push the intersects nodes into queue
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

    /**
     * Get the data objects inside the query circle.
     * @param radius the radius of circle
     * @param queryPoint circle center
     * @return DataObject list result of circle query
     */
    public ArrayList<DataObject> circleRangeQuery(final Point queryPoint, double radius) {
        ArrayList<DataObject> result = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(this.root);

        // fetch data objects by bfs
        while (!queue.isEmpty()) {
            TreeNode curNode = queue.poll();
            if(curNode.getMBR().intersects(queryPoint, radius)) {
                // if leaf node, judge every data objects
                if(curNode.getHeight() == 1) {
                    ArrayList<DataObject> entries = ((LeafNode<DataObject>) curNode).getEntries();
                    for (DataObject dataObject: entries) {
                        if(dataObject.calDistance(queryPoint) <= radius){
                            result.add(dataObject);
                        }
                    }
                }
                else {
                    // if non-leaf node, push nodes into queue
                    ArrayList<TreeNode> childNodes = ((NonLeafNode) curNode).getChildNodes();
                    queue.addAll(childNodes);
                }
            }
        }
        return result;
    }

    /**
     * Get the nearest k data objects from query point.
     * @param queryPoint the point to query
     * @param k the size of result set
     * @return DataObject list result of knn query
     */
    public ArrayList<DataObject> knnQuery(final Point queryPoint, int k) {
        // find the distance nearest k nodes from the query point.
        ArrayList<Double> distances = this.knnDistance(queryPoint, k);

        // circle range query using the furthest distance as radius
        double refined_bound = distances.get(distances.size() - 1);
        ArrayList<DataObject> rangeDataObjects = circleRangeQuery(queryPoint, refined_bound);

        // sort by distance
        rangeDataObjects.sort((o1, o2) -> {
            Double d1 = o1.calDistance(queryPoint);
            Double d2 = o2.calDistance(queryPoint);
            return d1.compareTo(d2);
        });

        // get top-k
        ArrayList<DataObject> result = new ArrayList<>(k);
        for (int i = 0; i < rangeDataObjects.size(); i++) {
            if (i == k)
                break;
            result.add(rangeDataObjects.get(i));
        }
        return result;
    }

    /**
     * Find distance of the nearest k nodes from the query point.
     * @param k the size of result
     * @param queryPoint the point to query
     * @return Double list result of knn distance query
     */
    public ArrayList<Double> knnDistance(final Point queryPoint, int k) {
        ArrayList<Double> result = new ArrayList<>();
        int count = 0;

        // priority queue to load tree nodes
        PriorityQueue<TreeNode> queue = new PriorityQueue<>(1, (o1, o2) -> {
            Double distance1 = o1.getMBR().calculateDistance(queryPoint);
            Double distance2 = o2.getMBR().calculateDistance(queryPoint);
            return distance1.compareTo(distance2);
        });

        // find the top-k nearest tree nodes using bfs
        queue.add(this.root);
        while(!queue.isEmpty()) {
            TreeNode curNode = queue.poll();
            // if leaf node, add all data objects into result
            if(curNode.getHeight() == 1) {
                ArrayList<DataObject> dataObjects = ((LeafNode<DataObject>) curNode).getEntries();
                for (DataObject dataObject : dataObjects) {
                    result.add(dataObject.calDistance(queryPoint));
                }
                count += dataObjects.size();
            }
            else {
                // if non-leaf node, add all nodes into queue
                ArrayList<TreeNode> childNodes = ((NonLeafNode) curNode).getChildNodes();
                queue.addAll(childNodes);
            }

            // fetch top k into result list
            if(count >= k){
                Collections.sort(result);
                ArrayList<Double> list = new ArrayList<>(k);
                for (int i = 0; i < result.size(); i++) {
                    if (i == k)
                        break;
                    list.add(result.get(i));
                }
                return list;
            }
        }
        return result;
    }

    /**
     * Get all data objects inside r-tree.
     * @return result list of data objects
     */
    public ArrayList<DataObject> getDataObjects() {
        ArrayList<TreeNode> leafNodes = getLeafNodes();
        ArrayList<DataObject> results = new ArrayList<>(leafNodes.size() * maxNodeNb);
        for (TreeNode leafNode : leafNodes) {
            results.addAll(((LeafNode) leafNode).getEntries());
        }
        return results;
    }

    /**
     * Get all leaf nodes inside r-tree.
     * @return result list of leaf nodes
     */
    private ArrayList<TreeNode> getLeafNodes(){
        return getTreeNode(1);
    }


    /**
     * Get all tree nodes at height.
     * @return result list of tree nodes
     */
    private ArrayList<TreeNode> getTreeNode(int height) {
        assert height >= 1 && height <= root.getHeight();
        TreeNode node = this.root;
        Queue<TreeNode> queue = new LinkedList<>();
        ArrayList<TreeNode> result = new ArrayList<>();

        // If root level, then return the MBR of the whole tree.
        if (node.getHeight() == height) {
            result.add(node);
            return result;
        } else if (node.getHeight() - 1 == height) {
            NonLeafNode nonLeafNode = (NonLeafNode) node;
            result.addAll(nonLeafNode.getChildNodes());
            return result;
        }

        queue.add(node);
        NonLeafNode nonLeafNode;
        while (!queue.isEmpty()) {
            nonLeafNode = (NonLeafNode) queue.poll();
            for (TreeNode child : nonLeafNode.getChildNodes()) {
                if (child.getHeight() == height + 1) {
                    result.addAll(((NonLeafNode) child).getChildNodes());
                } else if (height + 1 < child.getHeight()) {
                    queue.add(child);
                }
            }
        }
        return result;
    }

    /**
     * Get all mbrs of tree nodes at height
     * @return result list of mbrs
     */
    public ArrayList<MBR> getMBRsWithHeight(int height) {
        ArrayList<TreeNode> treeNodes = getTreeNode(height);
        ArrayList<MBR> results = new ArrayList<>(treeNodes.size());
        for (TreeNode treeNode : treeNodes) {
            results.add(treeNode.getMBR());
        }
        return results;
    }

    /**
     * Method to serialize this r-tree
     */
    public void save(String file) throws IOException {
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
        outputStream.writeObject(this);
        outputStream.close();
    }

    /**
     * Method to deserialize the file to a r-tree
     * @param file r-tree model path
     * @return r-tree object
     */
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
