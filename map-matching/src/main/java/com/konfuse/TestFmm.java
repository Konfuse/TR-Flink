package com.konfuse;

import com.konfuse.fmm.FmmMatcher;
import com.konfuse.road.*;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/1/8
 */
public class TestFmm {
    public static long points_search_time = 0L;
    public static long points = 0L;

    public static void main(String[] args) throws Exception{
        long memory = 0;
        String udobtPath = "udobt1.table";

//        new ShapeFileRoadReader("C:\\Users\\Konfuse\\Desktop\\shapefile\\output\\network_dual.shp")
        RoadMap map = RoadMap.Load(new PostgresRoadReader());
        map.construct();

        FmmMatcher fmmMatcher = new FmmMatcher(2);

        System.out.println("read ubodt table.");
        System.gc();
        memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long start = System.currentTimeMillis();
        fmmMatcher.readUDOBTFile(udobtPath, map);
        long end = System.currentTimeMillis();
        long build_time = end - start;
        System.out.println("UBODT read time :" + build_time + "ms");
        System.gc();
        memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - memory;
        System.out.println(Math.max(0, Math.round(memory)) + " bits used for UBODT table (estimate)" );

        testMatch("D:\\SchoolWork\\HUST\\DataBaseGroup\\Roma\\experiment", fmmMatcher, map);
        System.out.println("coordinate convert time: " + RoadMap.convertTime + "ms");
        System.out.println("Total search time: " + points_search_time / 1000.0 + "s");
        System.out.println("Search speed: " + points * 1000.0 / points_search_time + "pt/s");

//        GenerateTestGPSPoint test = new GenerateTestGPSPoint();
//        List<GPSPoint> testRoads = test.generateTestGPSPoint(map);
//        List<GPSPoint> testGPSPoint = test.generateTestCase(testRoads);
//        List<GPSPoint> testGPSPoint = readGPSPoint("199_2014-02-28.txt");

//        List<Road> c_path = fmmMatcher.constructCompletePathOptimized(matchedRoadPoints, map);
//        List<GPSPoint> c_path_gps = fmmMatcher.getCompletePathGPS(c_path);

//        System.out.println("************road***********");
//        test.writeAsTxt(testRoads, "output/road.txt");
//        System.out.println("***************************");

//        System.out.println("************trajectory***********");
//        test.writeAsTxt(testGPSPoint, "output/trajectory.txt");
//        System.out.println("***************************");
//
//        System.out.println("************matched***********");
//        write(matchedRoadPoints, "output/matched.txt");
//        System.out.println("***************************");
//
//        System.out.println("*******complete path*******");
//        test.writeAsTxt(c_path_gps, "output/c_path.txt");
//        System.out.println("***************************");
    }

    public static List<GPSPoint> readGPSPoint(String path) {
        List<GPSPoint> gpsPoints = new ArrayList<>();

        BufferedReader reader;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            reader = new BufferedReader(new FileReader(path));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] items = line.split(";");
                double x = Double.parseDouble(items[0]);
                double y = Double.parseDouble(items[1]);
                long time = simpleDateFormat.parse(items[2]).getTime() / 1000;
                gpsPoints.add(new GPSPoint(time, x, y));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return gpsPoints;
    }

    public static void write(List<RoadPoint> points, String path) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(path));
            for (RoadPoint point : points) {
                double x = point.point().getX();
                double y = point.point().getY();
                System.out.println(x + ";" + y);
                writer.write(x + ";" + y);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void testMatch(String path, FmmMatcher fmmMatcher, RoadMap map) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<GPSPoint> gpsPoints = new ArrayList<>();
        File[] fileList = new File(path).listFiles();
        BufferedReader reader = null;
        long search_time = 0;
        int trajectoryCount = 0, exceptCount = 0;
        long pointCount = 0, currentTrajectoryPointCount;

        for (File file : fileList) {
            currentTrajectoryPointCount = 0;
//            if (trajectoryCount == 1000) {
//                break;
//            }
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
                fmmMatcher.match(gpsPoints, map, 20, 0.002);
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

        points = pointCount;
        points_search_time = search_time;
        System.out.println("trajectories processed: " + trajectoryCount);
        System.out.println("trajectories failed: " + exceptCount);
        System.out.println("trajectory points matched: " + pointCount);
        System.out.println("candidate search time: " + FmmMatcher.candidateSearchTime / 1000.0 + "s");
        System.out.println("viterbi every step time: " + FmmMatcher.viterbiStepTime / 1000.0 + "s");
    }
}
