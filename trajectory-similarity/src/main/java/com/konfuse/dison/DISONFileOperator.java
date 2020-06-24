package com.konfuse.dison;

import com.konfuse.geometry.Point;
import com.konfuse.road.Road;
import com.konfuse.road.RoadMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/4/28
 */
public class DISONFileOperator {
    public static ArrayList<DISONTrajectory> readDisonTrajectories(String trajectoriesFolderPath, RoadMap map) {
        ArrayList<DISONTrajectory> records = new ArrayList<>();
        File[] fileList = new File(trajectoriesFolderPath).listFiles();
        BufferedReader reader = null;
        int trajectoryCount = 0, id = 0;
        for (File file : fileList) {
            ArrayList<Long> roads = new ArrayList<>();
            if (trajectoryCount == 30000) {
                break;
            }
            ++trajectoryCount;
//            System.out.println("the " + (++trajectoryCount) + "th trajectory is being processed: " + file.getName());

            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] items = line.split(";");
                    long x = Long.parseLong(items[0]);
                    roads.add(x);
                }

                List<DISONEdge> disonEdges = new ArrayList<>();

                for (Long roadId : roads) {
                    Road road = map.getEdges().get(roadId);
                    List<Point> pointList = road.getPoints();
                    Point start = new Point(road.source(), pointList.get(0).getX(), pointList.get(0).getY());
                    Point end = new Point(road.target(), pointList.get(pointList.size() - 1).getX(), pointList.get(pointList.size() - 1).getY());
                    disonEdges.add(new DISONEdge(roadId, start, end, road.length()));
                }

                if (roads.size() >= 10) {
                    DISONTrajectory trajectory = new DISONTrajectory(id);
                    trajectory.setTrajectoryData(disonEdges);
                    records.add(trajectory);
                    ++id;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("trajectory count: " + records.size());
        return records;
    }
}
