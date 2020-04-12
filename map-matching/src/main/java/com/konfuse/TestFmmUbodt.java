package com.konfuse;

import com.konfuse.fmm.FmmMatcher;
import com.konfuse.road.*;

/**
 * @Auther todd
 * @Date 2020/3/13
 */
public class TestFmmUbodt {
    public static void main(String[] args) throws Exception{
        long memory = 0;

        RoadMap map = RoadMap.Load(new ShapeFileRoadReader("C:\\Users\\Konfuse\\Desktop\\shapefile\\output\\network_dual.shp"));
        map.construct();

        FmmMatcher fmmMatcher = new FmmMatcher(2);

//        String udobtPath = "udobt.table";
//        System.out.println("***************************");
//        fmmMatcher.writeUBODTFile(udobtPath, map, 3000);
//        System.out.println("***************************");
//        long start = System.currentTimeMillis();
//        fmmMatcher.readUDOBTFile(udobtPath, map);
//        long end = System.currentTimeMillis();
//        long search_time = end - start;
//        System.out.println("Read Binary file time:" + search_time);
//
//        String udobtPathBinary = "udobtBinary.table";
//        System.out.println("***************************");
//        fmmMatcher.writeUBODTFileBinary(udobtPathBinary, map, 3000);
//        System.out.println("***************************");
//        long start2 = System.currentTimeMillis();
//        fmmMatcher.readUDOBTFileBinary(udobtPathBinary, map);
//        long end2 = System.currentTimeMillis();
//        long search_time2 = end2 - start2;
//        System.out.println("Read Binary file time:" + search_time2);
//        System.out.println("***************************");

        String udobtPath2 = "udobt2.table";
        System.gc();
        memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long start = System.currentTimeMillis();
        fmmMatcher.writeUBODTFileDirect(udobtPath2, map, 0.05);
        long end = System.currentTimeMillis();
        long build_time = end - start;
        System.out.println("UBODT build time :" + build_time + "ms");
        System.gc();
        memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - memory;
        System.out.println(Math.max(0, Math.round(memory)) + " bits used for UBODT table (estimate)" );

//        fmmMatcher.readUDOBTFile(udobtPath2, map);

//        System.out.println("Read file2 time:" );
    }
}
