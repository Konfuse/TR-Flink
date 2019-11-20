package com.konfuse.util;

import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.jdbc.JDBCDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Konfuse
 * @Date: 2019/11/20 19:40
 */
public class GeoToolsUtil {
    public static void main(String[] args){
        String host = "localhost";
        String schema = "public" ;
        String database = "taiwan" ;
        String user = "postgres" ;
        String pass = "1234" ;
        String tablename = "ds_tz01_osm_ln_featuretoline1" ;
        int port = 5432;
        //读取
        SimpleFeatureCollection colls1 = readPostgisTable(host, port, user, pass, database, schema, tablename);
        if(colls1 == null){
            System.out.println("请检查参数，确保jdbc连接正常以及表存在.");
            return;
        }
        //拿到所有features
        SimpleFeatureIterator iters = colls1.features();
        //遍历打印
        int i = 0;
        while(iters.hasNext()){
            if(i == 100){
                break;
            }
            SimpleFeature feature = iters.next();
            System.out.println(feature.getAttributes());
            System.out.println(feature.getAttribute("geom"));
            i++;
        }
    }


    public static SimpleFeatureCollection  readPostgisTable(String host , int port , String user , String pass , String dbname, String schema , String tablename ){
        return readPostgisTable(host, port, user, pass, dbname, schema, tablename , null);
    }

    public static SimpleFeatureCollection  readPostgisTable(String host , int port , String user , String pass , String dbname, String schema , String tablename , Filter filter){
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("dbtype", "postgis");
        params.put("host", host);
        params.put("port", port);
        params.put("schema", schema);
        params.put("database", dbname);
        params.put("user", user);
        params.put("passwd", pass);
        try {
            JDBCDataStore dataStore = (JDBCDataStore) DataStoreFinder.getDataStore(params);
            return readDatastore(dataStore, tablename, filter);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SimpleFeatureCollection readDatastore(JDBCDataStore store ,String typeName , Filter filter){
        try {
            SimpleFeatureSource featureSource = store.getFeatureSource(typeName);
            return filter != null ? featureSource.getFeatures(filter) : featureSource.getFeatures();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
