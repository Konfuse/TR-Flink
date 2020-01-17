package com.konfuse;

import com.konfuse.fmm.FmmMatcher;
import com.konfuse.road.*;
import com.konfuse.tools.GenerateTestGPSPoint;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/1/8
 */
public class testFmm {
    public static void main(String[] args) throws Exception{
        RoadMap map = RoadMap.Load(new RoadReader());
        map.construct();

        GenerateTestGPSPoint test = new GenerateTestGPSPoint();
        List<GPSPoint> testRoads = test.generateTestGPSPoint(map);
        List<GPSPoint> testGPSPoint = test.generateTestCase(testRoads);

        FmmMatcher fmmMatcher = new FmmMatcher(2);
        fmmMatcher.constructUBODT(map, 3000);

        long start = System.currentTimeMillis();
        List<RoadPoint> matchedRoadPoints = fmmMatcher.match(testGPSPoint, map, 30);
        long end = System.currentTimeMillis();
        long search_time = end - start;
        System.out.println("Search time :" + search_time);

        List<Road> c_path = fmmMatcher.constructCompletePathOptimized(matchedRoadPoints,  map);
        List<GPSPoint> c_path_gps = fmmMatcher.getCompletePathGPS(c_path);

        System.out.println("************road***********");
        test.writeAsTxt(testRoads, "output/road.txt");

        System.out.println("***************************");
        System.out.println("************trajectory***********");
        test.writeAsTxt(testGPSPoint, "output/trajectory.txt");

        System.out.println("***************************");
        System.out.println("************matched***********");
        write(matchedRoadPoints, "output/matched.txt");

        System.out.println("***************************");
        System.out.println("*******complete path*******");

        test.writeAsTxt(c_path_gps, "output/c_path.txt");
        System.out.println("***************************");
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
}
