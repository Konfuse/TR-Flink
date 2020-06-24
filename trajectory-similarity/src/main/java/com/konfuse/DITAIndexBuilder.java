package com.konfuse;

import com.konfuse.dita.DITAGlobalIndex;
import com.konfuse.dita.DITALocalIndex;
import com.konfuse.dita.DITATrajectory;
import com.konfuse.geometry.Point;
import com.konfuse.geometry.Rectangle;
import com.konfuse.strtree.IndexBuilder;
import com.konfuse.strtree.RTree;
import com.konfuse.util.Tuple;
import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author todd
 * @date 2020/5/22 8:23
 * @description: TODO
 */

public class DITAIndexBuilder implements Serializable {

    // the idea of index builder is
    // Step 1: Partition data
    // Step 2: Build up global tree index
    // Step 3: build local index by map partition

    public DITAIndexBuilderResult buildIndex(DataSet<DITATrajectory> data, final DataSet<Integer> globalPartitionNum, final DataSet<Integer> localIndexedPivotSize, final DataSet<Integer> localMinNodeSize, final int partitionNum) throws Exception {
        DataSet<Tuple2<Integer, DITATrajectory>> trajectoryWithIndex = data
                .filter(new FilterFunction<DITATrajectory>() {
                    @Override
                    public boolean filter(DITATrajectory ditaTrajectory) throws Exception {
                        return ditaTrajectory.getNum() >= 10 && ditaTrajectory.getNum() <= 1000;
                    }
                })
                .mapPartition(new RichMapPartitionFunction<DITATrajectory, Tuple2<Integer, DITATrajectory>>() {
                    private Integer globalPartitionNum;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        super.open(parameters);
                        this.globalPartitionNum = (Integer) this.getRuntimeContext().getBroadcastVariable("globalPartitionNum").get(0);
                    }

                    @Override
                    public void mapPartition(Iterable<DITATrajectory> iterable, Collector<Tuple2<Integer, DITATrajectory>> collector) throws Exception {
                        ArrayList<DITATrajectory> trajectories = new ArrayList<>();
                        Iterator<DITATrajectory> iter = iterable.iterator();
                        while (iter.hasNext()) {
                            trajectories.add(iter.next());
                        }
                        int numPartition = 0;
                        LinkedList<LinkedList<DITATrajectory>> buckets = STRPartition(trajectories, globalPartitionNum, 0);
                        for (LinkedList<DITATrajectory> bucket : buckets) {
                            System.out.println("Bucket size: " + bucket.size());
                            LinkedList<LinkedList<DITATrajectory>> subBuckets = STRPartition(bucket, globalPartitionNum, 1);
                            System.out.println("subBucket num: " + subBuckets.size());
                            for (LinkedList<DITATrajectory> subBucket : subBuckets) {
                                for (DITATrajectory ditaTrajectory : subBucket) {
                                    collector.collect(new Tuple2<>(numPartition, ditaTrajectory));
                                }
                                numPartition++;
                                System.out.println("subBucket size: " + subBucket.size());
                            }
                        }

                    }
                }).withBroadcastSet(globalPartitionNum, "globalPartitionNum");


        TrajectoryPartitioner partitioner = new TrajectoryPartitioner();

        DataSet<DITATrajectory> partitionedData = trajectoryWithIndex
                .partitionCustom(partitioner, new KeySelector<Tuple2<Integer, DITATrajectory>, Integer>() {
                    @Override
                    public Integer getKey(Tuple2<Integer, DITATrajectory> trajectoryWithIndex) throws Exception {
                        return trajectoryWithIndex.f0;
                    }
                })
                .mapPartition(new MapPartitionFunction<Tuple2<Integer, DITATrajectory>, DITATrajectory>() {
                    @Override
                    public void mapPartition(Iterable<Tuple2<Integer, DITATrajectory>> iterable, Collector<DITATrajectory> collector) throws Exception {
                        for (Tuple2<Integer, DITATrajectory> trajectoryWithIndex : iterable) {
                            collector.collect(trajectoryWithIndex.f1);
                        }
                    }
                });

