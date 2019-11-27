package com.konfuse;

import com.alibaba.fastjson.JSON;
import com.konfuse.bean.Entry;
import com.konfuse.bean.LeafNode;
import com.konfuse.bean.Line;
import com.konfuse.bean.MBR;
import com.konfuse.tools.Visualization;
import sun.reflect.generics.tree.Tree;

import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;


/**
 * @Author: Konfuse
 * @Date: 2019/11/26 16:05
 */
public class Test4IndexBuild {
    public static PreparedStatement ps;
    private static Connection connection;

    public static void main(String[] args) throws SQLException {
        open();
        ResultSet resultSet = ps.executeQuery();
        ArrayList<LeafNode> lines = new ArrayList<>();
        while (resultSet.next()) {
            Line line = new Line(
                    resultSet.getString("name"),
                    resultSet.getDouble("x1"),
                    resultSet.getDouble("y1"),
                    resultSet.getDouble("x2"),
                    resultSet.getDouble("y2")
            );
            lines.add(line.toLeafNode());
        }
        close();
        int size = lines.size();
        System.out.println("total data size: " + size + " lines...");
        LeafNode[] leafNodes = lines.toArray(new LeafNode[size]);
        System.out.println("start building r-tree");
        RTree tree = new IndexBuilder().STRPacking(leafNodes);
        System.out.println("the root height is: " + tree.getRoot().getHeight());
        System.out.println("the root's mbr is: " + tree.getRoot().getMBR());

        //test for visualization
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Visualization.createAndShowGui(tree);
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
