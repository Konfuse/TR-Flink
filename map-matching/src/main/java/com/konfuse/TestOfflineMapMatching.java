package com.konfuse;

import com.konfuse.hmm.OfflineMatcher;
import com.konfuse.road.GPSPoint;
import com.konfuse.road.RoadMap;
import com.konfuse.road.RoadReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2019/12/31
 */
public class TestOfflineMapMatching {
    public static void main(String[] args) throws Exception{
        long memory = 0;

        RoadMap map = RoadMap.Load(new RoadReader());
        map.construct();

        OfflineMatcher offlineMatcher = new OfflineMatcher();
        long search_time = testMatch("D:\\SchoolWork\\HUST\\DataBaseGroup\\Roma\\Roma_by_half_hour", offlineMatcher, map);
        System.out.println("Search time :" + search_time + "ms");

//        HashMap<Long, Road> roads = map.getRoads();
//        Geography spatial = new Geography();

//        GenerateTestGPSPoint test = new GenerateTestGPSPoint();
//        List<GPSPoint> testRoads = test.generateTestGPSPoint(map);
//        List<GPSPoint> testGPSPoint = test.generateTestCase(testRoads);

//        System.out.println("************road***********");
//        for(GPSPoint point1 : testRoads){
//            double x1 = point1.getPosition().getX();
//            double y1 = point1.getPosition().getY();
//            System.out.println(x1 + ";" + y1);
//        }
//        System.out.println("***************************");
//
//        System.out.println("************test***********");
//        for(GPSPoint point2 : testGPSPoint){
//            double x2 = point2.getPosition().getX();
//            double y2 = point2.getPosition().getY();
//            System.out.println(x2 + ";" + y2);
//        }
//        System.out.println("***************************");
//
//        System.out.println("************match***********");
//        for (RoadPoint matchedRoadPoint : matchedRoadPoints) {
//            Point point = matchedRoadPoint.point();
//            System.out.println(point.getX() + ";" + point.getY());
//        }
//        System.out.println("***************************");
    }

    public static long testMatch(String path, OfflineMatcher offlineMatcher, RoadMap map) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<GPSPoint> gpsPoints = new ArrayList<>();
        File[] fileList = new File(path).listFiles();
        BufferedReader reader = null;
        long search_time = 0;
        int trajectoryCount = 0, exceptCount = 0;
        long pointCount = 0, currentTrajectoryPointCount;

        for (File file : fileList) {
            currentTrajectoryPointCount = 0;
            if (trajectoryCount == 1000) {
                break;
            }
            System.out.println("the " + (++trajectoryCount) + "th trajectory is being processed: " + file.getName());
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] items = line.split(";");
                    double x = Double.parseDouble(items[0]);
                    double y = Double.parseDouble(items[1]);
                    long time = simpleDateFormat.parse(items[2]).getTime() / 1000;
                    gpsPoints.add(new GPSPoint(time, x, y));
                    ++currentTrajectoryPointCount;
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }

            try {
                long start = System.currentTimeMillis();
                offlineMatcher.match(gpsPoints, map, 20);
                long end = System.currentTimeMillis();
                search_time += end - start;
                pointCount += currentTrajectoryPointCount;
            } catch (Exception e) {
                e.printStackTrace();
                ++exceptCount;
                System.out.println((trajectoryCount) + "th trajectory failed");

//                if (reader != null) {
//                    try {
//                        reader.close();
//                    } catch (IOException e2) {
//                        e2.printStackTrace();
//                    }
//                }
//
//                try{
//                    if(file.delete()) {
//                        System.out.println(file.getName() + " 文件已被删除！");
//                    } else {
//                        System.out.println("文件删除失败！");
//                    }
//                } catch(Exception e3){
//                    e3.printStackTrace();
//                }
            }
            gpsPoints.clear();
        }
        System.out.println("trajectories processed: " + trajectoryCount);
        System.out.println("trajectories failed: " + exceptCount);
        System.out.println("trajectory points matched: " + pointCount);
        return search_time;
    }
}