        DataSet<DITAGlobalIndex> globalIndex = partitionedData
                .mapPartition(new RichMapPartitionFunction<DITATrajectory, Tuple2<Rectangle, Rectangle>>() {
                    @Override
                    public void mapPartition(Iterable<DITATrajectory> iterable, Collector<Tuple2<Rectangle, Rectangle>> collector) throws Exception {
                        LinkedList<DITATrajectory> trajectories = new LinkedList<>();
                        Iterator<DITATrajectory> iter = iterable.iterator();
                        while (iter.hasNext()) {
                            trajectories.add(iter.next());
                        }
                        Rectangle[] rectangles = calcPartitionBound(trajectories, this.getRuntimeContext().getIndexOfThisSubtask());
                        collector.collect(new Tuple2<>(rectangles[0], rectangles[1]));
                    }
                })
                .reduceGroup(new RichGroupReduceFunction<Tuple2<Rectangle, Rectangle>, DITAGlobalIndex>() {
                    @Override
                    public void reduce(Iterable<Tuple2<Rectangle, Rectangle>> iterable, Collector<DITAGlobalIndex> collector) throws Exception {
                        ArrayList<Rectangle> firstPointRectangles = new ArrayList<>();
                        ArrayList<Rectangle> lastPointRectangles = new ArrayList<>();
                        int globalPartitionNum = 0;
                        Iterator<Tuple2<Rectangle, Rectangle>> iter = iterable.iterator();
                        while (iter.hasNext()) {
                            firstPointRectangles.add(iter.next().f0);
                            lastPointRectangles.add(iter.next().f1);
                            globalPartitionNum++;
                        }
                        RTree STRtreeFirst = new IndexBuilder().createRTreeBySTR(firstPointRectangles.toArray(new Rectangle[firstPointRectangles.size()]));
                        RTree STRtreeLast = new IndexBuilder().createRTreeBySTR(lastPointRectangles.toArray(new Rectangle[lastPointRectangles.size()]));
                        collector.collect(new DITAGlobalIndex(globalPartitionNum, STRtreeFirst, STRtreeLast));
                    }
                });



        DataSet<DITALocalIndex> localIndex = partitionedData
                .mapPartition(new RichMapPartitionFunction<DITATrajectory, DITALocalIndex>() {
                    private int localIndexedPivotSize;
                    private int localMinNodeSize;
                    @Override
                    public void open(Configuration parameters) throws Exception {
                        super.open(parameters);
                        this.localIndexedPivotSize = (Integer) this.getRuntimeContext().getBroadcastVariable("localIndexedPivotSize").get(0);
                        this.localMinNodeSize = (Integer) this.getRuntimeContext().getBroadcastVariable("localMinNodeSize").get(0);
                    }
                    @Override
                    public void mapPartition(Iterable<DITATrajectory> iterable, Collector<DITALocalIndex> collector) throws Exception {
                        LinkedList<DITATrajectory> trajectories = new LinkedList<>();
                        Iterator<DITATrajectory> iter = iterable.iterator();
                        while (iter.hasNext()) {
                            trajectories.add(iter.next());
                        }
                        DITALocalIndex index = new DITALocalIndex(this.getRuntimeContext().getIndexOfThisSubtask(), localIndexedPivotSize, localMinNodeSize);
                        index.buildLocalIndexDirectly(trajectories);
                        collector.collect(index);
                    }
                })
                .withBroadcastSet(localIndexedPivotSize, "localIndexedPivotSize")
                .withBroadcastSet(localMinNodeSize, "localMinNodeSize");

