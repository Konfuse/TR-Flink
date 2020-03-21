package com.konfuse;

import com.konfuse.internal.MBR;
import com.konfuse.geometry.Point;
import org.apache.flink.api.common.functions.RichGroupReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @Author: Konfuse
 * @Date: 2019/12/8 14:48
 */
public class Test4MyTree {
    public static double lon1 = -6.8394859 * 10000000;
    public static double lat1 = 39.2084899 * 10000000;
    public static double lon2 = -6.7762898 * 10000000;
    public static double lat2 = 39.3037728 * 10000000;

    public static void main(String[] args) throws Exception {
        ExecutionEnvironment environment = ExecutionEnvironment.getExecutionEnvironment();
        String data_env = "file:///home/data_set/";
//        String data_env = "C:/Users/Konfuse/Desktop/session-cluster/data_set";

        Point[] list = new Point[10000];
        int id = 0;
        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            float x = random.nextFloat() * 100 + 100;
            float y = random.nextFloat() * 100 + 100;
            list[id] = new Point(id, x, y);
            ++id;
        }

//        DataSet<String> data = environment.fromCollection(list);
//        DataSet<Point> points = data.map((MapFunction<String, Point>) s -> {
//            String[] pointData = s.split(",");
//            return new Point(
//                    Long.parseLong(pointData[0]),
//                    Double.parseDouble(pointData[1]),
//                    Double.parseDouble(pointData[2])
//            );
//        });
        DataSet<Point> points = environment.fromElements(list);

        PointIndex index = new IndexBuilder().createPointIndex(points, 0.3, 1, 40, 16);

//        index.getGlobalTree().map(new MapFunction<RTree<PartitionedMBR>, Tuple3<Integer, Long, MBR>>() {
//            @Override
//            public Tuple3<Integer, Long, MBR> map(RTree<PartitionedMBR> rTree) throws Exception {
//                return new Tuple3<>(((PartitionedLeafNode)rTree.getRoot()).getEntries().size(), rTree.getEntryCount(), rTree.getRoot().getMBR());
//            }
//        }).print();

//        index.getLocalTrees().map(new MapFunction<RTree<Point>, Tuple3<Integer, Long, MBR>>() {
//            @Override
//            public Tuple3<Integer, Long, MBR> map(RTree<Point> pointRTree) throws Exception {
//                return new Tuple3<>(pointRTree.getHeight(), pointRTree.getEntryCount(), pointRTree.getRoot().getMBR());
//            }
//        }).print();

//        System.out.println("After partitioning partitioner ");
//
//        List<TreeNode> leafNodes = index.getPartitioner().getTree().getLeafNodes();
//        for (TreeNode node : leafNodes) {
//            PartitionedLeafNode leaf = (PartitionedLeafNode) node;
//            List<PartitionedMBR> entries = leaf.getEntries();
//            for (PartitionedMBR mbr : entries) {
//                System.out.println(mbr.getMBR().toString());
//            }
//        }
//
//        System.out.println("RTree in partitioner: ");
//        System.out.println(index.getPartitioner().getTree());

//        PartitionedLeafNode leafNode = (PartitionedLeafNode) index.getGlobalTree().collect().get(0).getRoot();
//        ArrayList<PartitionedMBR> partitionedMBRs = leafNode.getEntries();
//        for (PartitionedMBR partitionedMBR : partitionedMBRs) {
//            System.out.println("partition number is: " + partitionedMBR.getPartitionNumber());
//        }

