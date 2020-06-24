package com.konfuse;

import com.konfuse.dison.*;
import com.konfuse.road.RoadMap;
import com.konfuse.road.ShapeFileRoadReader;
import com.konfuse.util.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @Auther todd
 * @Date 2020/4/21
 */
public class testDISON {

    public static void main(String[] args) throws Exception {

        RoadMap map = RoadMap.Load(new ShapeFileRoadReader("E:\\roma_project\\output\\output\\network_dual.shp"));
        map.construct();

        String trajectoriesFolderPath = "E:\\roma_project\\matchedResults";

        ArrayList<DISONTrajectory> trajectories = DISONFileOperator.readDisonTrajectories(trajectoriesFolderPath, map);
        System.out.println("*****************Build Global Index*****************");
        DISONGlobalIndex globalIndex = new DISONGlobalIndex(DISONConfig.globalPartitionNum);
        ArrayList<Tuple<Integer, LinkedList<DISONTrajectory>>> partitions = globalIndex.buildGlobalIndex(trajectories);

        System.out.println("*****************Build Local Index*****************");
        ArrayList<DISONLocalIndex> localIndices = new ArrayList<>();

        for (int i = 0; i < partitions.size(); i++) {
            DISONLocalIndex DISONLocalIndex = new DISONLocalIndex(partitions.get(i).f0);
            DISONLocalIndex.buildLocalIndex(partitions.get(i).f1, DISONConfig.threshold);
            localIndices.add(DISONLocalIndex);
        }

        System.out.println("*****************Search*****************");

        ExecutorService pool = Executors.newFixedThreadPool(6);

        int allTime = 0;
        for (int i = 100; i < 200; i++) {
            DISONTrajectory query = trajectories.get(i);
            long start1 = System.currentTimeMillis();
            List<Integer> partitionID = globalIndex.searchGlobalIndex(query, map.getNodes(), DISONConfig.threshold);
            LinkedList<Tuple2<Integer, Double>> results = new LinkedList<>();
            long end1 = System.currentTimeMillis();

            long start2 = System.currentTimeMillis();
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
                        return localIndices.get(id).searchLocalIndex(query, DISONConfig.threshold);
                    }

                });
                futures.add(future);
//                List<Tuple2<Integer, Double>> ans = localIndices.get(id).searchLocalIndex(query, DISONConfig.threshold);
//                results.addAll(ans);
            }

            for (Future<List<Tuple2<Integer, Double>>> future : futures) {
                results.addAll(future.get());
            }
            long end2 = System.currentTimeMillis();
//            System.out.println(partitionID.toString());
            System.out.println(results.toString());
            System.out.println("Global Search time(ms) :" + (end1 - start1));
            System.out.println("Local Search time(ms) :" + (end2 - start2));
            allTime += end2 - start1;
        }

        System.out.println(allTime);
        pool.shutdown();

//        System.gc();

//        System.out.println("*****************Print Search*****************");
//        List<Tuple2<Integer, Double>> realResults = new LinkedList<>();
//        for (Tuple2<Integer, Double> result : results) {
//            realResults.add(result);
//        }
//        realResults.sort((o1, o2) -> new Double(o2.f1).compareTo(o1.f1));
//
//        int j = 0;
//        DecimalFormat df = new DecimalFormat("#0.000000000000");
//        for (Tuple2<Integer, Double> result : realResults) {
//            System.out.println(j +"; " +result.f0 + "; " + df.format(result.f1));
//            j++;
////            trajectoriesResults.add(trajectories.get(result.f0).getTrajectoryData());
//        }
//        System.out.println("*****************Real results*****************");
//        ArrayList<Tuple<Integer, Double>> allResults = new ArrayList<>(trajectories.size());
//        long start2 = System.currentTimeMillis();
//        for (DISONTrajectory trajectory : trajectories) {
//            allResults.add(new Tuple<>(trajectory.getId(), TrajectoryUtils.calcLCRSDistance(trajectory, query)));
//        }
//        long end2 = System.currentTimeMillis();
//        System.out.println("calc time(ms): " + (end2 - start2));
//        allResults.sort((o1, o2) -> new Double(o2.f1).compareTo(o1.f1));
//        for (int i = 0; i < 10; i++) {
//            System.out.println(i + "; " + allResults.get(i).f0 + "; " + df.format(allResults.get(i).f1));
//        }
    }

}
