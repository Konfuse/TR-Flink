package com.konfuse;

import com.konfuse.geometry.Line;
import com.konfuse.geometry.Point;
import com.konfuse.internal.TreeNode;
import com.konfuse.internal.MBR;
import com.konfuse.internal.NonLeafNode;
import com.konfuse.internal.LeafNode;

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
    private TreeNode root;

    public IndexBuilder() {
        this.entryCount = 0;
        this.height = 1;
    }

    public IndexBuilder(int M, int m) {
        this.M = M;
        this.m = m;
        this.entryCount = 0;
        this.height = 1;
    }


    /*
    * r is the total count of records, i.e. entries.size()
    * M is the maximum capacity of each partition
    * p is the total count of partitions, i.e. p = r / M
    * s is the due slice count of each dimension, i.e. s = Math.sqrt(r / M)
    * ctr is records traveling count
    * */
    public RTree STRPacking(Point... points) {
        ArrayList<Point> pointList = new ArrayList<>(Arrays.stream(points).collect(Collectors.toList()));
        this.entryCount = pointList.size();

        //calculate leaf node num
        double p = pointList.size() * 1.0 / M;

        //start building r-tree leaf node
        pointList.sort(new Point.PointComparator(1));
        if (pointList.size() <= M) {
            this.root = new LeafNode<>(pointList, Point.mbr(pointList));
            return new RTree(root);
        }

        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(p) / 2));
        ArrayList<Point> list = new ArrayList<>();
        ArrayList<TreeNode> nextLevel = new ArrayList<TreeNode>();

        int ctr = 0;
        for (Point point : pointList) {
            list.add(point);
            ++ctr;
            if (ctr == s * M) {
                PackPoint(list, nextLevel);
                list.clear();
                ctr = 0;
            }
        }
        if(list.size() > 0) {
            PackPoint(list, nextLevel);
            list.clear();
        }
        return STRLeafNodePacking(nextLevel);
    }

    public RTree STRPacking(Line... lines) {
        ArrayList<Line> lineList = new ArrayList<>(Arrays.stream(lines).collect(Collectors.toList()));
        this.entryCount = lineList.size();

        //calculate leaf node num
        double p = lineList.size() * 1.0 / M;

        //start building r-tree leaf node
        lineList.sort(new MBR.MBRComparatorWithLine(1, true));

        // if size of lines <= M, then return RTree directly
        if (lineList.size() <= M) {
            this.root = new LeafNode<>(lineList, MBR.union(Line.getMBRs(lineList)));
            return new RTree(root);
        }

        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(p) / 2));
        ArrayList<Line> list = new ArrayList<>();
        ArrayList<TreeNode> nextLevel = new ArrayList<TreeNode>();

        int ctr = 0;
        for (Line line : lineList) {
            list.add(line);
            ++ctr;
            if (ctr == s * M) {
                PackLine(list, nextLevel);
                list.clear();
                ctr = 0;
            }
        }
        if(list.size() > 0) {
            PackLine(list, nextLevel);
            list.clear();
        }
        return STRLeafNodePacking(nextLevel);
    }

    public RTree STRLeafNodePacking(ArrayList<TreeNode> treeNodes) {
        // calculate partition num
        double p = treeNodes.size() * 1.0 / M;

        // start build r-tree structure bottom-to-up recursively
        treeNodes.sort(new MBR.MBRComparatorWithTreeNode(1, true));
        treeNodes = STRRecursive(p, treeNodes, 2);

        NonLeafNode nonLeafNode = new NonLeafNode(M, this.height);
        nonLeafNode.setChildNodes(treeNodes);
        nonLeafNode.setMBR(MBR.union(nonLeafNode.getMBRs()));
        this.root = nonLeafNode;
        return new RTree(root);
    }

    public ArrayList<TreeNode> STRRecursive(double p, ArrayList<TreeNode> entries, int height) {
        if (entries.size() <= M) {
            this.height = height;
            return entries;
        }
        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(p) / 2));
        ArrayList<TreeNode> list = new ArrayList<TreeNode>();
        ArrayList<TreeNode> nextLevel = new ArrayList<TreeNode>();

        int ctr = 0;
        for (TreeNode treeNode : entries) {
            list.add(treeNode);
            ++ctr;
            if (ctr == s * M) {
                PackNodes(height, list, nextLevel);
                list.clear();
                ctr = 0;
            }
        }
        if(list.size() > 0) {
            PackNodes(height, list, nextLevel);
            list.clear();
        }
        return STRRecursive(nextLevel.size() * 1.0 / M, nextLevel, height + 1);
    }

    public void PackPoint(ArrayList<Point> points, ArrayList<TreeNode> nextLevel) {
        points.sort(new Point.PointComparator(2));
        LeafNode<Point> leafNode = new LeafNode<>(M);
        for (Point point : points) {
            leafNode.getEntries().add(point);
            if (leafNode.getEntries().size() == M) {
                leafNode.setMBR(Point.mbr(leafNode.getEntries()));
                nextLevel.add(leafNode);
                leafNode = new LeafNode<Point>(M);
            }
        }
        if (leafNode.getEntries().size() > 0) {
            if (leafNode.getEntries().size() < m) {
                LeafNode<Point> swapped = (LeafNode<Point>) nextLevel.get(nextLevel.size() - 1);
                ArrayList<Point> swappedPoints = swapped.getEntries();
                ArrayList<Point> lastPoints = leafNode.getEntries();
                while (leafNode.getEntries().size() < m) {
                    lastPoints.add(0, swappedPoints.remove(swappedPoints.size() - 1));
                }
            }
            leafNode.setMBR(Point.mbr(leafNode.getEntries()));
            nextLevel.add(leafNode);
        }
    }

    public void PackLine(ArrayList<Line> lines, ArrayList<TreeNode> nextLevel) {
        lines.sort(new MBR.MBRComparatorWithLine(2, true));
        LeafNode<Line> leafNode = new LeafNode<>(M);
        for (Line line : lines) {
            leafNode.getEntries().add(line);
            if (leafNode.getEntries().size() == M) {
                leafNode.setMBR(MBR.union(Line.getMBRs(leafNode.getEntries())));
                nextLevel.add(leafNode);
                leafNode = new LeafNode<Line>(M);
            }
        }
        if (leafNode.getEntries().size() > 0) {
            if (leafNode.getEntries().size() < m) {
                LeafNode<Line> swapped = (LeafNode<Line>) nextLevel.get(nextLevel.size() - 1);
                ArrayList<Line> swappedLines = swapped.getEntries();
                ArrayList<Line> lastLines = leafNode.getEntries();
                while (leafNode.getEntries().size() < m) {
                    lastLines.add(0, swappedLines.remove(swappedLines.size() - 1));
                }
            }
            leafNode.setMBR(MBR.union(Line.getMBRs(leafNode.getEntries())));
            nextLevel.add(leafNode);
        }
    }

    //get a slide in list, and divide it into nodes
    public void PackNodes(int height, ArrayList<TreeNode> list, ArrayList<TreeNode> nextLevel) {
        list.sort(new MBR.MBRComparatorWithTreeNode(2, true));
        NonLeafNode nonLeafNode = new NonLeafNode(M, height);
        for (TreeNode treeNode : list) {
            nonLeafNode.getChildNodes().add(treeNode);
            if (nonLeafNode.getChildNodes().size() == M) {
                nonLeafNode.setMBR(MBR.union(nonLeafNode.getMBRs()));
                nextLevel.add(nonLeafNode);
                nonLeafNode = new NonLeafNode(M, height);
            }
        }
        if (nonLeafNode.getChildNodes().size() > 0) {
            if (nonLeafNode.getChildNodes().size() < m) {
                NonLeafNode swapped = (NonLeafNode) nextLevel.get(nextLevel.size() - 1);
                ArrayList<TreeNode> lastTreeNodes = nonLeafNode.getChildNodes();
                ArrayList<TreeNode> swappedTreeNodes = swapped.getChildNodes();
                while (nonLeafNode.getChildNodes().size() < m) {
                    lastTreeNodes.add(0, swappedTreeNodes.remove(swappedTreeNodes.size() - 1));
                }
            }
            nonLeafNode.setMBR(MBR.union(nonLeafNode.getMBRs()));
            nextLevel.add(nonLeafNode);
        }
    }
}
