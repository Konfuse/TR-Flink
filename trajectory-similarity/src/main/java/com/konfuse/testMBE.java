package com.konfuse;

import com.konfuse.mbe.MBEConfig;
import com.konfuse.mbe.MBEIO;
import com.konfuse.mbe.MBEIndex;
import com.konfuse.mbe.MBETrajectory;
import com.konfuse.util.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/4/24
 */
public class testMBE {
    public static void main(String[] args) {
        String trajectoriesFolderPath = "E:\\roma_project\\roma_by_half_hour";
//        String resultsFolderPath = "E:\\roma_project\\results";
//        String indexPath = "E:\\roma_project\\index\\mbe\\index";
        ArrayList<MBETrajectory> trajectories = MBEIO.readMBETrajectories(trajectoriesFolderPath);
        System.out.println("*****************Build Index*****************");
        MBEIndex index = new MBEIndex(trajectories, MBEConfig.splitPercentage);
//        try {
//            index.save(indexPath);
//        } catch (IOException e){
//            e.printStackTrace();
//        }
        System.out.println("*****************Search*****************");
        double accumulatedTime = 0.0;
        for (int i = 100; i < 200; i++) {

            MBETrajectory query = trajectories.get(i);
            long start = System.currentTimeMillis();
            List<Tuple<Integer, Double>> results = index.rangeSearch(query, trajectories, MBEConfig.threshold, MBEConfig.splitPercentage);
            long end = System.currentTimeMillis();
            System.out.println(results.toString());
            System.out.println("Search time(ms): " + i + ":" + (end - start));
            accumulatedTime += end - start;

        }

        System.out.println("100条累积时间(10次): " + accumulatedTime);

//        MBETrajectory query = trajectories.get(10);
//        long start = System.currentTimeMillis();
//        List<Tuple<Integer, Double>> results = index.rangeSearch(query, trajectories, MBEConfig.threshold, MBEConfig.splitPercentage);
//        long end = System.currentTimeMillis();
//        System.out.println("Search time(ms): " + (end - start));
//        results.sort((o1, o2) -> new Double(o1.f1).compareTo(o2.f1));
//        System.out.println("*****************Write results*****************");
//        LinkedList<List<Point>> trajectoriesResults = new LinkedList<>();
//        DecimalFormat df = new DecimalFormat("#0.000000000000");
//        int j = 0;
//        for (Tuple<Integer, Double> result : results) {
//            System.out.println(j +";" +result.f0 + ";" + df.format(result.f1));
//            j++;
//            trajectoriesResults.add(trajectories.get(result.f0).getTrajectoryData());
//        }
//
////        DITAFile.writeDITATrajectory(resultsFolderPath, trajectoriesResults);
//        System.out.println("*****************Real results*****************");
//        ArrayList<Tuple<Integer, Double>> allResults = new ArrayList<>(trajectories.size());
//        long start2 = System.currentTimeMillis();
//        for (MBETrajectory trajectory : trajectories) {
//            allResults.add(new Tuple<>(trajectory.getId(), TrajectoryUtils.calcDTWDistance(trajectory.getTrajectoryData(), query.getTrajectoryData())));
//        }
//        long end2 = System.currentTimeMillis();
//        System.out.println("calc time(ms): " + (end2 - start2));
//        allResults.sort((o1, o2) -> new Double(o1.f1).compareTo(o2.f1));
//        for (int i = 0; i < 100; i++) {
//            System.out.println(i + ";" + allResults.get(i).f0 + ";" + df.format(allResults.get(i).f1));
//        }
    }
}
