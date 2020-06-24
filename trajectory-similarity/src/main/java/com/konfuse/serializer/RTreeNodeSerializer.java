package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.geometry.Line;
import com.konfuse.geometry.Point;
import com.konfuse.geometry.Rectangle;
import com.konfuse.strtree.*;

import java.util.ArrayList;
import java.util.List;

import static com.konfuse.strtree.RTreeNodeType.STR_TREE_INTERNAL_NODE;
import static com.konfuse.strtree.RTreeNodeType.STR_TREE_ROOT_NODE;

/**
 * @author todd
 * @date 2020/5/20 21:59
 * @description: TODO
 */
public class RTreeNodeSerializer extends Serializer<RTreeNode> {
    @Override
    public void write(Kryo kryo, Output output, RTreeNode object) {
        kryo.writeObject(output, MBR.class);
        output.writeBoolean(object.isLeaf());
        output.writeInt(object.getHeight());
        output.writeInt(object.getTypeNum());
        output.writeInt(object.getSize());
        if(object instanceof RTreeInternalNode) {
            List<RTreeNode> childNodes = ((RTreeInternalNode) object).getChildNodes();
            for (int i = 0; i < childNodes.size(); i++) {
                kryo.writeObject(output, childNodes.get(i));
            }
        } else {
            if (object instanceof RTreePointLeafNode) {
                RTreePointLeafNode leaf = (RTreePointLeafNode) object;
                for (int i = 0; i < leaf.getSize(); i++) {
                    kryo.writeObject(output, leaf.getEntries().get(i));
                }
            } else if(object instanceof RTreeLineLeafNode) {
                RTreeLineLeafNode leaf = (RTreeLineLeafNode) object;
                for (int i = 0; i < leaf.getSize(); i++) {
                    kryo.writeObject(output, leaf.getEntries().get(i));
                }
            } else {
                RTreeRectLeafNode leaf = (RTreeRectLeafNode) object;
                for (int i = 0; i < leaf.getSize(); i++) {
                    kryo.writeObject(output, leaf.getEntries().get(i));
                }
            }
        }
    }

    @Override
    public RTreeNode read(Kryo kryo, Input input, Class<RTreeNode> type) {
        MBR mbr = kryo.readObject(input, MBR.class);
        Boolean isLeaf = input.readBoolean();
        int height = input.readInt();
        int typeNum = input.readInt();
        int size = input.readInt();
        if (!isLeaf) {
            RTreeNodeType nodeType;
            ArrayList<RTreeNode> childNodes = new ArrayList<RTreeNode>(size);
            for (int i = 0; i < size; i++) {
                RTreeInternalNode child = kryo.readObject(input, RTreeInternalNode.class);
                childNodes.add(child);
            }
            if (typeNum == 1) {
                nodeType = STR_TREE_ROOT_NODE;
            } else {
                nodeType = STR_TREE_INTERNAL_NODE;
            }
            RTreeInternalNode node = new RTreeInternalNode(childNodes, mbr, height, nodeType);
            return node;
        } else if(typeNum == 3) {
            ArrayList<Point> childNodes = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                Point point = kryo.readObject(input, Point.class);
                childNodes.add(point);
            }
            RTreePointLeafNode leaf = new RTreePointLeafNode(childNodes, mbr, height);
            return leaf;
        } else if(typeNum == 4) {
            ArrayList<Line> childNodes = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                Line line = kryo.readObject(input, Line.class);
                childNodes.add(line);
            }
            RTreeLineLeafNode leaf = new RTreeLineLeafNode(childNodes, mbr, height);
            return leaf;
        } else {
            ArrayList<Rectangle> childNodes = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                Rectangle rect = kryo.readObject(input, Rectangle.class);
                childNodes.add(rect);
            }
            RTreeRectLeafNode leaf = new RTreeRectLeafNode(childNodes, mbr, height);
            return leaf;
        }
    }
}
