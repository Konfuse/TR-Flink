package com.konfuse.strtree;

import com.konfuse.geometry.Line;

import java.io.Serializable;
import java.util.List;

import static com.konfuse.strtree.RTreeNodeType.STR_TREE_LINE_LEAF_NODE;

/**
 * @author todd
 * @date 2020/5/21 11:36
 * @description: TODO
 */
public class RTreeLineLeafNode extends RTreeLeafNode<Line> implements Serializable {
    public RTreeLineLeafNode(RTreeNodeType type) {
        super(type);
    }

    public RTreeLineLeafNode(List<Line> childNodes, MBR mbr) {
        super(childNodes, mbr, STR_TREE_LINE_LEAF_NODE);
    }

    public RTreeLineLeafNode(List<Line> childNodes, MBR mbr, int height) {
        super(childNodes, mbr, height, STR_TREE_LINE_LEAF_NODE);
    }

    public RTreeLineLeafNode(int capacity) {
        super(capacity, STR_TREE_LINE_LEAF_NODE);
    }

    @Override
    public boolean equals(Object obj) {
        RTreeLineLeafNode leaf2 = (RTreeLineLeafNode) obj;
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
