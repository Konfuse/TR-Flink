package com.konfuse;

import com.konfuse.road.GPSPoint;
import com.konfuse.road.Road;
import com.konfuse.road.RoadMap;
import com.konfuse.road.RoadReader;
import com.konfuse.tools.GenerateTestGPSPoint;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @Author: Konfuse
 * @Date: 2020/1/2 14:52
 */
public class TestRoadMap {
    public static void main(String[] args) throws Exception{
        RoadMap map = RoadMap.Load(new RoadReader());
        map.construct();
        HashMap<Long, Road> roads = map.getRoads();
//        for(Long id : roads.keySet()){
//            Road edge = roads.get(id);
//            LinkedList<Point> points = (LinkedList)edge.base().getPoints();
//            System.out.println(id + ": " + edge.source() + " " + edge.target());
//            System.out.println(id + ": " + points.getFirst() + " " + points.getLast());
//            Iterator<Road> nextRoad = edge.successors();
//            System.out.println("************successor************");
//            while (nextRoad.hasNext()) {
//                Road successor = nextRoad.next();
//                System.out.println("->" + successor.id());
//            }
//            System.out.println("*********************************");
//        }

        GenerateTestGPSPoint test = new GenerateTestGPSPoint();
        List<GPSPoint> testRoads = test.generateTestGPSPoint(map);
        List<GPSPoint> testGPSPoint = test.generateTestCase(testRoads);
        System.out.println("************edge***********");
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
    }
}
