package com.konfuse.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * @Author: Konfuse
 * @Date: 2019/12/2 12:10
 */
public class GenerateTestCase {
    public static double lon1 = -6.8394859 * 10000000;
    public static double lat1 = 39.2084899 * 10000000;
    public static double lon2 = -6.7762898 * 10000000;
    public static double lat2 = 39.3037728 * 10000000;

    public static void main(String[] args) {
        String pathPoint = "query_points.txt";
        String pathArea = "points_areas_to_query.txt";
        String pathData = "data_points.txt";
        int caseNum = 100;
        int size = 1000000;
        int boarder = 100;
//        pointGenerate(pathPoint, caseNum);
        areaGenerate(pathArea, caseNum);
//        dataGenerate(pathData, size, boarder);
    }

    /**
     * function for generating points data to build r-tree
     * @param path out put path of data
     * @param size the data size
     * @param boarder point's bound
     * format is:
     *                x1,y1
     *                x2,y2
     *                ...
     */
    public static void dataGenerate(String path, int size, int boarder) {
        BufferedWriter writer = null;
        Random random = new Random();
        try {
            writer = new BufferedWriter(new FileWriter(path));
            long id = 0;
            for (int i = 0; i < size; i++) {
                float x = random.nextFloat() * boarder + 100;
                float y = random.nextFloat() * boarder + 100;
                writer.write(id + "," + x + "," + y);
                writer.newLine();
                ++id;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * function for generating point test cases
     * @param path out put path of point cases
     * @param caseNum the cases number
     * format is:
     *                x1,y1
     *                x2,y2
     *                ...
     */
    public static void pointGenerate(String path, int caseNum) {
        BufferedWriter writer = null;
        Random random = new Random();
        try {
            writer = new BufferedWriter(new FileWriter(path));
            for (int i = 0; i < caseNum; i++) {
                double x = ((lat2 - lat1) * random.nextDouble() + lat1) / 10000000;
                double y = ((lon2 - lon1) * random.nextDouble() + lon1) / 10000000;
                writer.write(x + "," + y);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * function for generating area test cases
     * @param path out put path of area cases
     * @param caseNum the cases number
     * format is:
     *                x1,y1,x2,y2
     *                ...
     */
    public static void areaGenerate(String path, int caseNum) {
        BufferedWriter writer = null;
        Random random = new Random();
        try {
            writer = new BufferedWriter(new FileWriter(path));
            for (int i = 0; i < caseNum; i++) {
//                double x1 = ((lat2 - lat1) * random.nextDouble() + lat1);
//                double y1 = ((lon2 - lon1) * random.nextDouble() + lon1);
//                double x2 = ((lat2 - x1) * random.nextDouble() + x1) / 10000000;
//                double y2 = ((lon2 - y1) * random.nextDouble() + y1) / 10000000;
//                x1 = x1 / 10000000;
//                y1 = y1 / 10000000;
                double x1 = (100 * random.nextDouble() + 100);
                double y1 = (100 * random.nextDouble() + 100);
                double x2 = ((200 - x1) * random.nextDouble() + x1);
                double y2 = ((200 - y1) * random.nextDouble() + y1);
                writer.write(x1 + "," + y1 + "," + x2 + "," + y2);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
