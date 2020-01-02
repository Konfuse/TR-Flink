package com.konfuse;

import com.konfuse.road.GPSPoint;
import com.konfuse.road.Road;
import com.konfuse.road.RoadMap;
import com.konfuse.road.RoadReader;
import com.konfuse.tools.GenerateTestGPSPoint;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * @Author: Konfuse
 * @Date: 2020/1/2 14:52
 */
public class TestRoadMap {
    public static void main(String[] args) throws Exception{
        RoadReader roadReader = new RoadReader();
        roadReader.open();
        RoadMap map = RoadMap.Load(new RoadReader());
        map.construct();
        HashMap<Long, Road> roads = map.getRoads();
//        for(Long id : roads.keySet()){
//            Road road = roads.get(id);
//            LinkedList<Point> points = (LinkedList)road.base().getPoints();
//            System.out.println(id + ": " + road.source() + " " + road.target());
//            System.out.println(id + ": " + points.getFirst() + " " + points.getLast());
//            Iterator<Road> nextRoad = road.successors();
//            System.out.println("************successor************");
//            while (nextRoad.hasNext()) {
//                Road successor = nextRoad.next();
//                System.out.println("->" + successor.id());
//            }
//            System.out.println("*********************************");
//        }

        GenerateTestGPSPoint test = new GenerateTestGPSPoint();
        LinkedList<GPSPoint> testRoads = (LinkedList)test.generateTestGPSPoint(map);
        LinkedList<GPSPoint> testGPSPoint = (LinkedList)test.generateTestCase(testRoads);
        System.out.println("************road***********");
        for(GPSPoint point1 : testRoads){
            Double x1 = new Double(point1.getPosition().getX());
            Double y1 = new Double(point1.getPosition().getY());
            System.out.println(x1 + ";" + y1);
        }
        System.out.println("***************************");
        System.out.println("************test***********");
        for(GPSPoint point2 : testGPSPoint){
            Double x2 = new Double(point2.getPosition().getX());
            Double y2 = new Double(point2.getPosition().getY());
            System.out.println(x2 + ";" + y2);
        }
        System.out.println("***************************");
    }
}
