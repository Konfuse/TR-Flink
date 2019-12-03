package com.konfuse;

import com.alibaba.fastjson.JSON;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Line;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rtree.internal.LeafDefault;
import com.konfuse.bean.MBR;

import static com.github.davidmoten.rtree.geometry.Geometries.line;

/**
 * @Author: Konfuse
 * @Date: 19-3-15 上午10:28
 */
public class RTreeTest {
    public static void printRtree() {
        //        创建RTree
        RTree<String, Line> tree = RTree.maxChildren(5).create();
        tree = tree.add("DAVE", line(10, 20, 11, 21))
                .add("FRED", line(12, 25, 13, 26))
                .add("MARY", line(97, 125, 98, 126));
        LeafDefault<String, Line> nodes = (LeafDefault<String, Line>) tree.root().get();
        for (Entry<String, Line> entry : nodes.entries()) {
            System.out.println(entry.value());
        }
        Rectangle rectangle = nodes.geometry().mbr();
        System.out.println(rectangle.x1() + ", " + rectangle.y1() + "; " + rectangle.x2() + ", " + rectangle.y2());
    }

    public static void main(String[] args) {
//        for (int i = 0; i < 8; i++) {
//            MBR MBR = new MBR(-122.42 - i * 0.01, 37.7 - i * 0.01, -122.41 + i * 0.01, 37.71 + i * 0.01);
//            System.out.println("区域数据: " + JSON.toJSONString(MBR));
//        }
        printRtree();
        //创建RTree
//        RTree<String, Point> tree = RTree.maxChildren(5).create();
//        tree = tree.add("DAVE", pointGeographic(10, 20))
//                .add("FRED", pointGeographic(12, 25))
//                .add("MARY", pointGeographic(97, 125));

        //RTree写入路径和序列化方法
//        String path = "tree.model";
//        Serializer<String, Point> serializer = Serializers.flatBuffers().utf8();

        //写入RTree
//        try {
//            OutputStream outputStream = new FileOutputStream(path);
//            Serializer<String, Point> serializer = Serializers.flatBuffers().utf8();
//            serializer.write(tree, outputStream);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        //读取RTree
//        RTree<String, Point> rTree = null;
//        InputStream inputStream = null;
//        try {
//            File file = new File(path);
//            inputStream = new FileInputStream(file);
//            rTree = serializer.read(inputStream, file.length(), InternalStructure.DEFAULT);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (null != inputStream) {
//                try {
//                    inputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }

        //观察者模式查询
//        Observable<Entry<String, Point>> entries = rTree.search(Geometries.rectangleGeographic(-122.42, 37.7, -122.39, 37.8));
//        entries.subscribe(new Subscriber<Entry<String, Point>>() {
//            @Override
//            public void onCompleted() {
//                System.out.println("Completed......");
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//                System.out.println("Error......");
//            }
//
//            @Override
//            public void onNext(Entry<String, Point> stringPointEntry) {
//                System.out.println(stringPointEntry.value());
//            }
//        });

        //迭代器方法查询
//        Iterator result = tree.search(Geometries.rectangle(8, 15, 30, 35)).toBlocking().toIterable().iterator();
//        while (result.hasNext()) {
//            System.out.println(result.next());
//        }
    }
}
