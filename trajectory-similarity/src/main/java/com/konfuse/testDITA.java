package com.konfuse;

import com.konfuse.dita.*;
import com.konfuse.util.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @Auther todd
 * @Date 2020/4/19
 */
public class testDITA {
    public static ArrayList<DITALocalIndex> localIndices = new ArrayList<>();
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String trajectoriesFolderPath = "E:\\roma_project\\roma_by_half_hour";
        String resultsFolderPath = "E:\\roma_project\\results";
        ArrayList<DITATrajectory> trajectories = DITAFileOperator.readDITATrajectories(trajectoriesFolderPath);
        String localIndexPath = "E:\\roma_project\\index\\dita\\local";
        String globalIndexPath = "E:\\roma_project\\index\\dita\\global";
        System.out.println("*****************Build Global Index*****************");
        DITAGlobalIndex DITAGlobalIndex = new DITAGlobalIndex(DITAConfig.globalPartitionNum);
        ArrayList<Tuple<Integer, LinkedList<DITATrajectory>>> partitions = DITAGlobalIndex.buildGlobalIndex(trajectories);

        try{
            DITAGlobalIndex.save(globalIndexPath + "\\globalIndex");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        ArrayList<DITALocalIndex> localIndices = new ArrayList<>();
        System.out.println("*****************Build Local Index*****************");
        for (int i = 0; i < partitions.size(); i++) {
            System.out.println("Build local index: " + partitions.get(i).f0 + ", " + partitions.get(i).f1.size());
            DITALocalIndex DITALocalIndex = new DITALocalIndex(i, DITAConfig.localIndexedPivotSize, DITAConfig.localMinNodeSize);
            DITALocalIndex.buildLocalIndexDirectly(partitions.get(i).f1);
            localIndices.add(DITALocalIndex);
        }

        System.out.println("*****************Search*****************");
        int allCount = 0;
        ExecutorService pool = Executors.newFixedThreadPool(6);
        for (int i = 100; i < 200; i++) {
            DITATrajectory query = trajectories.get(i);
            long start = System.currentTimeMillis();
            List<Tuple2<Integer, Double>> results = new LinkedList();
            LinkedList<Integer> partitionID = DITAGlobalIndex.search(query, DITAConfig.threshold);
//            System.out.println("Target Partition : " + partitionID.toString());
            List<Future<List<Tuple2<Integer, Double>>>> futures = new ArrayList<>();
            for (Integer id : partitionID) {
                Future<List<Tuple2<Integer, Double>>> future = pool.submit(new Callable<List<Tuple2<Integer, Double>>>() {
                    /**
                     * Computes a result, or throws an exception if unable to do so.
                     *
                     * @return computed result
                     * @throws Exception if unable to compute a result
                     */
                    @Override
                    public List<Tuple2<Integer, Double>> call() throws Exception {
                        return localIndices.get(id).search(query, DITAConfig.threshold);
                    }

                });
                futures.add(future);
//                List<Tuple2<Integer, Double>> searchResults = localIndices.get(id).search(query, DITAConfig.threshold);
//                results.addAll(searchResults);
            }
            for (Future<List<Tuple2<Integer, Double>>> future : futures) {
                results.addAll(future.get());
            }
            long end = System.currentTimeMillis();
            System.out.println(results.toString());
            System.out.println("Search time(ms): " + (end - start));
            allCount += end - start;
        }
        System.out.println(allCount);

        System.out.println("****************************************");
//        DITATrajectory query = trajectories.get(0);
//        long start = System.currentTimeMillis();
//        List<Tuple2<Integer, Double>> results = new LinkedList();
//        LinkedList<Integer> partitionID = DITAGlobalIndex.search(query, DITAConfig.threshold);
//        System.out.println("Target Partition : " + partitionID.toString());
//        for (Integer id : partitionID) {
//            List<Tuple2<Integer, Double>> searchResults = localIndices.get(id).search(query, DITAConfig.threshold);
//            results.addAll(searchResults);
//        }
//        long end = System.currentTimeMillis();
//        System.out.println("Search time(ms): " + (end - start));
//        System.out.println("*****************Print Search results*****************");
//        results.sort((o1, o2) -> new Double(o1.f1).compareTo(o2.f1));
//        DecimalFormat df = new DecimalFormat("#0.000000000000");
//        int count = 0;
//        for (Tuple2<Integer, Double> result : results) {
//            System.out.println(count +"; " +result.f0 + "; " + df.format(result.f1));
//            count++;
//        }
//        System.out.println("*****************Print Real results*****************");
//        ArrayList<Tuple<Integer, Double>> allResults = new ArrayList<>(trajectories.size());
//        long start2 = System.currentTimeMillis();
//        for (DITATrajectory trajectory : trajectories) {
//            allResults.add(new Tuple<>(trajectory.getId(), TrajectoryUtils.calcDTWDistance(trajectory.getTrajectoryData(), query.getTrajectoryData())));
//        }
//        long end2 = System.currentTimeMillis();
//        System.out.println("calc time(ms): " + (end2 - start2));
//        allResults.sort((o1, o2) -> new Double(o1.f1).compareTo(o2.f1));
//        for (int i = 0; i < 50; i++) {
//            System.out.println(i + "; " + allResults.get(i).f0 + "; " + df.format(allResults.get(i).f1));
//        }

    }
}
