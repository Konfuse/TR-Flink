package com.konfuse;

import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.MBR;
import com.konfuse.geometry.PartitionedMBR;
import com.konfuse.geometry.Point;
import com.konfuse.internal.RTree;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

import java.util.List;

/**
 * Distributed index structure in the whole system.
 *
 * Index includes two types: point index and line index.
 * Each type of two indexes contains a global r-tree which is a PartitionedMBR r-tree.
 *
 * Global r-tree maintains the information of flink partitions.
 *
 * Local r-trees maintain a data set of local trees. And partitioner
 * is the corresponding partition method.
 *
 * data contains all the data records maintained in r-tree.
 *
 * Need to specify generics when initializing the class:
 * either Point or Line.
 *
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

    /**
     * Get the data objects inside the query area.
     * @param area query area.
     * @return DataSet of data objects according to box range query.
     */
    public DataSet<T> boxRangeQuery(final MBR area) {
        // Get the partition number related to the query area
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

        // In partition, if partition number is in partitionFlags,
        // do box range query in the corresponding  local tree.
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

    /**
     * Get the data objects inside the query circle.
     * @param radius the radius of circle
     * @param queryPoint circle center
     * @return DataSet of data objects according to circle range query.
     */
    public DataSet<T> circleRangeQuery(final Point queryPoint, final double radius) {
        // Get the partition number related to the query area
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

        // In partition, if partition number is in partitionFlags,
        // do circle range query in the corresponding  local tree.
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

    /**
     * Get the nearest k data objects from query point.
     * @param queryPoint the point to query
     * @param k the size of result set
     * @return DataSet of data objects according of knn query
     */
    public DataSet<T> knnQuery(final Point queryPoint, final int k) throws Exception {
//        final DataSet<T> sampleData = DataSetUtils.sample(this.data, false, 0.3);
//        DataSet<RTree<T>> trees = sampleData.reduceGroup(new RichGroupReduceFunction<T, RTree<T>>() {
//            @Override
//            public void reduce(Iterable<T> iterable, Collector<RTree<T>> collector) throws Exception {
//                ArrayList<T> list = new ArrayList<>();
//                for (T t : iterable) {
//                    list.add(t);
//                }
//                RTree<T> tree = createLineLocalRTree(list, max, min);
//                collector.collect(tree);
//            }
//        });

        // find the distance of k nearest nodes from the query point.
        DataSet<Double> knnDistances = this.localTrees
                .flatMap(new RichFlatMapFunction<RTree<T>, Double>() {
                    @Override
                    public void flatMap(RTree<T> rTree, Collector<Double> collector) throws Exception {
                        List<Double> list = rTree.knnDistance(queryPoint, k);
                        for (Double distance : list) {
                            collector.collect(distance);
                        }
                    }
                })
                .sortPartition("value", Order.ASCENDING)
                .first(k);

        // circle range query using the furthest distance as radius
        double refined_bound = knnDistances.collect().get(k - 1);
        // sort data objects by distance
        DataSet<T> result = circleRangeQuery(queryPoint, refined_bound)
                .map(new MapFunction<T, Tuple2<Double, T>>() {
                    @Override
                    public Tuple2<Double, T> map(T t) throws Exception {
                        return new Tuple2<>(t.calDistance(queryPoint), t);
                    }
                })
                .sortPartition(0, Order.ASCENDING)
                .first(k)
                .map(new MapFunction<Tuple2<Double, T>, T>() {
                    @Override
                    public T map(Tuple2<Double, T> tuple2) throws Exception {
                        return tuple2.f1;
                    }
                });
        return result;
    }
}
