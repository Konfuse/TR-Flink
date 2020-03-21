package com.konfuse;

import com.konfuse.internal.MBR;
import com.konfuse.geometry.PartitionedMBR;
import com.konfuse.geometry.Point;
import com.konfuse.geopartitioner.PointSTRPartitioner;
import com.konfuse.internal.RTree;
import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.util.*;

/**
 * @Author: Konfuse
 * @Date: 2019/12/10 23:15
 */
public class PointIndex extends Index<Point> implements Serializable {
    public PointIndex(DataSet<RTree<PartitionedMBR>> globalTree, DataSet<RTree<Point>> localTrees, Partitioner<Point> partitioner, DataSet<Point> data) {
        super(globalTree, localTrees, partitioner, data);
    }

    @Override
    public PointSTRPartitioner getPartitioner() {
        return (PointSTRPartitioner) super.getPartitioner();
    }

    @Override
    public DataSet<Point> getData() {
        return (DataSet<Point>) super.getData();
    }

    @Override
    public DataSet<Point> boxRangeQuery(MBR area) {
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
        DataSet<Point> result = getLocalTrees()
                .flatMap(new RichFlatMapFunction<RTree<Point>, Point>() {
                    @Override
                    public void flatMap(RTree<Point> rTree, Collector<Point> collector) throws Exception {
                        List<Integer> partitionFlags = getRuntimeContext().getBroadcastVariable("partitionFlags");
                        if (partitionFlags.contains(getRuntimeContext().getIndexOfThisSubtask())) {
                            List<Point> dataObjects = rTree.boxRangeQuery(area);
                            for (Point point : dataObjects) {
                                collector.collect(point);
                            }
                        }
                    }
                })
                .withBroadcastSet(partitionFlags, "partitionFlags");
        return result;
    }

    @Override
    public DataSet<Point> circleRangeQuery(Point queryPoint, double radius) {
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
        DataSet<Point> result = getLocalTrees()
                .flatMap(new RichFlatMapFunction<RTree<Point>, Point>() {
                    @Override
                    public void flatMap(RTree<Point> rTree, Collector<Point> collector) throws Exception {
                        List<Integer> partitionFlags = getRuntimeContext().getBroadcastVariable("partitionFlags");
                        if (partitionFlags.contains(getRuntimeContext().getIndexOfThisSubtask())) {
                            List<Point> result = rTree.circleRangeQuery(queryPoint, radius);
                            for (Point point : result) {
                                collector.collect(point);
                            }
                        }
                    }
                })
                .withBroadcastSet(partitionFlags, "partitionFlags");

        return result;
    }

    @Override
    public DataSet<Point> knnQuery(Point queryPoint, int k) {
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

//        DataSet<Point> result = getLocalTrees()
//                .flatMap(new FlatMapFunction<RTree<Point>, Tuple2<Point, Double>>() {
//                    @Override
//                    public void flatMap(RTree<Point> pointRTree, Collector<Tuple2<Point, Double>> collector) throws Exception {
//                        ArrayList<Point> list = pointRTree.getDataObjects();
//                        for (Point point : list) {
//                            collector.collect(new Tuple2<>(point, point.calDistance(queryPoint)));
//                        }
//                    }
//                })
//                .reduceGroup(new RichGroupReduceFunction<Tuple2<Point, Double>, Point>() {
//                    @Override
//                    public void reduce(Iterable<Tuple2<Point, Double>> iterable, Collector<Point> collector) throws Exception {
//                        Iterator<Tuple2<Point, Double>> iter = iterable.iterator();
//                        List<Tuple2<Point, Double>> tupleList = new ArrayList<>();
//                        int count = 0;
//
//                        while (iter.hasNext()) {
//                            tupleList.add(iter.next());
//                        }
//
//                        Collections.sort(tupleList, new Comparator<Tuple2<Point, Double>>() {
//                            @Override
//                            public int compare(Tuple2<Point, Double> o1, Tuple2<Point, Double> o2) {
//                                return o1.f1.compareTo(o2.f1);
//                            }
//                        });
//                        while (count < k) {
//                            collector.collect(tupleList.get(count).f0);
//                            count++;
//                        }
//                    }
//                });

        DataSet<Point> result = getLocalTrees()
                .flatMap(new FlatMapFunction<RTree<Point>, Point>() {
                    @Override
                    public void flatMap(RTree<Point> pointRTree, Collector<Point> collector) throws Exception {
                        List<Point> list = pointRTree.knnQuery(queryPoint, k);
                        for (Point point : list) {
                            collector.collect(point);
                        }
                    }
                })
                .map(new MapFunction<Point, Tuple2<Point, Double>>() {
                    @Override
                    public Tuple2<Point, Double> map(Point point) throws Exception {
                        return new Tuple2<>(point, point.calDistance(queryPoint));
                    }
                })
                .reduceGroup(new RichGroupReduceFunction<Tuple2<Point, Double>, Point>() {
                    @Override
                    public void reduce(Iterable<Tuple2<Point, Double>> iterable, Collector<Point> collector) throws Exception {
                        Iterator<Tuple2<Point, Double>> iter = iterable.iterator();
                        List<Tuple2<Point, Double>> tupleList = new ArrayList<>();
                        int count = 0;

                        while (iter.hasNext()) {
                            tupleList.add(iter.next());
                        }

                        Collections.sort(tupleList, new Comparator<Tuple2<Point, Double>>() {
                            @Override
                            public int compare(Tuple2<Point, Double> o1, Tuple2<Point, Double> o2) {
                                return o1.f1.compareTo(o2.f1);
                            }
                        });
                        while (count < k) {
                            collector.collect(tupleList.get(count).f0);
                            count++;
                        }
                    }
                });

//        DataSet<Point> result = getData()
//                .map(new MapFunction<Point, Tuple2<Point, Double>>() {
//                    @Override
//                    public Tuple2<Point, Double> map(Point point) throws Exception {
//                        return new Tuple2<>(point, point.calDistance(queryPoint));
//                    }
//                })
//                .sortPartition(1, Order.ASCENDING)
//                .mapPartition(new RichMapPartitionFunction<Tuple2<Point, Double>, Tuple2<Point, Double>>() {
//                    @Override
//                    public void mapPartition(Iterable<Tuple2<Point, Double>> iterable, Collector<Tuple2<Point, Double>> collector) throws Exception {
//                        int count = 0;
//                        for (Tuple2<Point, Double> pointDoubleTuple2 : iterable) {
//                            if (count >= k)
//                                break;
//                            collector.collect(pointDoubleTuple2);
//                            count++;
//                        }
//                    }
//                })
//                .reduceGroup(new RichGroupReduceFunction<Tuple2<Point, Double>, Point>() {
//                    @Override
//                    public void reduce(Iterable<Tuple2<Point, Double>> iterable, Collector<Point> collector) throws Exception {
//                        Iterator<Tuple2<Point, Double>> iter = iterable.iterator();
//                        List<Tuple2<Point, Double>> tupleList = new ArrayList<>();
//                        int count = 0;
//
//                        while (iter.hasNext()) {
//                            tupleList.add(iter.next());
//                        }
//
//                        Collections.sort(tupleList, new Comparator<Tuple2<Point, Double>>() {
//                            @Override
//                            public int compare(Tuple2<Point, Double> o1, Tuple2<Point, Double> o2) {
//                                return o1.f1.compareTo(o2.f1);
//                            }
//                        });
//                        while (count < k) {
//                            collector.collect(tupleList.get(count).f0);
//                            count++;
//                        }
//                    }
//                });

        return result;
    }
}
