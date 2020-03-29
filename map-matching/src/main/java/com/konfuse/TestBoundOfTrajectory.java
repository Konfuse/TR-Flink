package com.konfuse;

import com.konfuse.road.GPSPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @Author: Konfuse
 * @Date: 2020/3/28 17:45
 */
public class TestBoundOfTrajectory {
    public static void main(String[] args) {
        getBoundOfTrajectory("D:\\SchoolWork\\HUST\\DataBaseGroup\\Roma\\Roma_by_date");
    }

    public static void getBoundOfTrajectory(String path) {
        File[] fileList = new File(path).listFiles();
        BufferedReader reader = null;
        double xmin = 1000, ymin = 1000, xmax = -1, ymax = -1;
        int count = 0;
        for (File file : fileList) {
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] items = line.split(";");
                    if (items.length != 3) {
                        ++count;
                        break;
                    }
                    double x = Double.parseDouble(items[0]);
                    double y = Double.parseDouble(items[1]);
                    if (x > 12.6197 || x < 12.3713 || y > 41.9929 || y < 41.7932) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try{
                            if(file.delete()) {
                                System.out.println(file.getName() + " 文件已被删除！");
                            } else {
                                System.out.println("文件删除失败！");
                            }
                        } catch(Exception e3){
                            e3.printStackTrace();
                        }
                        ++count;
//                        System.out.println(line);
                        break;
                    }
                    if (x > xmax) xmax = x;
                    if (x < xmin) xmin = x;
                    if (y > ymax) ymax = y;
                    if (y < ymin) ymin = y;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(count);
        System.out.println("xmax = " + xmax + " xmin = " + xmin + " ymax = " + ymax + " ymin = " + ymin);
    }
}
