package com.konfuse;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Line;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import rx.Observable;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;

import static com.github.davidmoten.rtree.geometry.Geometries.line;
import static com.github.davidmoten.rtree.geometry.Geometries.pointGeographic;

/**
 * @Author: Konfuse
 * @Date: 2019/12/2 16:53
 */
public class Test4David {
    public static PreparedStatement ps;
    private static Connection connection;

    public static void main(String[] args) throws SQLException {
//        open();
//        ResultSet resultSet = ps.executeQuery();
//        RTree<Long, Line> tree = RTree.maxChildren(40).minChildren(16).create();
//
//        while (resultSet.next()) {
//            Line line = line(
//                    resultSet.getDouble("x1"),
//                    resultSet.getDouble("y1"),
//                    resultSet.getDouble("x2"),
//                    resultSet.getDouble("y2")
//            );
//            tree = tree.add(resultSet.getLong("gid"), line);
//        }
//        close();

        RTree<Long, Point> tree = RTree.maxChildren(40).minChildren(16).create();
        BufferedReader reader = null;
        String path = "data_points.txt";
        String line;
        String[] data;
        try {
            reader = new BufferedReader(new FileReader(path));
            while ((line = reader.readLine()) != null) {
                data = line.split(",");
                Point point = pointGeographic(Double.parseDouble(data[1]), Double.parseDouble(data[2]));
                tree = tree.add(Long.parseLong(data[0]), point);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println("the root's unionPoints is: " + tree.mbr().get().toString());
        queryTest(tree);
    }

//    public static void queryTest(RTree<Long, Line> tree) {
//        System.out.println("************************query test*************************");
//        String path = "lines_areas_to_query.txt";
//        String output = "lines_areas_query_result_david.txt";
//
//        BufferedReader reader = null;
//        String line;
//        String[] data;
//        ArrayList<Rectangle> list = new ArrayList<>(100);
//
//        try {
//            reader = new BufferedReader(new FileReader(path));
//            Rectangle rectangle;
//            while ((line = reader.readLine()) != null) {
//                data = line.split(",");
//                rectangle = Geometries.rectangle(
//                        Double.parseDouble(data[0]),
//                        Double.parseDouble(data[1]),
//                        Double.parseDouble(data[2]),
//                        Double.parseDouble(data[3])
//                );
//                list.add(rectangle);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        BufferedWriter writer = null;
//        try {
//            writer = new BufferedWriter(new FileWriter(output));
//            for (Rectangle rectangle : list) {
//                long startTime = System.currentTimeMillis();
//
//                Observable<Entry<Long, Line>> entries = tree.search(rectangle);
//
//                long endTime = System.currentTimeMillis();
//                System.out.println("query time: " + (endTime - startTime) + "ms");
//                writer.write((endTime - startTime) + ":");
//
//                Iterator<Entry<Long, Line>> iterator = entries.toBlocking().toIterable().iterator();
//                System.out.println("\nquery result is: ");
//                boolean flag = false;
//                while (iterator.hasNext()) {
//                    Entry<Long, Line> entry = iterator.next();
//                    if (!flag) {
//                        writer.write(String.valueOf(entry.value()));
//                        System.out.print(entry.value());
//                        flag = true;
//                    } else {
//                        writer.write( "," + entry.value());
//                        System.out.print("," + entry.value());
//                    }
//                }
//                writer.newLine();
//                System.out.println();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                writer.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public static void queryTest(RTree<Long, Point> tree) {
        System.out.println("************************query test*************************");
        String path = "points_areas_to_query.txt";
        String output = "points_areas_query_result_david.txt";

        BufferedReader reader = null;
        String line;
        String[] data;
        ArrayList<Rectangle> list = new ArrayList<>(100);

        try {
            reader = new BufferedReader(new FileReader(path));
            Rectangle rectangle;
            while ((line = reader.readLine()) != null) {
                data = line.split(",");
                rectangle = Geometries.rectangle(
                        Double.parseDouble(data[0]),
                        Double.parseDouble(data[1]),
                        Double.parseDouble(data[2]),
                        Double.parseDouble(data[3])
                );
                list.add(rectangle);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(output));
            for (Rectangle rectangle : list) {
                long startTime = System.currentTimeMillis();

                Observable<Entry<Long, Point>> entries = tree.search(rectangle);

                long endTime = System.currentTimeMillis();
                System.out.println("query time: " + (endTime - startTime) + "ms");
                writer.write((endTime - startTime) + ":");

                Iterator<Entry<Long, Point>> iterator = entries.toBlocking().toIterable().iterator();
                System.out.println("query result is: ");
                boolean flag = false;
                while (iterator.hasNext()) {
                    Entry<Long, Point> entry = iterator.next();
                    if (!flag) {
                        writer.write(String.valueOf(entry.value()));
                        System.out.print(entry.value());
                        flag = true;
                    } else {
                        writer.write( "," + entry.value());
                        System.out.print("," + entry.value());
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

    public static void close() throws SQLException {
        if (connection != null) { //关闭连接和释放资源
            connection.close();
        }
        if (ps != null) {
            ps.close();
        }
    }

    public static void open() throws SQLException {
        connection = getConnection();
        String sql = "select gid, name, x1, y1, x2, y2 from ways;";
        ps = connection.prepareStatement(sql);
    }

    public static Connection getConnection() {
        Connection con = null;
        try {
            Class.forName("org.postgresql.Driver");
            con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/roma?useUnicode=true&characterEncoding=UTF-8", "postgres","9713");
        } catch (Exception e) {
            System.out.println("postgresql get connection has exception , msg = " + e.getMessage());
        }
        return con;
    }
}
