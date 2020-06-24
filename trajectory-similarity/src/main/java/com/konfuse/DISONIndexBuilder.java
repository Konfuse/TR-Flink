package com.konfuse;

import com.konfuse.dison.DISONGlobalIndex;
import com.konfuse.dison.DISONLocalIndex;
import com.konfuse.dison.DISONTrajectory;
import com.konfuse.geometry.Point;
import com.konfuse.util.Tuple;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.common.functions.RichGroupReduceFunction;
import org.apache.flink.api.common.functions.RichMapPartitionFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author todd
 * @date 2020/5/24 20:17
 * @description: TODO
 */
public class DISONIndexBuilder {
    public DISONIndexBuilderResult buildIndex(DataSet<DISONTrajectory> data, final DataSet<Integer> globalPartitionNum, final int partitionNum, DataSet<Double> threshold) throws Exception {

        TrajectoryPartitioner partitioner = new TrajectoryPartitioner();

        DataSet<Tuple3<Integer, Integer, DISONTrajectory>> dataWithIndex = data
                .mapPartition(new RichMapPartitionFunction<DISONTrajectory, Tuple3<Integer, Integer, DISONTrajectory>>() {
                    private Integer globalPartitionNum;
                    @Override
                    public void open(Configuration parameters) throws Exception {
                        super.open(parameters);
                        this.globalPartitionNum = (Integer) this.getRuntimeContext().getBroadcastVariable("globalPartitionNum").get(0);
                    }

                    @Override
                    public void mapPartition(Iterable<DISONTrajectory> iterable, Collector<Tuple3<Integer, Integer, DISONTrajectory>> collector) throws Exception {
                        ArrayList<DISONTrajectory> trajectories = new ArrayList<>();
                        Iterator<DISONTrajectory> iter = iterable.iterator();
                        while (iter.hasNext()) {
                            trajectories.add(iter.next());
                        }
                        int firstPartitionSeq = 0;
                        int secondPartitionSeq = 0;
                        LinkedList<LinkedList<DISONTrajectory>> buckets = STRPartition(trajectories, globalPartitionNum, 0);

                        for (LinkedList<DISONTrajectory> bucket : buckets) {
                            LinkedList<LinkedList<DISONTrajectory>> partitions = STRPartition(bucket, globalPartitionNum, 1);
                            for (LinkedList<DISONTrajectory> partition : partitions) {
                                for (DISONTrajectory trajectory : partition) {
                                    collector.collect(new Tuple3<>(firstPartitionSeq, secondPartitionSeq, trajectory));
                                }
                                secondPartitionSeq++;
                            }
                            firstPartitionSeq++;
                        }
                    }

                })
                .withBroadcastSet(globalPartitionNum, "globalPartitionNum");

        DataSet<Tuple3<Integer, Integer, DISONTrajectory>> partitionedDataSet = dataWithIndex
                .partitionCustom(new Partitioner<Integer>() {
                    @Override
                    public int partition(Integer integer, int i) {
                        return integer % i;
                    }
                }, 1)
                .mapPartition(new RichMapPartitionFunction<Tuple3<Integer, Integer, DISONTrajectory>, Tuple3<Integer, Integer, DISONTrajectory>>() {
                    @Override
                    public void mapPartition(Iterable<Tuple3<Integer, Integer, DISONTrajectory>> iterable, Collector<Tuple3<Integer, Integer, DISONTrajectory>> collector) throws Exception {
                        Iterator<Tuple3<Integer, Integer, DISONTrajectory>> iter = iterable.iterator();
                        while (iter.hasNext()) {
                            Tuple3<Integer, Integer, DISONTrajectory> data = iter.next();
                            collector.collect(new Tuple3<>(data.f0, this.getRuntimeContext().getIndexOfThisSubtask(), data.f2));

                        }
                    }
                });

        DataSet<DISONGlobalIndex> globalIndexDataSet = partitionedDataSet
                .reduceGroup(new RichGroupReduceFunction<Tuple3<Integer, Integer, DISONTrajectory>, DISONGlobalIndex>() {
                    private Integer globalPartitionNum;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        super.open(parameters);
                        this.globalPartitionNum = (Integer) this.getRuntimeContext().getBroadcastVariable("globalPartitionNum").get(0);
                    }
                    @Override
                    public void reduce(Iterable<Tuple3<Integer, Integer, DISONTrajectory>> iterable, Collector<DISONGlobalIndex> collector) throws Exception {
                        LinkedList<Tuple3<Integer, Integer, DISONTrajectory>> trajectoriesWithIndex = new LinkedList<>();
                        Iterator<Tuple3<Integer, Integer, DISONTrajectory>> iter = iterable.iterator();
                        while (iter.hasNext()) {
                            trajectoriesWithIndex.add(iter.next());
                        }
                        DISONGlobalIndex globalIndex = new DISONGlobalIndex(globalPartitionNum);
                        globalIndex.buildGlobalIndexDistributed(trajectoriesWithIndex);
                        collector.collect(globalIndex);
                    }
                })
                .withBroadcastSet(globalPartitionNum, "globalPartitionNum");


