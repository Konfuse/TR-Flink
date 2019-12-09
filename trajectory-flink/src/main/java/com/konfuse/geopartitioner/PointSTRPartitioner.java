package com.konfuse.geopartitioner;

import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.PartitionedMBR;
import com.konfuse.geometry.Point;
import com.konfuse.internal.PartitionedLeafNode;
import com.konfuse.internal.RTree;
import com.konfuse.internal.TreeNode;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.List;

/**
 * @Author: Konfuse
 * @Date: 2019/12/8 11:06
 */
public class PointSTRPartitioner implements Partitioner<Point> {
    private RTree tree;

    public PointSTRPartitioner(RTree tree){
        this.tree = tree;
    }

    @Override
    public int partition(Point point, int i) {
        List<TreeNode> nodes = this.tree.search(point);
        // If we could not find MBR which contains the Point, then get all leaf nodes and calculate the distance
        if(nodes == null || nodes.isEmpty()){
            nodes = this.tree.getLeafNodes();
        }

        PartitionedMBR mbrChosen = null;
        double minDistance = Double.MIN_VALUE;
        for (TreeNode node : nodes) {
            PartitionedLeafNode partitionedLeafNode = (PartitionedLeafNode) node;
            List<PartitionedMBR> partitions = partitionedLeafNode.getEntries();
            for (PartitionedMBR partition : partitions) {
                double distance = partition.getMBR().calculateDistance(point);
                if (distance < minDistance) {
                    mbrChosen = partition;
                    minDistance = distance;
                }
            }
        }

        return mbrChosen.getPartitionNumber();
    }
}
