package com.konfuse;

import com.konfuse.geometry.DataObject;
import com.konfuse.internal.LeafNode;
import com.konfuse.geometry.Line;
import com.konfuse.internal.MBR;
import com.konfuse.tools.Visualization;

import javax.swing.*;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


/**
 * @Author: Konfuse
 * @Date: 2019/11/26 16:05
 */
public class Test4IndexBuild {
    public static PreparedStatement ps;
    private static Connection connection;

    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        open();
        ResultSet resultSet = ps.executeQuery();
        ArrayList<Line> lines = new ArrayList<>();
        MBR[] queries = new MBR[10];
        Set<Long> dueResults = new HashSet<>();

        int count = 0;
        while (resultSet.next()) {
            Line line = new Line(
                    resultSet.getLong("id"),
                    resultSet.getString("name"),
                    resultSet.getDouble("x1"),
                    resultSet.getDouble("y1"),
                    resultSet.getDouble("x2"),
                    resultSet.getDouble("y2")
            );
            lines.add(line);
            if (count < 10) {
                dueResults.add(line.getId());
                queries[count] = line.mbr();
            }
            ++count;
        }
        close();

        int size = lines.size();
        System.out.println("total data size: " + size + " lines...");

        System.out.println("start building r-tree");
        long startTime = System.currentTimeMillis();
        RTree tree = new IndexBuilder().STRPacking(lines.toArray(new Line[size]));
        long endTime = System.currentTimeMillis();

        System.out.println("building time: " + (endTime - startTime) + "ms");
        System.out.println("the root height is: " + tree.getRoot().getHeight());
        System.out.println("the root's mbr is: " + tree.getRoot().getMBR());

        System.out.println("************************query test*************************");
        MBR area = MBR.union(queries);
        ArrayList<DataObject> dataObjects = tree.rangeQuery(area);

        Set<Long> results = new HashSet<>();
        System.out.println("query result is: ");
        for (DataObject dataObject : dataObjects) {
            results.add(dataObject.getId());
            System.out.print(dataObject.getId() + ", ");
        }

        System.out.println("\ndue result: ");
        dueResults.remove("null");
        for (Long dueId : dueResults) {
            System.out.print(dueId + ", ");
        }
        results.retainAll(dueResults);
        System.out.println("\nset intersection is:\n" + results);
        System.out.println("accuracy is: " + results.size() * 1.0 / dueResults.size());

        System.out.println("*********************after serializable*********************");
        tree.save("tree.model");
        tree = RTree.loadRTree("tree.model");
        System.out.println("the root height is: " + tree.getRoot().getHeight());
        System.out.println("the root's mbr is: " + tree.getRoot().getMBR());

        //test for visualization
        RTree finalTree = tree;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Visualization.createAndShowGui(finalTree);
            }
        });
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
