package com.konfuse.strtree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import static com.konfuse.strtree.RTreeNodeType.*;

/**
 * The non-leaf node that is the internal node in the r-tree extends TreeNode.
 * It has an attribute referring to the list of internal tree nodes in the r-tree,
 * i.e.  childNodes.
 *
 * @Author: Konfuse
 * @Date: 2019/11/26 11:10
 */
public class RTreeInternalNode extends RTreeNode implements Serializable {
    private List<RTreeNode> childNodes;

    public RTreeInternalNode() {
        this.childNodes = new ArrayList<>();
    }

    public RTreeInternalNode(ArrayList<RTreeNode> childNodes, MBR mbr, int height) {
        super(mbr, height,  childNodes.size(),false, STR_TREE_INTERNAL_NODE);
        this.childNodes = childNodes;
    }
    public RTreeInternalNode(ArrayList<RTreeNode> childNodes, MBR mbr, int height, RTreeNodeType type) {
        super(mbr, height,  childNodes.size(),false, type);
        this.childNodes = childNodes;
    }

    public RTreeInternalNode(int capacity, int height) {
        super(new MBR(), height, false, STR_TREE_INTERNAL_NODE);
        this.childNodes = new ArrayList<>(capacity);
    }

    public RTreeInternalNode(int capacity, int height, RTreeNodeType type) {
        super(new MBR(), height, false, type);
        this.childNodes = new ArrayList<>(capacity);
    }

    public List<RTreeNode> getChildNodes() {
        return childNodes;
    }

    public void setChildNodes(List<RTreeNode> childNodes) {
        this.childNodes = childNodes;
        this.setSize(childNodes.size());
    }

    public MBR[] getMbrs() {
        MBR[] mbrs = new MBR[childNodes.size()];
        int i = 0;
        for (RTreeNode RTreeNode : childNodes) {
            mbrs[i] = RTreeNode.getMBR();
            i++;
        }
        return mbrs;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("");
        str.append("< ");
        for(int i = 0; i< this.childNodes.size(); i++){
            RTreeNode node = this.childNodes.get(i);
            str.append(node.toString() + " , ");
        }
        str.append(" > \n");
        return str.toString();
    }
}
