package com.konfuse.util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @Author: Konfuse
 * @Date: 2019/11/20 19:51
 */
public class JDBCUtil {
    public static void main(String[] args) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/taiwan","postgres","1234");
            System.out.println("连接数据库成功!");
            stmt = conn.createStatement();
            // ResultSet rs = stmt.executeQuery("SELECT ST_AsEWKT(geom) FROM ds_tz01_osm_ln_featuretoline1 where osmid = '32708325'");
            ResultSet rs = stmt.executeQuery("SELECT ST_AsEWKT(b.geom) FROM ds_tz01_osm_pt a, ds_tz01_osm_pt b where a.osmid = '27565027' and ST_DWithin(a.geom, b.geom, 10)");
            //SELECT ST_Distance(a.geom, b.geom) as distance, ST_AsEWKT(a.geom), ST_AsEWKT(b.geom) FROM ds_tz01_osm_pt a, ds_tz01_osm_pt b where a.osmid = '27565027' and ST_DWithin(a.geom, b.geom, 0.001) order by distance desc
            while(rs.next()){
                String geom = rs.getString(1);
                System.out.println(geom);
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
