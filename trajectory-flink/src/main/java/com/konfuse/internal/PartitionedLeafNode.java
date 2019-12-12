package com.konfuse.internal;

import com.konfuse.geometry.MBR;
import com.konfuse.geometry.PartitionedMBR;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The leaf node of r-tree containing a list of Partitioned MBR entries.
 *
 * Extends form LeafNodes.
 * entries: is the list of Partitioned MBR entries in the r-tree,
 *
 * @Author: Konfuse
 * @Date: 2019/12/8 1:56
 */
public class PartitionedLeafNode extends LeafNode<PartitionedMBR> implements Serializable {
    public PartitionedLeafNode(){
    }

    public PartitionedLeafNode(ArrayList<PartitionedMBR> entries, MBR mbr) {
        super(entries, mbr);
    }
}