        return new DITAIndexBuilderResult(globalIndex, localIndex, partitionedData);
    }

    public class TrajectoryPartitioner implements Partitioner<Integer> {
        @Override
        public int partition(Integer key, int i) {
            return key % i;
        }
    }

    private LinkedList<LinkedList<DITATrajectory>> STRPartition(List<DITATrajectory> trajectories, int globalPartitionNum, int globalPivotNum) {
        int trajectoriesSize = trajectories.size();
        ArrayList<Tuple<DITATrajectory, Point>> pivotTuples = new ArrayList<>(trajectoriesSize);
        for (DITATrajectory trajectory : trajectories) {
            if (globalPivotNum == 0) {
                pivotTuples.add(new Tuple<>(trajectory, trajectory.getGlobalIndexedPivot().get(0)));
            } else {
                pivotTuples.add(new Tuple<>(trajectory, trajectory.getGlobalIndexedPivot().get(1)));
            }

        }
        int[] dim = new int[2];
        dim[0] = (int) Math.ceil(Math.sqrt(globalPartitionNum));
        dim[1] = globalPartitionNum / dim[0];
        int nodeCapacityLon = (int) Math.ceil(trajectoriesSize * 1.0 / dim[0]);
        pivotTuples.sort((o1, o2) -> new Double(o1.f1.getX()).compareTo(o2.f1.getX()));
        LinkedList<LinkedList<DITATrajectory>> buckets = new LinkedList<>();
        int p1 = 0, p2 = 0;
        do {
            int sliceTo = Math.min(p1 + nodeCapacityLon, trajectoriesSize);
            ArrayList<Tuple<DITATrajectory, Point>> subFirstPoint = new ArrayList<>();
            for (int i = p1; i < sliceTo; i++) {
                subFirstPoint.add(pivotTuples.get(i));
            }
            int sliceSize = subFirstPoint.size();
            int nodeCapacityLat = (int) Math.ceil(sliceSize * 1.0 / dim[1]);
            p2 = 0;
            do {
                int packTo = Math.min(p2 + nodeCapacityLat, sliceSize);
                subFirstPoint.sort((o1, o2) -> new Double(o1.f1.getY()).compareTo(o2.f1.getY()));
                LinkedList<DITATrajectory> subBuckets = new LinkedList<>();
                for (int i = p2; i < packTo; i++) {
                    subBuckets.add(subFirstPoint.get(i).f0);
                }
                buckets.add(subBuckets);
                p2 = packTo;
            } while (p2 < sliceSize);
            p1 = sliceTo;
        } while (p1 < trajectoriesSize);
        return buckets;
    }

    private Rectangle[] calcPartitionBound(List<DITATrajectory> trajectories, int id) {
        double firstPointMinLon = 180, firstPointMinLat = 90, firstPointMaxLon = -180, firstPointMaxLat = -90;
        double lastPointMinLon = 180, lastPointMinLat = 90, lastPointMaxLon = -180, lastPointMaxLat = -90;
        for (DITATrajectory trajectory : trajectories) {
            double firstPointLon = trajectory.getGlobalIndexedPivot().get(0).getX();
            double firstPointLat = trajectory.getGlobalIndexedPivot().get(0).getY();
            double lastPointLon = trajectory.getGlobalIndexedPivot().get(1).getX();
            double lastPointLat = trajectory.getGlobalIndexedPivot().get(1).getY();
            firstPointMinLon = Math.min(firstPointMinLon, firstPointLon);
            firstPointMinLat = Math.min(firstPointMinLat, firstPointLat);

            lastPointMinLon = Math.min(lastPointMinLon, lastPointLon);
            lastPointMinLat = Math.min(lastPointMinLat, lastPointLat);

            firstPointMaxLon = Math.max(firstPointMaxLon, firstPointLon);
            firstPointMaxLat = Math.max(firstPointMaxLat, firstPointLat);

            lastPointMaxLon = Math.max(lastPointMaxLon, firstPointLon);
            lastPointMaxLat = Math.max(lastPointMaxLat, firstPointLat);
        }

        Rectangle rectFirst = new Rectangle(id, firstPointMinLon, firstPointMinLat, firstPointMaxLon, firstPointMaxLat);
        Rectangle rectLast = new Rectangle(id, lastPointMinLon, lastPointMinLat, lastPointMaxLon, lastPointMaxLat);
        return new Rectangle[]{rectFirst, rectLast};
    }

}
