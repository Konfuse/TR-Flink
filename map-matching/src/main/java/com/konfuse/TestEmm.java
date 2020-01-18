package com.konfuse;

import com.konfuse.emm.EmmMatcher;
import com.konfuse.emm.Vertex;
import com.konfuse.geometry.Point;
import com.konfuse.road.*;
import com.konfuse.tools.GenerateTestGPSPoint;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther todd
 * @Date 2020/1/10
 */
public class TestEmm {
    public static void main(String[] args) throws Exception {
        RoadMap map = RoadMap.Load(new RoadReader());
        map.construct();

        /*======================build vertex index==========================*/
        HashMap<Long, Vertex> vertices = getVertices(map);

        /*======================build vertex index==========================*/
        ArrayList<Point> pointList = new ArrayList<>();
        for (Map.Entry<Long, Vertex> vertex : vertices.entrySet()) {
            pointList.add(new Point(vertex.getKey(), vertex.getValue().x, vertex.getValue().y));
        }
        int size = pointList.size();
        RTree tree = new IndexBuilder().createRTreeBySTR(pointList.toArray(new Point[size]));

        /*======================generate test case==========================*/
        GenerateTestGPSPoint test = new GenerateTestGPSPoint();
        List<GPSPoint> testRoads = test.generateTestGPSPoint(map);
        List<GPSPoint> testGPSPoint = test.generateTestCase(testRoads);

        /*======================match==========================*/
        EmmMatcher emmMatcher = new EmmMatcher();

        long start = System.currentTimeMillis();
        List<RoadPoint> matchedRoadPoints = emmMatcher.match(testGPSPoint, map, vertices, tree);
        long end = System.currentTimeMillis();
        long search_time = end - start;
        System.out.println("Search time :" + search_time);

        System.out.println("************road***********");
        test.writeAsTxt(testRoads, "output/road.txt");
        System.out.println("***************************");
        System.out.println("************test***********");
        test.writeAsTxt(testGPSPoint, "output/trajectory.txt");
        System.out.println("***************************");
        System.out.println("************matched***********");
        write(matchedRoadPoints, "output/matched.txt");
        System.out.println("***************************");
    }

    public static HashMap<Long, Vertex> getVertices(RoadMap map) {
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
        return vertices;
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


