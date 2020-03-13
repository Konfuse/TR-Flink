package com.konfuse;

import com.konfuse.fmm.FmmMatcher;
import com.konfuse.road.*;
import com.konfuse.tools.GenerateTestGPSPoint;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/3/13
 */
public class testFmmUbodt {
    public static void main(String[] args) throws Exception{
        RoadMap map = RoadMap.Load(new RoadReader());
        map.construct();

        FmmMatcher fmmMatcher = new FmmMatcher(2);

        String udobtPath = "udobt.table";
        System.out.println("***************************");
        fmmMatcher.writeUBODTFile(udobtPath, map, 3000);
        System.out.println("***************************");
        long start = System.currentTimeMillis();
        fmmMatcher.readUDOBTFile(udobtPath, map);
        long end = System.currentTimeMillis();
        long search_time = end - start;
        System.out.println("Read Binary file time:" + search_time);

        String udobtPathBinary = "udobtBinary.table";
        System.out.println("***************************");
        fmmMatcher.writeUBODTFileBinary(udobtPathBinary, map, 3000);
        System.out.println("***************************");
        long start2 = System.currentTimeMillis();
        fmmMatcher.readUDOBTFileBinary(udobtPathBinary, map);
        long end2 = System.currentTimeMillis();
        long search_time2 = end2 - start2;
        System.out.println("Read Binary file time:" + search_time2);
        System.out.println("***************************");

        String udobtPath2 = "udobt2.table";
        System.out.println("***************************");
        fmmMatcher.writeUBODTFileDirect(udobtPath2, map, 3000);
        System.out.println("***************************");
        long start3 = System.currentTimeMillis();
        fmmMatcher.readUDOBTFile(udobtPath2, map);
        long end3 = System.currentTimeMillis();
        long search_time3 = end - start;
        System.out.println("Read file2 time:" + search_time3);
    }




}
