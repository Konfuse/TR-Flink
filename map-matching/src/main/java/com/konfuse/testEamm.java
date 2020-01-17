package com.konfuse;

import com.konfuse.eamm.EammMatcher;
import com.konfuse.fmm.FmmMatcher;
import com.konfuse.geometry.Point;
import com.konfuse.geometry.Rectangle;
import com.konfuse.road.*;
import com.konfuse.spatial.Geography;
import com.konfuse.tools.GenerateTestGPSPoint;

import java.util.*;

/**
 * @Auther todd
 * @Date 2020/1/10
 */
public class testEamm {
    public static void main(String[] args) throws Exception {
        RoadMap map = RoadMap.Load(new RoadReader());
        map.construct();

        /*======================build vertex index==========================*/
        HashMap<Long, Road> roads = map.getEdges();
        HashMap<Long, Long> nodeRef = new HashMap<>();
        HashMap<Long, Vertex> vertices = new HashMap<>();

        long count = 0;
        for (Road road : roads.values()) {
            List<Point> edgeNodes = road.getPoints();
            int N = edgeNodes.size();
            for (int i = 0; i < N; i++) {
                if (i == 0) {
                    if (!nodeRef.containsKey(road.source())) {
                        nodeRef.put(road.source(), count);
                        vertices.put(count, new Vertex(count, edgeNodes.get(i).getX(), edgeNodes.get(i).getY()));
                    }
                    vertices.get(nodeRef.get(road.source())).getRelateEdges().add(road.id());
                } else if (i == N - 1) {
                    if (!nodeRef.containsKey(road.target())) {
                        nodeRef.put(road.target(), count);
                        vertices.put(count, new Vertex(count, edgeNodes.get(i).getX(), edgeNodes.get(i).getY()));
                    }
                    vertices.get(nodeRef.get(road.target())).getRelateEdges().add(road.id());
                } else {
                    vertices.put(count, new Vertex(count, edgeNodes.get(i).getX(), edgeNodes.get(i).getY()));
                    vertices.get(count).getRelateEdges().add(road.id());
                }
                count++;
            }
        }

        ArrayList<Point> pointList = new ArrayList<>();

        /*======================build vertex index==========================*/
        for (Map.Entry<Long, Vertex> vertex : vertices.entrySet()) {
            pointList.add(new Point(vertex.getKey(), vertex.getValue().x, vertex.getValue().y));
        }
        int size = pointList.size();
        RTree tree = new IndexBuilder().createRTreeBySTR(pointList.toArray(new Point[size]));

        /*======================generate test case==========================*/
        GenerateTestGPSPoint test = new GenerateTestGPSPoint();
        List<GPSPoint> testRoads = test.generateTestGPSPointbyLine(map);
        List<GPSPoint> testGPSPoint = test.generateTestCase(testRoads);

        /*======================match==========================*/
        EammMatcher eammMatcher = new EammMatcher();

        long start = System.currentTimeMillis();
        List<RoadPoint> matchedRoadPoints = eammMatcher.match(testGPSPoint, map, vertices, tree);
        long end = System.currentTimeMillis();
        long search_time = end - start;
        System.out.println("Search time :" + search_time);

        System.out.println("************road***********");
        for (GPSPoint point1 : testRoads) {
            double x1 = point1.getPosition().getX();
            double y1 = point1.getPosition().getY();
            System.out.println(x1 + ";" + y1);
        }
        System.out.println("***************************");
        System.out.println("************test***********");
        for (GPSPoint point2 : testGPSPoint) {
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
    }
}


