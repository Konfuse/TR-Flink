package com.konfuse;

import com.konfuse.dita.*;
import com.konfuse.geometry.Point;
import com.konfuse.util.TrajectoryUtils;
import com.konfuse.util.Tuple;

import java.text.DecimalFormat;
import java.util.*;

/**
 * @Auther todd
 * @Date 2020/4/19
 */
public class testDITA {
    public static void main(String[] args) {
        String trajectoriesFolderPath = "E:\\roma_project\\roma_by_half_hour";
        String resultsFolderPath = "E:\\roma_project\\results";
        ArrayList<DITATrajectory> trajectories = DITAFile.readDITATrajectories(trajectoriesFolderPath);
        System.out.println("*****************Build Global Index*****************");
        GlobalIndex globalIndex = new GlobalIndex(DITAConfig.globalPartitionNum);
        globalIndex.buildGlobalIndex(trajectories);
        ArrayList<LocalIndex> localIndices = new ArrayList<>();
        System.out.println("*****************Build Local Index*****************");
        for (int i = 0; i < globalIndex.getGlobalPartitions().size(); i++) {
            System.out.println("Build local index: " + i + ", " + globalIndex.getGlobalPartitions().get(i).getTrajectorySeqNums().size());
            LocalIndex localIndex = new LocalIndex(i, DITAConfig.localIndexedPivotSize, DITAConfig.localMinNodeSize);
            localIndex.buildLocalIndex(trajectories, globalIndex.getGlobalPartitions().get(i).getTrajectorySeqNums());
            localIndices.add(localIndex);
        }
        System.out.println("*****************Search*****************");
        DITATrajectory query = trajectories.get(21374);
        long start = System.currentTimeMillis();
        List<Tuple<Integer, Double>> results = new LinkedList();
        LinkedList<Integer> partitionID = globalIndex.search(query, DITAConfig.threshold);
        for (Integer id : partitionID) {
            List<Tuple<Integer, Double>> searchResults = localIndices.get(id).search(trajectories, query, DITAConfig.threshold);
            results.addAll(searchResults);
        }
        long end = System.currentTimeMillis();
        System.out.println("Search time(ms): " + (end - start));
        results.sort((o1, o2) -> new Double(o1.f1).compareTo(o2.f1));
        System.out.println("*****************Write results*****************");
        LinkedList<List<Point>> trajectoriesResults = new LinkedList<>();
        DecimalFormat df = new DecimalFormat("#0.000000000000");
        int j = 0;
        for (Tuple<Integer, Double> result : results) {
            System.out.println(j +";" +result.f0 + ";" + df.format(result.f1));
            j++;
            trajectoriesResults.add(trajectories.get(result.f0).getTrajectoryData());
        }

//        DITAFile.writeDITATrajectory(resultsFolderPath, trajectoriesResults);
        System.out.println("*****************Real results*****************");
        ArrayList<Tuple<Integer, Double>> allResults = new ArrayList<>(trajectories.size());
        long start2 = System.currentTimeMillis();
        for (DITATrajectory trajectory : trajectories) {
            allResults.add(new Tuple<>(trajectory.getId(), TrajectoryUtils.calcDTWDistance(trajectory, query)));
        }
        long end2 = System.currentTimeMillis();
        System.out.println("calc time(ms): " + (end2 - start2));
        allResults.sort((o1, o2) -> new Double(o1.f1).compareTo(o2.f1));
        for (int i = 0; i < 100; i++) {
            System.out.println(i + ";" + allResults.get(i).f0 + ";" + df.format(allResults.get(i).f1));
        }
    }
}
