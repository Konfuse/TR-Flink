package com.konfuse;

import com.konfuse.geometry.*;
import com.konfuse.geopartitioner.LineSTRPartitioner;
import com.konfuse.internal.PartitionedLeafNode;
import com.konfuse.internal.RTree;
import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.util.*;

/**
 * @Author: Konfuse
 * @Date: 2019/12/10 23:16
 */
public class LineIndex extends Index<Line> implements Serializable {
    public LineIndex(DataSet<RTree<PartitionedMBR>> globalTree, DataSet<RTree<Line>> localTrees, Partitioner<Line> partitioner, DataSet<Line> data) {
        super(globalTree, localTrees, partitioner, data);
    }

    @Override
    public LineSTRPartitioner getPartitioner() {
        return (LineSTRPartitioner) super.getPartitioner();
    }

    @Override
    public DataSet<Line> getData() {
        return (DataSet<Line>) super.getData();
    }

    @Override
    public DataSet<Line> boxRangeQuery(MBR area) {
        // Get the partition number related to the query area
        DataSet<Integer> partitionFlags = getGlobalTree()
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
        DataSet<Line> result = getLocalTrees()
                .flatMap(new RichFlatMapFunction<RTree<Line>, Line>() {
                    @Override
                    public void flatMap(RTree<Line> rTree, Collector<Line> collector) throws Exception {
                        List<Integer> partitionFlags = getRuntimeContext().getBroadcastVariable("partitionFlags");
                        if (partitionFlags.contains(getRuntimeContext().getIndexOfThisSubtask())) {
                            List<Line> dataObjects = rTree.boxRangeQuery(area);
                            for (Line line : dataObjects) {
                                collector.collect(line);
                            }
                        }
                    }
                })
                .withBroadcastSet(partitionFlags, "partitionFlags");
        return result;
    }

    @Override
    public DataSet<Line> circleRangeQuery(Point queryPoint, double radius) {
        // Get the partition number related to the query area
        DataSet<Integer> partitionFlags = getGlobalTree()
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
        DataSet<Line> result = getLocalTrees()
                .flatMap(new RichFlatMapFunction<RTree<Line>, Line>() {
                    @Override
                    public void flatMap(RTree<Line> rTree, Collector<Line> collector) throws Exception {
                        List<Integer> partitionFlags = getRuntimeContext().getBroadcastVariable("partitionFlags");
                        if (partitionFlags.contains(getRuntimeContext().getIndexOfThisSubtask())) {
                            List<Line> result = rTree.circleRangeQuery(queryPoint, radius);
                            for (Line line : result) {
                                collector.collect(line);
                            }
                        }
                    }
                })
                .withBroadcastSet(partitionFlags, "partitionFlags");

        return result;
    }

    @Override
    public DataSet<Line> knnQuery(Point queryPoint, int k) {
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
        DataSet<Line> result = getLocalTrees()
                .flatMap(new FlatMapFunction<RTree<Line>, Line>() {
                    @Override
                    public void flatMap(RTree<Line> pointRTree, Collector<Line> collector) throws Exception {
                        List<Line> list = pointRTree.knnQuery(queryPoint, k);
                        for (Line line : list) {
                            collector.collect(line);
                        }
                    }
                })
                .map(new MapFunction<Line, Tuple2<Line, Double>>() {
                    @Override
                    public Tuple2<Line, Double> map(Line line) throws Exception {
                        return new Tuple2<>(line, line.calDistance(queryPoint));
                    }
                })
                .reduceGroup(new RichGroupReduceFunction<Tuple2<Line, Double>, Line>() {
                    @Override
                    public void reduce(Iterable<Tuple2<Line, Double>> iterable, Collector<Line> collector) throws Exception {
                        Iterator<Tuple2<Line, Double>> iter = iterable.iterator();
                        List<Tuple2<Line, Double>> tupleList = new ArrayList<>();
                        int count = 0;

                        while (iter.hasNext()) {
                            tupleList.add(iter.next());
                        }

                        Collections.sort(tupleList, new Comparator<Tuple2<Line, Double>>() {
                            @Override
                            public int compare(Tuple2<Line, Double> o1, Tuple2<Line, Double> o2) {
                                return o1.f1.compareTo(o2.f1);
                            }
                        });
                        while (count < k) {
                            collector.collect(tupleList.get(count).f0);
                            count++;
                        }
                    }
                });
        return result;
    }
}
