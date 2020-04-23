package com.konfuse.road;

import java.sql.*;

/**
 * @Author: todd
 * @Date: 2020/1/2
 */
public class PostgresRoadReader implements RoadReader {
    public PreparedStatement ps;
    private Connection connection;
    ResultSet resultSet = null;

    @Override
    public void open() throws Exception {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/roma","postgres","9713");
            System.out.println("连接数据库成功!");
        } catch (Exception e) {
            System.out.println("postgresql get connection has exception , msg = " + e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean isOpen() {
        return connection != null;
    }

    @Override
    public void close() throws Exception {
        if (connection != null) { //关闭连接和释放资源
            connection.close();
        }
        if (ps != null) {
            ps.close();
        }
        if (resultSet != null) {
            resultSet = null;
        }
    }

    @Override
    public BaseRoad next() throws Exception {
        if (resultSet == null) {
            String sql = "select gid, osm_id, source, target, one_way, priority, maxspeed_forward, maxspeed_backward, length_m, ST_AsBinary(the_geom) as geom from ways";
            ps = connection.prepareStatement(sql);
            resultSet = ps.executeQuery();
        }

        try {
            if (!resultSet.next()) {
                return null;
            }
            long id = resultSet.getLong("gid");
            long refId = resultSet.getLong("osm_id");
            long source = resultSet.getLong("source");
            long target = resultSet.getLong("target");
            int oneway = resultSet.getInt("one_way");
            float priority = resultSet.getFloat("priority");
            float maxSpeedForward = resultSet.getFloat("maxspeed_forward");
            float maxSpeedBackward = resultSet.getFloat("maxspeed_backward");
            double length = resultSet.getDouble("length_m");
            byte[] geometry = resultSet.getBytes("geom");
            return new BaseRoad(id, source, target, refId, oneway, priority, maxSpeedForward, maxSpeedBackward, length, geometry);
        } catch (SQLException e) {
            System.out.println("Reading query result failed: " + e.getMessage());
            throw e;
        }
    }
}
