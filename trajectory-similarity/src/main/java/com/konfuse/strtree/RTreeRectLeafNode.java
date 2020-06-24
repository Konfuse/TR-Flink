package com.konfuse.strtree;

import com.konfuse.geometry.Rectangle;

import java.io.Serializable;
import java.util.List;

import static com.konfuse.strtree.RTreeNodeType.STR_TREE_RECTANGLE_LEAF_NODE;

/**
 * @author todd
 * @date 2020/5/21 11:37
 * @description: TODO
 */
public class RTreeRectLeafNode extends RTreeLeafNode<Rectangle> implements Serializable {
    public RTreeRectLeafNode(RTreeNodeType type) {
        super(type);
    }

    public RTreeRectLeafNode(List<Rectangle> childNodes, MBR mbr) {
        super(childNodes, mbr, STR_TREE_RECTANGLE_LEAF_NODE);
    }

    public RTreeRectLeafNode(List<Rectangle> childNodes, MBR mbr, int height) {
        super(childNodes, mbr, height, STR_TREE_RECTANGLE_LEAF_NODE);
    }

    public RTreeRectLeafNode(int capacity) {
        super(capacity, STR_TREE_RECTANGLE_LEAF_NODE);
    }

    @Override
    public boolean equals(Object obj) {
        RTreeRectLeafNode leaf2 = (RTreeRectLeafNode) obj;
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
