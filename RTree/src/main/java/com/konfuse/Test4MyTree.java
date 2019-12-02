package com.konfuse;

import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.Line;
import com.konfuse.internal.MBR;
import com.konfuse.tools.Visualization;

import javax.swing.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;


/**
 * @Author: Konfuse
 * @Date: 2019/11/26 16:05
 */
public class Test4MyTree {
    public static PreparedStatement ps;
    private static Connection connection;

    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        open();
        ResultSet resultSet = ps.executeQuery();
        ArrayList<Line> lines = new ArrayList<>();

        while (resultSet.next()) {
            Line line = new Line(
                    resultSet.getLong("gid"),
                    resultSet.getString("name"),
                    resultSet.getDouble("x1"),
                    resultSet.getDouble("y1"),
                    resultSet.getDouble("x2"),
                    resultSet.getDouble("y2")
            );
            lines.add(line);
        }
        close();

        int size = lines.size();
        System.out.println("total data size: " + size + " lines...");

        System.out.println("start building r-tree");
        long startTime = System.currentTimeMillis();
        RTree myTree = new IndexBuilder().createRTreeBySTR(lines.toArray(new Line[size]));
        long endTime = System.currentTimeMillis();
        System.out.println("building time: " + (endTime - startTime) + "ms");
        System.out.println("the root height is: " + myTree.getRoot().getHeight());
        System.out.println("the root's unionPoints is: " + myTree.getRoot().getMBR());

        queryTest(myTree);
    }

    public static void travelDataObjects(RTree tree) {
        System.out.println("************************travel all test*************************");
        System.out.println("the data objects included in rtree are: ");
        ArrayList<DataObject> list = tree.getDataObjects();
        for (DataObject dataObject : list) {
            System.out.print(dataObject.getId() + ",");
        }
    }

    public static void queryTest(RTree tree) {
        System.out.println("************************query test*************************");
        String path = "query_areas.txt";
        String output = "query_areas_result.txt";

        BufferedReader reader = null;
        String line;
        String[] data;
        ArrayList<MBR> list = new ArrayList<>(100);

        try {
            reader = new BufferedReader(new FileReader(path));
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
                ArrayList<DataObject> dataObjects = tree.rangeQuery(area);
                System.out.println("\nquery result is: ");
                boolean flag = false;
                for (DataObject dataObject : dataObjects) {
                    if (!flag) {
                        writer.write(String.valueOf(dataObject.getId()));
                        System.out.print(dataObject.getId());
                        flag = true;
                    }
                    writer.write( "," + dataObject.getId());
                    System.out.print("," + dataObject.getId());
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

    public static void serializableTest(RTree tree) throws IOException, ClassNotFoundException {
        System.out.println("*********************after serializable*********************");
        tree.save("tree.model");
        tree = RTree.loadRTree("tree.model");
        System.out.println("the root height is: " + tree.getRoot().getHeight());
        System.out.println("the root's unionPoints is: " + tree.getRoot().getMBR());
    }

    public static void visualizationTest(RTree tree) {
        System.out.println("*********************visualization test*********************");
        SwingUtilities.invokeLater(() -> Visualization.createAndShowGui(tree));
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
