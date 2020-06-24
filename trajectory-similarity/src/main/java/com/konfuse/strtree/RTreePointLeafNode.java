package com.konfuse.strtree;

import com.konfuse.geometry.Point;

import java.io.Serializable;
import java.util.List;

import static com.konfuse.strtree.RTreeNodeType.STR_TREE_POINT_LEAF_NODE;

/**
 * @author todd
 * @date 2020/5/21 11:36
 * @description: TODO
 */
public class RTreePointLeafNode extends RTreeLeafNode<Point> implements Serializable {
    public RTreePointLeafNode(RTreeNodeType type) {
        super(type);
    }

    public RTreePointLeafNode(List<Point> childNodes, MBR mbr) {
        super(childNodes, mbr, STR_TREE_POINT_LEAF_NODE);
    }

    public RTreePointLeafNode(List<Point> childNodes, MBR mbr, int height) {
        super(childNodes, mbr, height, STR_TREE_POINT_LEAF_NODE);
    }

    public RTreePointLeafNode(int capacity) {
        super(capacity, STR_TREE_POINT_LEAF_NODE);
    }

    @Override
    public boolean equals(Object obj) {
        RTreePointLeafNode leaf2 = (RTreePointLeafNode) obj;
        if(!leaf2.getEntries().containsAll(this.getEntries())){
            return false;
        }
        if(!this.getEntries().containsAll(leaf2.getEntries())){
            return false;
        }
        if(leaf2.getHeight() != this.getHeight()) {
            return false;
        }
        if(!leaf2.getMBR().equals(this.getMBR())){
            return false;
        }

        return true;
    }
}
