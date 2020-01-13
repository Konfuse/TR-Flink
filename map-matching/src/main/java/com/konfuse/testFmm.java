package com.konfuse;

import com.konfuse.fmm.FmmMatcher;
import com.konfuse.geometry.Point;
import com.konfuse.road.*;
import com.konfuse.spatial.Geography;
import com.konfuse.tools.GenerateTestGPSPoint;

import java.util.List;

/**
 * @Auther todd
 * @Date 2020/1/8
 */
public class testFmm {
    public static void main(String[] args) throws Exception{
        RoadMap map = RoadMap.Load(new RoadReader());
        map.construct();

        Geography spatial = new Geography();

        GenerateTestGPSPoint test = new GenerateTestGPSPoint();
        List<GPSPoint> testRoads = test.generateTestGPSPoint(map);
        List<GPSPoint> testGPSPoint = test.generateTestCase(testRoads);
        FmmMatcher fmmMatcher = new FmmMatcher(456101, 100000, 0.3);
        fmmMatcher.constructUBODT(map, 1000);

        Long start = System.currentTimeMillis();
        List<RoadPoint> matchedRoadPoints = fmmMatcher.match(testGPSPoint, map, 30);
        Long end = System.currentTimeMillis();
        Long search_time = end - start;
        System.out.println("Search time :" + search_time);
        List<Road> c_path = fmmMatcher.constructCompletePathOptimized(matchedRoadPoints,  map);
        List<GPSPoint> c_path_gps = fmmMatcher.getCompletePathGPS(c_path);

        System.out.println("************road***********");
        for(GPSPoint point1 : testRoads){
            double x1 = point1.getPosition().getX();
            double y1 = point1.getPosition().getY();
            System.out.println(x1 + ";" + y1);
        }
        System.out.println("***************************");
        System.out.println("************test***********");
        for(GPSPoint point2 : testGPSPoint){
            double x2 = point2.getPosition().getX();
            double y2 = point2.getPosition().getY();
            System.out.println(x2 + ";" + y2);
        }
        System.out.println("***************************");
        System.out.println("************matched***********");
        for (RoadPoint matchedRoadPoint : matchedRoadPoints) {
            Point point = matchedRoadPoint.point();
            System.out.println(point.getX() + ";" + point.getY());
        }
        System.out.println("***************************");
        System.out.println("*******complete path*******");

        for(GPSPoint point3 : c_path_gps){
            double x3 = point3.getPosition().getX();
            double y3 = point3.getPosition().getY();
            System.out.println(x3 + ";" + y3);
        }
        System.out.println("***************************");
    }
}
