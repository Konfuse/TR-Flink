package com.konfuse;

import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.MBR;
import com.konfuse.geometry.PartitionedMBR;
import com.konfuse.geometry.Point;
import com.konfuse.internal.RTree;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.util.Collector;

import java.util.List;

/**
 * @Author: Konfuse
 * @Date: 2019/12/6 15:31
 */
public class Index<T extends DataObject> {
    private DataSet<RTree<PartitionedMBR>> globalTree;
    private DataSet<RTree<T>> localTrees;
    private Partitioner<T> partitioner;
    private DataSet<T> data;

    public Index(DataSet<RTree<PartitionedMBR>> globalTree, DataSet<RTree<T>> localTrees, Partitioner<T> partitioner, DataSet<T> data) {
        this.globalTree = globalTree;
        this.localTrees = localTrees;
        this.partitioner = partitioner;
        this.data = data;
    }

    public DataSet<RTree<PartitionedMBR>> getGlobalTree() {
        return globalTree;
    }

    public void setGlobalTree(DataSet<RTree<PartitionedMBR>> globalTree) {
        this.globalTree = globalTree;
    }

    public DataSet<RTree<T>> getLocalTrees() {
        return localTrees;
    }

    public void setLocalTrees(DataSet<RTree<T>> localTrees) {
        this.localTrees = localTrees;
    }

    public Partitioner<? extends DataObject> getPartitioner() {
        return partitioner;
    }

    public void setPartitioner(Partitioner<T> partitioner) {
        this.partitioner = partitioner;
    }

    public DataSet<? extends DataObject> getData() {
        return data;
    }

    public void setData(DataSet<T> data) {
        this.data = data;
    }

    public DataSet<T> boxRangeQuery(final MBR area) {
        DataSet<Integer> partitionFlags = this.globalTree
                .flatMap(new RichFlatMapFunction<RTree<PartitionedMBR>, PartitionedMBR>() {
                    @Override
                    public void flatMap(RTree<PartitionedMBR> rTree, Collector<PartitionedMBR> collector) throws Exception {
                        List<PartitionedMBR> partitionedMBRS = rTree.search(area);
                        for (PartitionedMBR partitionedMBR : partitionedMBRS) {
                            collector.collect(partitionedMBR);
                        }
                    }
                })
                .map(new MapFunction<PartitionedMBR, Integer>() {
                    @Override
                    public Integer map(PartitionedMBR partitionedMBR) throws Exception {
                        return partitionedMBR.getPartitionNumber();
                    }
                });

        DataSet<T> result = this.localTrees
                .flatMap(new RichFlatMapFunction<RTree<T>, T>() {
                    @Override
                    public void flatMap(RTree<T> rTree, Collector<T> collector) throws Exception {
                        List<Integer> partitionFlags = getRuntimeContext().getBroadcastVariable("partitionFlags");
                        if (partitionFlags.contains(getRuntimeContext().getIndexOfThisSubtask())) {
                            List<T> dataObjects = rTree.boxRangeQuery(area);
                            for (T t : dataObjects) {
                                collector.collect(t);
                            }
                        }
                    }
                }).withBroadcastSet(partitionFlags, "partitionFlags");
        return result;
    }

    public DataSet<T> circleRangeQuery(final Point queryPoint, final Float radius) {
        DataSet<Integer> partitionFlags = this.globalTree
                .flatMap(new RichFlatMapFunction<RTree<PartitionedMBR>, PartitionedMBR>() {
                    @Override
                    public void flatMap(RTree<PartitionedMBR> rTree, Collector<PartitionedMBR> collector) throws Exception {
                        List<PartitionedMBR> partitionedMBRS = rTree.search(queryPoint, radius);
                        for (PartitionedMBR partitionedMBR : partitionedMBRS) {
                            collector.collect(partitionedMBR);
                        }
                    }
                })
                .map(new MapFunction<PartitionedMBR, Integer>() {
                    @Override
                    public Integer map(PartitionedMBR partitionedMBR) throws Exception {
                        return partitionedMBR.getPartitionNumber();
                    }
                });

        DataSet<T> result = localTrees.flatMap(new RichFlatMapFunction<RTree<T>, T>() {
            @Override
            public void flatMap(RTree<T> rTree, Collector<T> collector) throws Exception {
                List<Integer> partitionFlags = getRuntimeContext().getBroadcastVariable("partitionFlags");
                if (partitionFlags.contains(getRuntimeContext().getIndexOfThisSubtask())) {
                    List<T> result = rTree.circleRangeQuery(queryPoint, radius);
                    for (T t : result) {
                        collector.collect(t);
                    }
                }
            }
        }).withBroadcastSet(partitionFlags, "partitionFlags");

        return result;
    }
}
