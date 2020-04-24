package com.konfuse.dita;

import com.konfuse.geometry.Point;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.flink.types.Key;

/**
 * @Auther todd
 * @Date 2020/4/19
 */
public class DITAFile {
    public static ArrayList<DITATrajectory> readDITATrajectories(String trajectoriesFolderPath) {
        ArrayList<DITATrajectory> records = new ArrayList<>();
        File[] fileList = new File(trajectoriesFolderPath).listFiles();
        BufferedReader reader = null;
        int trajectoryCount = 0, id = 0;
        for (File file : fileList) {
            ArrayList<Point> gpsPoints = new ArrayList<>();
//            if (trajectoryCount == 1000) {
//                break;
//            }
            System.out.println("the " + (++trajectoryCount) + "th trajectory is being processed: " + file.getName());
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] items = line.split(";");
                    double x = Double.parseDouble(items[0]);
                    double y = Double.parseDouble(items[1]);
                    gpsPoints.add(new Point(x, y));
                }
                if (gpsPoints.size() >= DITAConfig.localIndexedPivotSize) {
                    records.add(new DITATrajectory(id, gpsPoints));
                    ++id;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("trajectory count: " + records.size());
        return records;
    }

    public static void writeDITATrajectory(String trajectoriesFolderPath, LinkedList<List<Point>> trajectories) {
        int count = 0;
        for (List<Point> trajectory : trajectories) {
            String trajectoryPath = trajectoriesFolderPath + "\\" + count++ + ".txt";
            writeDITATrajectory(trajectoryPath, trajectory);
        }
    }

    public static void writeDITATrajectory(String trajectoryPath, List<Point> trajectories) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(trajectoryPath));
            for (Point point : trajectories) {
                double x = point.getX();
                double y = point.getY();
                writer.write(x + ";" + y);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
