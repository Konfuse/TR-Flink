package com.konfuse;

import com.konfuse.geometry.Point;
import com.konfuse.hmm.OfflineMatcher;
import com.konfuse.road.GPSPoint;
import com.konfuse.road.RoadMap;
import com.konfuse.road.RoadPoint;
import com.konfuse.road.RoadReader;
import com.konfuse.spatial.Geography;
import com.konfuse.tools.GenerateTestGPSPoint;

import java.util.List;

/**
 * @Auther todd
 * @Date 2019/12/31
 */
public class TestOfflineMapMatching {
    public static void main(String[] args) throws Exception{
        RoadMap map = RoadMap.Load(new RoadReader());
        map.construct();
//        HashMap<Long, Road> roads = map.getRoads();
        Geography spatial = new Geography();

        GenerateTestGPSPoint test = new GenerateTestGPSPoint();
        List<GPSPoint> testRoads = test.generateTestGPSPoint(map);
        List<GPSPoint> testGPSPoint = test.generateTestCase(testRoads);

        OfflineMatcher offlineMatcher = new OfflineMatcher();
        Long start = System.currentTimeMillis();
        List<RoadPoint> matchedRoadPoints = offlineMatcher.match(testGPSPoint, map, 20);
        Long end = System.currentTimeMillis();
        Long search_time = end - start;
        System.out.println("Search time :" + search_time);


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
        System.out.println("************match***********");
        for (RoadPoint matchedRoadPoint : matchedRoadPoints) {
            Point point = matchedRoadPoint.point();
            System.out.println(point.getX() + ";" + point.getY());
        }
        System.out.println("***************************");
    }

}