        DataSet<DISONLocalIndex> localIndexDataSet = partitionedDataSet
                .mapPartition(new RichMapPartitionFunction<Tuple3<Integer, Integer, DISONTrajectory>, DISONLocalIndex>() {
                    private double threshold;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        super.open(parameters);
                        this.threshold = (Double) this.getRuntimeContext().getBroadcastVariable("threshold").get(0);
                    }

                    @Override
                    public void mapPartition(Iterable<Tuple3<Integer, Integer, DISONTrajectory>> iterable, Collector<DISONLocalIndex> collector) throws Exception {
                        DISONLocalIndex localIndex = new DISONLocalIndex(this.getRuntimeContext().getIndexOfThisSubtask());
                        LinkedList<DISONTrajectory> trajectories = new LinkedList<>();
                        Iterator<Tuple3<Integer, Integer, DISONTrajectory>> iter = iterable.iterator();
                        while (iter.hasNext()) {
                            Tuple3<Integer, Integer, DISONTrajectory> trajectoryWithIndex = iter.next();
                            trajectories.add(trajectoryWithIndex.f2);
                        }
                        localIndex.buildLocalIndex(trajectories, threshold);
                        collector.collect(localIndex);
                    }
                }).withBroadcastSet(threshold, "threshold");

        return new DISONIndexBuilderResult(globalIndexDataSet, localIndexDataSet);
//        return new DISONIndexBuilderResult();
    }



    private static LinkedList<LinkedList<DISONTrajectory>> STRPartition(List<DISONTrajectory> trajectories, int globalPartitionNum, int pivotNum) {
        int trajectoriesSize = trajectories.size();
        ArrayList<Tuple<DISONTrajectory, Point>> pivotTuples = new ArrayList<>(trajectoriesSize);

        for (DISONTrajectory trajectory : trajectories) {
            if (pivotNum == 0) {
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
        LinkedList<LinkedList<DISONTrajectory>> buckets = new LinkedList<>();
        int p1 = 0, p2 = 0;
        do {
            int sliceTo = Math.min(p1 + nodeCapacityLon, trajectoriesSize);
            ArrayList<Tuple<DISONTrajectory, Point>> subPoint = new ArrayList<>();
            for (int i = p1; i < sliceTo; i++) {
                subPoint.add(pivotTuples.get(i));
            }
            Long flag = pivotTuples.get(sliceTo - 1).f1.getId();
            while (sliceTo < trajectoriesSize && flag.equals(pivotTuples.get(sliceTo).f1.getId())) {
                subPoint.add(pivotTuples.get(sliceTo));
                sliceTo++;
            }
            int sliceSize = subPoint.size();
            int nodeCapacityLat = (int) Math.ceil(sliceSize * 1.0 / dim[1]);
            p2 = 0;
            do {
                int packTo = Math.min(p2 + nodeCapacityLat, sliceSize);
                subPoint.sort((o1, o2) -> new Double(o1.f1.getY()).compareTo(o2.f1.getY()));
                LinkedList<DISONTrajectory> subBuckets = new LinkedList<>();
                for (int i = p2; i < packTo; i++) {
                    subBuckets.add(subPoint.get(i).f0);
                }
                Long subFlag = subPoint.get(packTo - 1).f1.getId();
                while (packTo < sliceSize && subFlag.equals(subPoint.get(packTo).f1.getId())) {
                    subBuckets.add(subPoint.get(packTo).f0);
                    packTo++;
                }
                buckets.add(subBuckets);
                p2 = packTo;
            } while (p2 < sliceSize);
            p1 = sliceTo;
        } while (p1 < trajectoriesSize);
        return buckets;
    }

    public class TrajectoryPartitioner implements Partitioner<Integer> {
        @Override
        public int partition(Integer key, int partitionNum) {
            return key % partitionNum;
        }
    }

}
