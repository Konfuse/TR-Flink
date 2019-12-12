package com.konfuse.geopartitioner;

import com.konfuse.geometry.Line;
import com.konfuse.geometry.PartitionedMBR;
import com.konfuse.internal.LeafNode;
//import com.konfuse.internal.PartitionedLeafNode;
import com.konfuse.internal.RTree;
import com.konfuse.internal.TreeNode;
import org.apache.flink.api.common.functions.Partitioner;

import java.util.ArrayList;
import java.util.List;

/**
 * The implementation of Partitioner overriding the partition method
 * helps to decide which partition the line should be assigned to.
 *
 * The partition method returns partition number of the line.
 *
 * @Author: Konfuse
 * @Date: 2019/12/9 2:19
 */
public class LineSTRPartitioner implements Partitioner<Line> {
    private RTree<PartitionedMBR> tree;

    public LineSTRPartitioner(RTree<PartitionedMBR> tree){
        this.tree = tree;
    }

    @Override
    public int partition(Line line, int i) {
        // If we could not find MBR which contains the Point, then get all leaf nodes and calculate the distance
        ArrayList<PartitionedMBR> partitions = this.tree.search(line);

        PartitionedMBR mbrChosen = null;
        double minDistance = Double.MAX_VALUE;

        // travel nodes to find the partitionedMbr that is closest to the line, then return its partition number.
        for (PartitionedMBR partition : partitions) {
            double distance = partition.getMBR().calculateDistance(line);
            if (distance < minDistance) {
                mbrChosen = partition;
                minDistance = distance;
            }
        }

        return mbrChosen.getPartitionNumber();
    }

    public RTree<PartitionedMBR> getTree() {
        return tree;
    }

    public void setTree(RTree<PartitionedMBR> tree) {
        this.tree = tree;
    }
}
