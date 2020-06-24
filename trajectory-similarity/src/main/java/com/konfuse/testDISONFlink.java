package com.konfuse;

import com.konfuse.dison.DISONConfig;
import com.konfuse.dison.DISONFileOperator;
import com.konfuse.dison.DISONTrajectory;
import com.konfuse.road.RoadMap;
import com.konfuse.road.ShapeFileRoadReader;
import com.konfuse.road.TopologyRoadMap;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.ArrayList;

/**
 * @author todd
 * @date 2020/5/31 14:05
 * @description: TODO
 */
public class testDISONFlink {
    public static void main(String[] args) throws Exception {
        int globalPartitionNum = DISONConfig.globalPartitionNum;
        double threshold = DISONConfig.threshold;

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(globalPartitionNum * globalPartitionNum);
//        Utils.registerTypeWithKryoSerializer(env);
//        Utils.registerCustomSerializer(env);
        String trajectoriesFolderPath = "E:\\roma_project\\matchedResults";
        RoadMap map = RoadMap.Load(new ShapeFileRoadReader("E:\\roma_project\\output\\output\\network_dual.shp"));
        map.construct();

        ArrayList<DISONTrajectory> trajectories = DISONFileOperator.readDisonTrajectories(trajectoriesFolderPath, map);
        DataSet<DISONTrajectory> data = env.fromCollection(trajectories);
        DataSet<Integer> globalPartitionNumBroadcast = env.fromElements(globalPartitionNum);
        DataSet<Double> thresholdToBroadcast = env.fromElements(threshold);
        TopologyRoadMap topologyRoadMap = new TopologyRoadMap(map.getEdges(), map.getNodes());
        DataSet<TopologyRoadMap> topologyRoadMapToBroadcast = env.fromElements(topologyRoadMap);
        System.out.println("*****************Build Index*****************");
        DISONIndexBuilder builder = new DISONIndexBuilder();
        DISONIndexBuilderResult index = builder.buildIndex(data, globalPartitionNumBroadcast, globalPartitionNum * globalPartitionNum, thresholdToBroadcast);
        OperationExecutor operationExecutor = new OperationExecutor();
        System.out.println("*****************Search*****************");
        DISONTrajectory query = trajectories.get(3);
        DataSet<DISONTrajectory> queryData = env.fromElements(query);

        long start = System.currentTimeMillis();
        DataSet<Tuple2<Integer, Double>> result = operationExecutor.trajectorySimilarityDSION(queryData, index.getGlobalIndexDataSet(), index.getLocalIndexDataSet(), topologyRoadMapToBroadcast, thresholdToBroadcast);
        long end = System.currentTimeMillis();
        result.print();
        System.out.println("search time: " + (end - start) + "ms");

    }
}