        String knnQueryPath = data_env + "points_knn_to_query.txt";
        knnQueryPrintTest(index, knnQueryPath, data_env + "points_knn_query_result_flink.txt");

//        String areaQueryPath = "C:/Users/Konfuse/Desktop/FlinkResearch/data_set/points_areas_to_query.txt";
//        areaQueryTest(index, areaQueryPath, "C:/Users/Konfuse/Desktop/FlinkResearch/data_set/points_areas_query_result_flink.txt");

//        String circleQueryPath = "C:/Users/Konfuse/Desktop/FlinkResearch/data_set/points_knn_to_query.txt";
//        circleQueryTest(index, circleQueryPath, "C:/Users/Konfuse/Desktop/FlinkResearch/data_set/points_circle_query_result_flink.txt");

//        environment.execute();
    }

    public static void knnQueryPrintTest(PointIndex index, String knnQueryPath, String output) throws Exception {
        System.out.println("************************point query test*************************");

        BufferedReader reader = null;
        Random random = new Random();
        ArrayList<Point> list = new ArrayList<>(100);

        for (int i = 0; i < 100; i++) {
//                double x = ((lat2 - lat1) * random.nextDouble() + lat1) / 10000000;
//                double y = ((lon2 - lon1) * random.nextDouble() + lon1) / 10000000;
            double x = (100 * random.nextDouble() + 100);
            double y = (100 * random.nextDouble() + 100);
            list.add(new Point(0, x, y));
        }

        for (Point point : list) {
            index.knnQuery(point, 100)
                    .reduceGroup(new RichGroupReduceFunction<Point, List<Long>>() {
                        @Override
                        public void reduce(Iterable<Point> iterable, Collector<List<Long>> collector) throws Exception {
                            List<Long> longList = new ArrayList<>();
                            for (Point point : iterable) {
                                longList.add(point.getId());
                            }
                            collector.collect(longList);
                        }
                    }).print();
        }
    }

    public static void knnQueryTest(PointIndex index, String knnQueryPath, String output) throws Exception {
        System.out.println("************************point query test*************************");

        BufferedReader reader = null;
        String line;
        String[] data;
        ArrayList<Point> list = new ArrayList<>(100);

        try {
            reader = new BufferedReader(new FileReader(knnQueryPath));
            Point point;
            while ((line = reader.readLine()) != null) {
                data = line.split(",");
                point = new Point(
                        0,
                        Double.parseDouble(data[0]),
                        Double.parseDouble(data[1])
                );
                list.add(point);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        index.knnQuery(list.get(1), 100)
//                .reduceGroup(new RichGroupReduceFunction<Point, List<Long>>() {
//                    @Override
//                    public void reduce(Iterable<Point> iterable, Collector<List<Long>> collector) throws Exception {
//                        List<Long> longList = new ArrayList<>();
//                        for (Point point : iterable) {
//                            longList.add(point.getId());
//                        }
//                        collector.collect(longList);
//                    }
//                }).print();

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(output));
            for (Point point : list) {
                long startTime = System.currentTimeMillis();

                List<Long> ids = index.knnQuery(point, 100)
                        .reduceGroup(new RichGroupReduceFunction<Point, List<Long>>() {
                            @Override
                            public void reduce(Iterable<Point> iterable, Collector<List<Long>> collector) throws Exception {
                                List<Long> longList = new ArrayList<>();
                                for (Point point : iterable) {
                                    longList.add(point.getId());
                                }
                                collector.collect(longList);
                            }
                        }).collect().get(0);

                long endTime = System.currentTimeMillis();
//                System.out.println("query time: " + (endTime - startTime) + "ms");
//                System.out.println("query result is: ");
                boolean flag = false;
                writer.write((endTime - startTime) + ":");

                for (Long id : ids) {
                    if (!flag) {
                        writer.write(String.valueOf(id));
//                        System.out.print(id);
                        flag = true;
                    } else {
                        writer.write( "," + id);
//                        System.out.print("," + id);
                    }
                }
                writer.newLine();
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void areaQueryTest(PointIndex index, String areaQueryPath, String output) throws Exception {
        System.out.println("************************query test*************************");

        BufferedReader reader = null;
        String line;
        String[] data;
        ArrayList<MBR> list = new ArrayList<>(100);

        try {
            reader = new BufferedReader(new FileReader(areaQueryPath));
            MBR area;
            while ((line = reader.readLine()) != null) {
                data = line.split(",");
                area = new MBR(
                        Double.parseDouble(data[0]),
                        Double.parseDouble(data[1]),
                        Double.parseDouble(data[2]),
                        Double.parseDouble(data[3])
                );
                list.add(area);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(output));
            for (MBR area : list) {
                long startTime = System.currentTimeMillis();

                List<Long> ids = index.boxRangeQuery(area)
                        .reduceGroup(new RichGroupReduceFunction<Point, List<Long>>() {
                            @Override
                            public void reduce(Iterable<Point> iterable, Collector<List<Long>> collector) throws Exception {
                                List<Long> list = new ArrayList<>();
                                for (Point point : iterable) {
                                    list.add(point.getId());
                                }
                                collector.collect(list);
                            }
                        }).collect().get(0);

                long endTime = System.currentTimeMillis();
//                System.out.println("query time: " + (endTime - startTime) + "ms");
//                System.out.println("query result is: ");
                boolean flag = false;
                writer.write((endTime - startTime) + ":");

                for (Long id : ids) {
                    if (!flag) {
                        writer.write(String.valueOf(id));
//                        System.out.print(id);
                        flag = true;
                    } else {
                        writer.write( "," + id);
//                        System.out.print("," + id);
                    }
                }
                writer.newLine();
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void circleQueryTest(PointIndex index, String circleQueryPath, String output) throws Exception {
        System.out.println("************************point query test*************************");

        BufferedReader reader = null;
        String line;
        String[] data;
        ArrayList<Point> list = new ArrayList<>(100);

        try {
            reader = new BufferedReader(new FileReader(circleQueryPath));
            Point point;
            while ((line = reader.readLine()) != null) {
                data = line.split(",");
                point = new Point(
                        0,
                        Double.parseDouble(data[0]),
                        Double.parseDouble(data[1])
                );
                list.add(point);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(output));
            for (Point point : list) {
                long startTime = System.currentTimeMillis();

                List<Long> ids = index.circleRangeQuery(point, 5)
                        .reduceGroup(new RichGroupReduceFunction<Point, List<Long>>() {
                            @Override
                            public void reduce(Iterable<Point> iterable, Collector<List<Long>> collector) throws Exception {
                                List<Long> longList = new ArrayList<>();
                                for (Point point : iterable) {
                                    longList.add(point.getId());
                                }
                                collector.collect(longList);
                            }
                        }).collect().get(0);

                long endTime = System.currentTimeMillis();
//                System.out.println("query time: " + (endTime - startTime) + "ms");
//                System.out.println("query result is: ");
                boolean flag = false;
                writer.write((endTime - startTime) + ":");

                for (Long id : ids) {
                    if (!flag) {
                        writer.write(String.valueOf(id));
//                        System.out.print(id);
                        flag = true;
                    } else {
                        writer.write( "," + id);
//                        System.out.print("," + id);
                    }
                }
                writer.newLine();
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}