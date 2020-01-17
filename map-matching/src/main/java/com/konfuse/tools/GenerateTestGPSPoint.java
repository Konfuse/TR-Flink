package com.konfuse.tools;

import com.konfuse.geometry.Point;
import com.konfuse.road.*;
import com.konfuse.topology.Cost;
import com.konfuse.topology.Dijkstra;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * @Author: todd
 * @Date: 2020/1/2
 */
public class GenerateTestGPSPoint {
    public List<GPSPoint> generateTestGPSPoint(RoadMap map){
        HashMap<Long, Road> roads = map.getEdges();
        Long[] keys = roads.keySet().toArray(new Long[0]);
        Dijkstra<Road, RoadPoint> dijkstra = new Dijkstra<>();
        Cost<Road> cost = new DistanceCost();
        List<Road> path;

        do {
            Random random = new Random();
            Long sourceKey = keys[random.nextInt(keys.length)];
            Long targetKey = keys[random.nextInt(keys.length)];
            Road sourceRoad = roads.get(sourceKey);
            Road targetRoad = roads.get(targetKey);
            System.out.println(sourceRoad.source() + "->" + targetRoad.target());
            System.out.println(sourceRoad.id() + "->" + targetRoad.id());
            RoadPoint sourceLocation = new RoadPoint(sourceRoad, 0);
            RoadPoint targetLocation = new RoadPoint(targetRoad, 1);
            path = dijkstra.route(sourceLocation, targetLocation, cost);
        } while(path == null);

        long count = 0;
        List<GPSPoint> testRoads = new LinkedList<>();
        for(Road road : path){
            testRoads.add(new GPSPoint(count++, road.geometry().getPoint(0).getX(), road.geometry().getPoint(0).getY()));
        }
        int pointCount = path.get(path.size() - 1).geometry().getPointCount();
        testRoads.add(new GPSPoint(count, path.get(path.size() - 1).geometry().getPoint(pointCount - 1).getX(), path.get(path.size() - 1).geometry().getPoint(pointCount - 1).getY()));
        return testRoads;
    }

    public List<GPSPoint> generateTestGPSPointbyLine(RoadMap map){
        HashMap<Long, Road> roads = map.getEdges();
        Long[] keys = roads.keySet().toArray(new Long[0]);
        Dijkstra<Road, RoadPoint> dijkstra = new Dijkstra<>();
        Cost<Road> cost = new DistanceCost();
        List<Road> path;

        do {
            Random random = new Random();
            Long sourceKey = keys[random.nextInt(keys.length)];
            Long targetKey = keys[random.nextInt(keys.length)];
            Road sourceRoad = roads.get(sourceKey);
            Road targetRoad = roads.get(targetKey);
            System.out.println(sourceRoad.source() + "->" + targetRoad.target());
            System.out.println(sourceRoad.id() + "->" + targetRoad.id());
            RoadPoint sourceLocation = new RoadPoint(sourceRoad, 0);
            RoadPoint targetLocation = new RoadPoint(targetRoad, 1);
            path = dijkstra.route(sourceLocation, targetLocation, cost);
        } while(path == null);

        long count = 0;
        List<GPSPoint> testRoads = new LinkedList<>();
        for(Road road : path){
            List<Point>  points = road.getPoints();
            if(count == 0){
                points = points.subList(1, points.size() - 1);
                for (Point point : points) {
                    testRoads.add(new GPSPoint(count++, point.getX(), point.getY()));
                }
            }
            else{
                for (Point point : points) {
                    testRoads.add(new GPSPoint(count++, point.getX(), point.getY()));
                }
            }
        }

        return testRoads;
    }

    public List<GPSPoint> generateTestCase(List<GPSPoint> testRoads) {
        LinkedList<GPSPoint> testCase = new LinkedList<>();
        for(GPSPoint testRoad : testRoads) {
            double offset = Math.abs(NormalDistribution(0, 4.07)) + 3;
            Random random = new Random();
            int randomNum = random.nextInt(360);
            GeodesicData data= Geodesic.WGS84.Direct(testRoad.getPosition().getY() , testRoad.getPosition().getX(), randomNum, offset);
            testCase.add(new GPSPoint(testRoad.getTime(), data.lon2, data.lat2));
        }
        return testCase;
    }

    //普通正态随机分布
    //参数 u 均值
    //参数 v 方差
    public static double NormalDistribution(double u, double v){
        Random random = new Random();
        return Math.sqrt(v) * random.nextGaussian() + u;
    }
}
