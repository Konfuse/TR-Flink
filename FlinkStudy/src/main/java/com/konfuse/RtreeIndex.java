package com.konfuse;

import com.github.davidmoten.rtree.*;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;
import rx.Observable;
import rx.Subscriber;

import java.io.*;

import static com.github.davidmoten.rtree.geometry.Geometries.pointGeographic;

/**
 * @Author: Konfuse
 * @Date: 19-3-16 下午1:56
 */
public class RtreeIndex {
    public static void main(String[] args) {
        RTree<String, Point> tree = RTree.create();

        String path = "/home/konfuse/Documents/SF/trajectory";
        File[] fileList = new File(path).listFiles();

        BufferedReader reader = null;
        String stamp = null;
        double lon;
        double lat;
        String line = null;
        String fileName = null;
        String[] items = null;

        try {
            long count;
            for (int i = 0; i < 5; i++) {
                count = 0;
                fileName = fileList[i].toString().split("_")[1].split("\\.")[0];
                System.out.println("File" + i + ": " + fileName);
                reader = new BufferedReader(new FileReader(fileList[i]));
                while ((line = reader.readLine()) != null) {
                    items = line.split(";");
                    lon = Double.parseDouble(items[0]);
                    lat = Double.parseDouble(items[1]);
                    stamp = fileName + ":" + count;
//                    System.out.println("x = " + lon + "; y = " + lat + "; stamp = " + stamp);
                    tree = tree.add(stamp, pointGeographic(lon, lat));
                    count++;
                }
                System.out.println("    Length of Data: " + count);
            }
            System.out.println("    Size of RTree: " + tree.size());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String destPath = "tree.model";
        OutputStream outputStream = null;
        Serializer<String, Point> serializer = Serializers.flatBuffers().utf8();
        try {
            File f = new File(destPath);
            outputStream = new FileOutputStream(f);
            serializer.write(tree, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != outputStream) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
