package com.konfuse.tools;

import com.konfuse.road.GPSPoint;
import com.konfuse.road.LocationOnRoad;
import com.konfuse.road.Road;
import com.konfuse.road.RoadMap;
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
 * @Date: 2020/1/2 14:53
 */
public class GenerateTestGPSPoint {
    public List<GPSPoint> generateTestGPSPoint(RoadMap map){
        HashMap<Long, Road> roads = map.getRoads();
        Long[] keys = roads.keySet().toArray(new Long[0]);
        Dijkstra dijkstra = new Dijkstra();
        Cost cost = new Cost();
        List<Road> path = new LinkedList<>();

        do{
            Random random = new Random();
            Long sourceKey = keys[random.nextInt(keys.length)];
            Long targetKey = keys[random.nextInt(keys.length)];
            Road sourceRoad = roads.get(sourceKey);
            Road targetRoad = roads.get(targetKey);
            System.out.println(sourceRoad.source() + "->" + targetRoad.target());
            System.out.println(sourceRoad.id() + "->" + targetRoad.id());
            LocationOnRoad<Road> sourceLocation = new LocationOnRoad<>(sourceRoad, 0);
            LocationOnRoad<Road> targetLocation = new LocationOnRoad<>(targetRoad, 1);
            path = dijkstra.shortestPath(sourceLocation, targetLocation, cost, 10000000000.0);
            System.out.println("path size: "+ path.size());
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

    public List<GPSPoint> generateTestCase(List<GPSPoint> testRoads){
        LinkedList<GPSPoint> testCase = new LinkedList<>();
        for(GPSPoint testRoad : testRoads){
            double offset = Math.abs(NormalDistribution(0, 50));
            Random random = new Random();
            int randomNum = random.nextInt(360 - 1);
            GeodesicData data= Geodesic.WGS84.Direct(testRoad.getPosition().getY() , testRoad.getPosition().getX(), randomNum, offset);
            testCase.add(new GPSPoint(testRoad.getTime(), data.lon2, data.lat2));
            System.out.println(offset);
            System.out.println(randomNum);
            System.out.println("original point" + testRoad.getPosition().getX() + "," + testRoad.getPosition().getY());
            System.out.println("changed point" + data.lon2 + "," + data.lat2);
        }
        return testCase;
    }

    //普通正态随机分布
    //参数 u 均值
    //参数 v 方差
    public static double NormalDistribution(double u,double v){
        Random random = new Random();
        return Math.sqrt(v)*random.nextGaussian()+u;
    }
}
