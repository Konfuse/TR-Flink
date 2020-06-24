package com.konfuse;

import com.konfuse.dison.DISONGlobalIndex;
import com.konfuse.dison.DISONLocalIndex;
import com.konfuse.dison.DISONTrajectory;
import com.konfuse.dita.DITAGlobalIndex;
import com.konfuse.dita.DITALocalIndex;
import com.konfuse.dita.DITATrajectory;
import com.konfuse.road.TopologyRoadMap;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.util.List;

/**
 * @author todd
 * @date 2020/5/24 10:09
 * @description: TODO
 */
public class OperationExecutor {
    public DataSet<Tuple2<Integer, Double>> trajectorySimilarityDITA(DITATrajectory query, DataSet<DITAGlobalIndex> globalIndex, DataSet<DITALocalIndex> localIndex, double threshold) {
        DataSet<Integer> partitionFlags = globalIndex
                .flatMap(new RichFlatMapFunction<DITAGlobalIndex, Integer>() {
//                    private DITATrajectory query;
//                    private double threshold;

//                    @Override
//                    public void open(Configuration parameters) throws Exception {
//                        super.open(parameters);
////                        this.query = (DITATrajectory) this.getRuntimeContext().getBroadcastVariable("query").get(0);
//                        this.threshold = (Double) this.getRuntimeContext().getBroadcastVariable("threshold").get(0);
//                    }
                    @Override
                    public void flatMap(DITAGlobalIndex globalIndexDITA, Collector<Integer> collector) throws Exception {
                        List<Integer> partitionNums = globalIndexDITA.search(query, threshold);
                        for (Integer partitionNum : partitionNums) {
                            collector.collect(partitionNum);
                        }
                    }
                });
//                .withBroadcastSet(query, "query")
//                .withBroadcastSet(threshold, "threshold");

        DataSet<Tuple2<Integer, Double>> results = localIndex
                .flatMap(new RichFlatMapFunction<DITALocalIndex, Tuple2<Integer, Double>>() {
//                    private DITATrajectory query;
//                    private double threshold;
//
//                    @Override
//                    public void open(Configuration parameters) throws Exception {
//                        super.open(parameters);
////                        this.query = (DITATrajectory) this.getRuntimeContext().getBroadcastVariable("query").get(0);
//                        this.threshold = (Double) this.getRuntimeContext().getBroadcastVariable("threshold").get(0);
//                    }

                    @Override
                    public void flatMap(DITALocalIndex localIndexDITA, Collector<Tuple2<Integer, Double>> collector) throws Exception {
                        List<Integer> partitionFlags = getRuntimeContext().getBroadcastVariable("partitionFlags");
                        if (partitionFlags.contains(getRuntimeContext().getIndexOfThisSubtask())) {
                            List<Tuple2<Integer, Double>> results = localIndexDITA.search(query, threshold);
                            for (Tuple2<Integer, Double> result : results) {
                                collector.collect(result);
                            }
                        }
                    }
                })
                .withBroadcastSet(partitionFlags, "partitionFlags");
//                .withBroadcastSet(query, "query")
//                .withBroadcastSet(threshold, "threshold");

        return results;

    }

    public DataSet<Tuple2<Integer, Double>> trajectorySimilarityDSION(DataSet<DISONTrajectory> query, DataSet<DISONGlobalIndex> globalIndex, DataSet<DISONLocalIndex> localIndex, DataSet<TopologyRoadMap> topologyRoadMapToBroadcast, DataSet<Double> threshold) throws Exception {

        DataSet<Integer> partitionFlags = globalIndex
                .flatMap(new RichFlatMapFunction<DISONGlobalIndex, Integer>() {
                    private DISONTrajectory query;
                    private double threshold;
                    TopologyRoadMap topologyRoadMap;
                    @Override
                    public void open(Configuration parameters) throws Exception {
                        super.open(parameters);
                        this.query = (DISONTrajectory) this.getRuntimeContext().getBroadcastVariable("query").get(0);
                        this.threshold = (Double) this.getRuntimeContext().getBroadcastVariable("threshold").get(0);
                        this.topologyRoadMap = (TopologyRoadMap) this.getRuntimeContext().getBroadcastVariable("topologyRoadMapToBroadcast").get(0);
                    }
                    @Override
                    public void flatMap(DISONGlobalIndex globalIndex, Collector<Integer> collector) throws Exception {
//                        System.out.println(topologyRoadMap.getRoadsMap().entrySet());
//                        System.out.println(topologyRoadMap.getRoadsMap().keySet());
//                        System.out.println(topologyRoadMap.getNodesMap().entrySet());
//                        System.out.println();

                        List<Integer> partitionNums = globalIndex.searchGlobalIndex(query, topologyRoadMap.constructNodesMap(), threshold);
                        System.out.println(partitionNums);
                        for (Integer partitionNum : partitionNums) {
                            collector.collect(partitionNum);
                        }
                    }
                })
                .withBroadcastSet(topologyRoadMapToBroadcast, "topologyRoadMapToBroadcast")
                .withBroadcastSet(query, "query")
                .withBroadcastSet(threshold, "threshold");

        DataSet<Tuple2<Integer, Double>> results = localIndex
                .flatMap(new RichFlatMapFunction<DISONLocalIndex, Tuple2<Integer, Double>>() {
                    private DISONTrajectory query;
                    private double threshold;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        super.open(parameters);
                        this.query = (DISONTrajectory) this.getRuntimeContext().getBroadcastVariable("query").get(0);
                        this.threshold = (Double) this.getRuntimeContext().getBroadcastVariable("threshold").get(0);
                    }

                    @Override
                    public void flatMap(DISONLocalIndex localIndexDITA, Collector<Tuple2<Integer, Double>> collector) throws Exception {
                        List<Integer> partitionFlags = getRuntimeContext().getBroadcastVariable("partitionFlags");
                        if (partitionFlags.contains(getRuntimeContext().getIndexOfThisSubtask())) {
                            List<Tuple2<Integer, Double>> results = localIndexDITA.searchLocalIndex(query, threshold);
                            for (Tuple2<Integer, Double> result : results) {
                                collector.collect(result);
                            }
                        }
                    }
                })
                .withBroadcastSet(partitionFlags, "partitionFlags")
                .withBroadcastSet(query, "query")
                .withBroadcastSet(threshold, "threshold");

        return results;

    }
}
