package com.konfuse;

import com.konfuse.emm.EmmMatcher;
import com.konfuse.emm.Vertex;
import com.konfuse.fmm.FmmMatcher;
import com.konfuse.geometry.Point;
import com.konfuse.road.*;
import com.konfuse.tools.GenerateTestGPSPoint;

import java.io.*;
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
//        HashMap<Long, Vertex> vertices = getVertices(map);
        HashMap<Long, Vertex> vertices = getNodes(map);
        /*======================build vertex index==========================*/
        ArrayList<Point> pointList = new ArrayList<>();
        for (Map.Entry<Long, Vertex> vertex : vertices.entrySet()) {
            pointList.add(new Point(vertex.getKey(), vertex.getValue().x, vertex.getValue().y));
        }
        int size = pointList.size();
        RTree tree = new IndexBuilder().createRTreeBySTR(pointList.toArray(new Point[size]));

        /*======================generate test case==========================*/
//        GenerateTestGPSPoint test = new GenerateTestGPSPoint();
//        List<GPSPoint> testRoads = test.generateTestGPSPoint(map);
//        List<GPSPoint> testGPSPoint = test.generateTestCase(testRoads);

        /*======================match==========================*/
        EmmMatcher emmMatcher = new EmmMatcher();

//        long start = System.currentTimeMillis();
//        List<RoadPoint> matchedRoadPoints = emmMatcher.match(testGPSPoint, map, vertices, tree);
//        long end = System.currentTimeMillis();
//        long search_time = end - start;
//        System.out.println("Search time :" + search_time);

        long search_time = testMatch("E:\\test1\\trajectory", emmMatcher, map, vertices, tree);
        System.out.println("Search time :" + search_time + "ms");

//        System.out.println("************road***********");
//        test.writeAsTxt(testRoads, "output/road.txt");
//        System.out.println("***************************");
//        System.out.println("************test***********");
//        test.writeAsTxt(testGPSPoint, "output/trajectory.txt");
//        System.out.println("***************************");
//        System.out.println("************matched***********");
//        write(matchedRoadPoints, "output/matched.txt");
//        System.out.println("***************************");
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

    public static HashMap<Long, Vertex> getNodes(RoadMap map) {
        HashMap<Long, Road> roads = map.getEdges();
        HashMap<Long, Vertex> vertices = new HashMap<>();

        for (Road road : roads.values()) {
            List<Point> edgeNodes = road.getPoints();
            if (!vertices.containsKey(road.source())) {
                vertices.put(road.source(), new Vertex(road.source(), edgeNodes.get(0).getX(), edgeNodes.get(0).getY()));
            }
            vertices.get(road.source()).getRelateEdges().add(road.id());
            if (!vertices.containsKey(road.target())) {
                vertices.put(road.target(), new Vertex(road.target(), edgeNodes.get(1).getX(), edgeNodes.get(1).getY()));
            }
            vertices.get(road.target()).getRelateEdges().add(road.id());
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

    public static long testMatch(String path, EmmMatcher emmMatcher, RoadMap map, HashMap<Long, Vertex> vertices, RTree tree) {
        List<GPSPoint> gpsPoints = new ArrayList<>();
        File[] fileList = new File(path).listFiles();
        BufferedReader reader = null;
        long search_time = 0;
        int count = 0;
        int except = 0;

        for (File file : fileList) {
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] items = line.split(";");
                    double x = Double.parseDouble(items[0]);
                    double y = Double.parseDouble(items[1]);
                    long time = Long.parseLong(items[2]);
                    gpsPoints.add(new GPSPoint(time, x, y));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("the " + (count++) + "th trajectory is being processed: " + file.getName());

            long start = System.currentTimeMillis();
            emmMatcher.match(gpsPoints, map, vertices, tree);
            long end = System.currentTimeMillis();
            search_time += end - start;

//            try {
//                long start = System.currentTimeMillis();
//                emmMatcher.match(gpsPoints, map, vertices, tree);
//                long end = System.currentTimeMillis();
//                search_time += end - start;
//            } catch (Exception e) {
//                e.printStackTrace();
//                ++except;
//
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
//            }
            gpsPoints.clear();
        }
        System.out.println(except + " trajectories failed");
        return search_time;
    }
}


