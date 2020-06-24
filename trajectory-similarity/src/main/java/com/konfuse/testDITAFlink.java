package com.konfuse;

import com.konfuse.dita.*;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.ArrayList;

/**
 * @author todd
 * @date 2020/5/31 12:08
 * @description: TODO
 */
public class testDITAFlink {
    public static void main(String[] args) throws Exception {
        int globalPartitionNum = DITAConfig.globalPartitionNum;
        int localIndexedPivotSize = DITAConfig.localIndexedPivotSize;
        int localMinNodeSize = DITAConfig.localMinNodeSize;
        double threshold = DITAConfig.threshold;

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(globalPartitionNum * globalPartitionNum);
        String trajectoriesFolderPath = "E:\\roma_project\\roma_by_half_hour";
        ArrayList<DITATrajectory> trajectories = DITAFileOperator.readDITATrajectories(trajectoriesFolderPath);
        DataSet<DITATrajectory> data = env.fromCollection(trajectories);
        DataSet<Integer> globalPartitionNumBroadcast = env.fromElements(globalPartitionNum);
        DataSet<Integer> localIndexedPivotSizeBroadcast = env.fromElements(localIndexedPivotSize);
        DataSet<Integer> localMinNodeSizeBroadcast = env.fromElements(localMinNodeSize);

        DITATrajectory query = trajectories.get(0);

        DITAIndexBuilder builder = new DITAIndexBuilder();
        DITAIndexBuilderResult index = builder.buildIndex(data, globalPartitionNumBroadcast, localIndexedPivotSizeBroadcast, localMinNodeSizeBroadcast, globalPartitionNum * globalPartitionNum);
        OperationExecutor operationExecutor = new OperationExecutor();
        long start = System.currentTimeMillis();
        DataSet<Tuple2<Integer, Double>> result = operationExecutor.trajectorySimilarityDITA(query, index.getGlobalIndex(), index.getLocalIndex(), threshold);
        long end = System.currentTimeMillis();
        result.print();

        System.out.println("search time: " + (end - start) + "ms");


    }
}
