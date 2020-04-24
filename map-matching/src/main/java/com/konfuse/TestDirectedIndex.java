package com.konfuse;

import com.konfuse.lmm.UndirectedIndex;
import com.konfuse.road.DemoRoadReader;
import com.konfuse.road.RoadMap;

/**
 * @Author: Konfuse
 * @Date: 2020/4/21 1:58
 */
public class TestDirectedIndex {
    public static void main(String[] args) throws Exception {
        // new ShapeFileRoadReader("C:\\Users\\Konfuse\\Desktop\\shapefile\\output\\network_dual.shp")
//        RoadMap map = RoadMap.Load(new ShapeFileRoadReader("C:\\Users\\Konfuse\\Desktop\\shapefile\\output\\network_dual.shp"));
        RoadMap map = RoadMap.Load(new DemoRoadReader());
        map.construct();

        //测试路网的节点出入度
//        HashMap<Long, Integer> nodes = map.getNodesDegree();
//        List<Long> list = new LinkedList<>();
//        for (Map.Entry<Long, Integer> entry : nodes.entrySet()) {
//            list.add(entry.getKey());
//        }
//
//        list.sort(Long::compareTo);
//
//        for (Long id : list) {
//            System.out.println(id);
//        }

        //测试有向图索引构建方法
//        System.out.println("start constructing land mark label...");
//        DirectedIndex directedIndex = DirectedIndex.constructLabel(map);
//        System.out.println("label constructed.");
//        directedIndex.store("demo_label_in.txt", "demo_label_out.txt");

        //测试有向图索引读入的方法
//        DirectedIndex directedIndex = DirectedIndex.read("demo_label_in.txt", "demo_label_out.txt");

//        System.out.println(label.getLabelOut());
//        System.out.println(directedIndex.query(9301, 9345));

        //测试无向图的索引构建方法
        System.out.println("start constructing land mark label...");
        UndirectedIndex undirectedIndex = UndirectedIndex.constructLabel(map);
        System.out.println("label constructed.");
        undirectedIndex.store("label_in.txt");

        System.out.println(undirectedIndex.query(4, 1));
    }
}
