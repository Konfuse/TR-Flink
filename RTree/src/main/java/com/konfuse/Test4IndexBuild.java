package com.konfuse;

import com.konfuse.bean.LeafNode;
import com.konfuse.bean.Line;
import com.konfuse.bean.MBR;
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
        ArrayList<LeafNode> lines = new ArrayList<>();
        MBR[] queries = new MBR[10];
        Set<String> dueWays = new HashSet<>();
        int count = 0;
        while (resultSet.next()) {
            Line line = new Line(
                    resultSet.getString("name"),
                    resultSet.getDouble("x1"),
                    resultSet.getDouble("y1"),
                    resultSet.getDouble("x2"),
                    resultSet.getDouble("y2")
            );
            lines.add(line.toLeafNode());
            if (count < 10) {
                dueWays.add(line.getName());
                queries[count] = line.mbr();
            }
            ++count;
        }
        close();
        int size = lines.size();
        System.out.println("total data size: " + size + " lines...");
        LeafNode[] leafNodes = lines.toArray(new LeafNode[size]);
        System.out.println("start building r-tree");
        long startTime = System.currentTimeMillis();
        RTree tree = new IndexBuilder().STRPacking(leafNodes);
        long endTime = System.currentTimeMillis();
        System.out.println("building time: " + (endTime - startTime) + "ms");
        System.out.println("the root height is: " + tree.getRoot().getHeight());
        System.out.println("the root's mbr is: " + tree.getRoot().getMBR());

        System.out.println("************************query test*************************");
        MBR area = MBR.union(queries);
        ArrayList<LeafNode> nodes = tree.search(area);
        Set<String> results = new HashSet<>();
        System.out.println("query result is: ");
        for (LeafNode node : nodes) {
            results.add(node.getDescribe());
            System.out.print(node.getDescribe() + ", ");
        }
        System.out.println("\ndue result: ");
        dueWays.remove("null");
        for (String dueWay : dueWays) {
            System.out.print(dueWay + ", ");
        }
        results.retainAll(dueWays);
        System.out.println("\nset intersection is:\n" + results);
        System.out.println("accuracy is: " + results.size() * 1.0 / dueWays.size());

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
        String sql = "select name, x1, y1, x2, y2 from ways;";
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
