package com.konfuse;

import com.konfuse.geometry.Line;
import com.konfuse.geometry.Point;
import com.konfuse.geometry.Rectangle;
import com.konfuse.internal.LeafNode;
import com.konfuse.internal.MBR;
import com.konfuse.internal.NonLeafNode;
import com.konfuse.internal.TreeNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Help to Build r-tree.
 * Three Attributes are important:
 * M: is the maximum number of nodes in the TreeNodes
 * m: is the minimum number of nodes in the TreeNodes
 * root: is the r-tree root node
 * Note that: m is no less than half of M.
 *
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


    /**
     * Build points r-tree using STR method.
     * r is the total count of records, i.e. entries.size()
     * M is the maximum capacity of each partition
     * p is the total count of partitions, i.e. p = r / M
     * s is the due slice count of each dimension, i.e. s = Math.sqrt(r / M)
     * ctr is records traveling count
     * @param points the input points
     */
    public RTree createRTreeBySTR(Point... points) {
        ArrayList<Point> pointList = new ArrayList<>(Arrays.stream(points).collect(Collectors.toList()));
        this.entryCount = pointList.size();

        //calculate leaf node num
        double p = pointList.size() * 1.0 / M;

        //start building r-tree leaf node
        pointList.sort(new Point.PointComparator(1));

        // if size is less than M, then pack the root directly.
        if (pointList.size() <= M) {
            this.height = 1;
            this.root = new LeafNode<>(pointList, Point.unionPoints(pointList));
            return new RTree(root, M, m);
        }

        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(p) / 2));
        ArrayList<Point> list = new ArrayList<>();
        ArrayList<TreeNode> nextLevel = new ArrayList<TreeNode>();

        int ctr = 0;
        for (Point point : pointList) {
            list.add(point);
            ++ctr;
            if (ctr == s * M) {
                packPoints(list, nextLevel);
                list.clear();
                ctr = 0;
            }
        }
        // the size of the last slide may be lower than s * M
        if(list.size() > 0) {
            packPoints(list, nextLevel);
            list.clear();
        }
        return leafNodePacking(nextLevel);
    }

    /**
     * Build lines r-tree using STR method.
     * r is the total count of records, i.e. entries.size()
     * M is the maximum capacity of each partition
     * p is the total count of partitions, i.e. p = r / M
     * s is the due slice count of each dimension, i.e. s = Math.sqrt(r / M)
     * ctr is records traveling count
     * @param lines the input lines
     */
    public RTree createRTreeBySTR(Line... lines) {
        ArrayList<Line> lineList = new ArrayList<>(Arrays.stream(lines).collect(Collectors.toList()));
        this.entryCount = lineList.size();

        //calculate leaf node num
        double p = lineList.size() * 1.0 / M;

        //start building r-tree leaf node
        lineList.sort(new MBR.MBRComparatorWithLine(1, true));

        // if size of lines <= M, then return RTree directly
        if (lineList.size() <= M) {
            this.height = 1;
            this.root = new LeafNode<>(lineList, Line.unionLines(lineList));
            return new RTree(root, M, m);
        }

        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(p) / 2));
        ArrayList<Line> list = new ArrayList<>();
        ArrayList<TreeNode> nextLevel = new ArrayList<TreeNode>();

        int ctr = 0;
        for (Line line : lineList) {
            list.add(line);
            ++ctr;
            if (ctr == s * M) {
                packLines(list, nextLevel);
                list.clear();
                ctr = 0;
            }
        }
        // the size of the last slide may be lower than s * M
        if(list.size() > 0) {
            packLines(list, nextLevel);
            list.clear();
        }
        return leafNodePacking(nextLevel);
    }

    public RTree createRTreeBySTR(Rectangle... rectangles) {
        ArrayList<Rectangle> rectangleList = new ArrayList<>(Arrays.stream(rectangles).collect(Collectors.toList()));
        this.entryCount = rectangleList.size();

        //calculate leaf node num
        double p = rectangleList.size() * 1.0 / M;

        //start building r-tree leaf node
        rectangleList.sort(new Rectangle.RectangleComparator(1, true));

        // if size of lines <= M, then return RTree directly
        if (rectangleList.size() <= M) {
            this.height = 1;
            MBR[] mbrs = new MBR[rectangleList.size()];
            for (int i = 0; i < rectangleList.size(); i++) {
                mbrs[i] = rectangleList.get(i).getMBR();
            }
            this.root = new LeafNode<>(rectangleList, MBR.union(mbrs));
            return new RTree(root, M, m);
        }

        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(p) / 2));
        ArrayList<Rectangle> list = new ArrayList<>();
        ArrayList<TreeNode> nextLevel = new ArrayList<TreeNode>();

        int ctr = 0;
        for (Rectangle rectangle : rectangleList) {
            list.add(rectangle);
            ++ctr;
            if (ctr == s * M) {
                packRectangles(list, nextLevel);
                list.clear();
                ctr = 0;
            }
        }
        // the size of the last slide may be lower than s * M
        if(list.size() > 0) {
            packRectangles(list, nextLevel);
            list.clear();
        }
        return leafNodePacking(nextLevel);
    }

    /**
     * Pack the leaf nodes that have been packed from data objects in the last step.
     * @param treeNodes list of leaf nodes
     */
    private RTree leafNodePacking(ArrayList<TreeNode> treeNodes) {
        // calculate partition num
        double p = treeNodes.size() * 1.0 / M;

        // start build r-tree structure bottom-to-up recursively
        treeNodes.sort(new MBR.MBRComparatorWithTreeNode(1, true));
        treeNodes = buildRecursivelyBySTR(p, treeNodes, 1);

        // pack the root
        NonLeafNode nonLeafNode = new NonLeafNode(M, this.height + 1);
        nonLeafNode.setChildNodes(treeNodes);
        nonLeafNode.setMBR(MBR.union(nonLeafNode.getMBRs()));

        this.root = nonLeafNode;
        return new RTree(root, M, m);
    }

    /**
     * Build r-tree bottom-to-up recursively
     * @param p partition num
     * @param entries list of tree nodes
     * @param height current height of building tree
     */
    private ArrayList<TreeNode> buildRecursivelyBySTR(double p, ArrayList<TreeNode> entries, int height) {
        // entries num in node should be no more than M, but if size <= M, return entries directly.
        if (entries.size() <= M) {
            this.height = height;
            return entries;
        }

        // calculate slides num
        int s = (int) Math.ceil(Math.pow(Math.E, Math.log(p) / 2));
        ArrayList<TreeNode> list = new ArrayList<>();
        ArrayList<TreeNode> nextLevel = new ArrayList<>();

        // start pack nodes
        int ctr = 0;
        for (TreeNode treeNode : entries) {
            list.add(treeNode);
            ++ctr;
            if (ctr == s * M) {
                packNodes(height, list, nextLevel);
                list.clear();
                ctr = 0;
            }
        }

        // the size of the last slide may be lower than s * M
        if(list.size() > 0) {
            packNodes(height, list, nextLevel);
            list.clear();
        }
        return buildRecursivelyBySTR(nextLevel.size() * 1.0 / M, nextLevel, height + 1);
    }


    /**
     * Pack points to leaf nodes.
     * @param points the sorted points by x dimension in a slide.
     * @param nextLevel the list to load leaf nodes.
     */
    private void packPoints(ArrayList<Point> points, ArrayList<TreeNode> nextLevel) {
        // sort by the y dimension
        points.sort(new Point.PointComparator(2));

        // pack points to leaf nodes
        LeafNode<Point> leafNode = new LeafNode<>(M);
        for (Point point : points) {
            leafNode.getEntries().add(point);
            if (leafNode.getEntries().size() == M) {
                leafNode.setMBR(Point.unionPoints(leafNode.getEntries()));
                nextLevel.add(leafNode);
                leafNode = new LeafNode<Point>(M);
            }
        }

        // the size of the last leaf node may be lower than m.
        // add records into it from neighbor node until the last node no less than m.
        if (leafNode.getEntries().size() > 0) {
            if (leafNode.getEntries().size() < m) {
                LeafNode<Point> swapped = (LeafNode<Point>) nextLevel.get(nextLevel.size() - 1);
                ArrayList<Point> swappedPoints = swapped.getEntries();
                ArrayList<Point> lastPoints = leafNode.getEntries();
                while (leafNode.getEntries().size() < m) {
                    lastPoints.add(0, swappedPoints.remove(swappedPoints.size() - 1));
                }
            }
            leafNode.setMBR(Point.unionPoints(leafNode.getEntries()));
            nextLevel.add(leafNode);
        }
    }

    /**
     * Pack lines to leaf nodes.
     * @param lines the sorted lines by x dimension in a slide.
     * @param nextLevel the list to load leaf nodes.
     */
    private void packLines(ArrayList<Line> lines, ArrayList<TreeNode> nextLevel) {
        // sort by the y dimension
        lines.sort(new MBR.MBRComparatorWithLine(2, true));

        // pack lines to leaf nodes
        LeafNode<Line> leafNode = new LeafNode<>(M);
        for (Line line : lines) {
            leafNode.getEntries().add(line);
            if (leafNode.getEntries().size() == M) {
                leafNode.setMBR(Line.unionLines(leafNode.getEntries()));
                nextLevel.add(leafNode);
                leafNode = new LeafNode<Line>(M);
            }
        }

        // the size of the last leaf node may be lower than m.
        // add records into it from neighbor node until the last node no less than m.
        if (leafNode.getEntries().size() > 0) {
            if (leafNode.getEntries().size() < m) {
                LeafNode<Line> swapped = (LeafNode<Line>) nextLevel.get(nextLevel.size() - 1);
                ArrayList<Line> swappedLines = swapped.getEntries();
                ArrayList<Line> lastLines = leafNode.getEntries();
                while (leafNode.getEntries().size() < m) {
                    lastLines.add(0, swappedLines.remove(swappedLines.size() - 1));
                }
            }
            leafNode.setMBR(Line.unionLines(leafNode.getEntries()));
            nextLevel.add(leafNode);
        }
    }

    /**
     * Pack rectangles to leaf nodes.
     * @param rectangles the sorted rectangles by x dimension in a slide.
     * @param nextLevel the list to load leaf nodes.
     */
    private void packRectangles(ArrayList<Rectangle> rectangles, ArrayList<TreeNode> nextLevel) {
        // sort by the y dimension
        rectangles.sort(new Rectangle.RectangleComparator(2, true));

        // pack rectangles to leaf nodes
        LeafNode<Rectangle> leafNode = new LeafNode<>(M);
        for (Rectangle rectangle : rectangles) {
            leafNode.getEntries().add(rectangle);
            if (leafNode.getEntries().size() == M) {
                ArrayList<Rectangle> list = leafNode.getEntries();
                MBR[] mbrs = new MBR[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    mbrs[i] = list.get(i).getMBR();
                }
                leafNode.setMBR(MBR.union(mbrs));
                nextLevel.add(leafNode);
                leafNode = new LeafNode<>(M);
            }
        }

        // the size of the last leaf node may be lower than m.
        // add records into it from neighbor node until the last node no less than m.
        if (leafNode.getEntries().size() > 0) {
            if (leafNode.getEntries().size() < m) {
                LeafNode<Rectangle> swapped = (LeafNode<Rectangle>) nextLevel.get(nextLevel.size() - 1);
                ArrayList<Rectangle> swappedRectangles = swapped.getEntries();
                ArrayList<Rectangle> lastRectangles = leafNode.getEntries();
                while (leafNode.getEntries().size() < m) {
                    lastRectangles.add(0, swappedRectangles.remove(swappedRectangles.size() - 1));
                }
            }
            ArrayList<Rectangle> list = leafNode.getEntries();
            MBR[] mbrs = new MBR[list.size()];
            for (int i = 0; i < list.size(); i++) {
                mbrs[i] = list.get(i).getMBR();
            }
            leafNode.setMBR(MBR.union(mbrs));
            nextLevel.add(leafNode);
        }
    }

    /**
     * Pack nodes to non-leaf nodes.
     * @param height current height of the building tree
     * @param list the sorted nodes by x dimension in a slide.
     * @param nextLevel the list to load non-leaf nodes.
     */
    private void packNodes(int height, ArrayList<TreeNode> list, ArrayList<TreeNode> nextLevel) {
        // sort by the y dimension
        list.sort(new MBR.MBRComparatorWithTreeNode(2, true));

        // pack nodes to non-leaf nodes
        NonLeafNode nonLeafNode = new NonLeafNode(M, height + 1);
        for (TreeNode treeNode : list) {
            nonLeafNode.getChildNodes().add(treeNode);
            if (nonLeafNode.getChildNodes().size() == M) {
                nonLeafNode.setMBR(MBR.union(nonLeafNode.getMBRs()));
                nextLevel.add(nonLeafNode);
                nonLeafNode = new NonLeafNode(M, height + 1);
            }
        }

        // the size of the last node may be lower than m.
        // add records into it from neighbor node until the last node's size no less than m.
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
