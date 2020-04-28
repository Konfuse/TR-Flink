package com.konfuse.mbe;

import com.konfuse.geometry.Point;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @Auther todd
 * @Date 2020/4/24
 */
public class MBEIO {
    public static ArrayList<MBETrajectory> readMBETrajectories(String trajectoriesFolderPath) {
        ArrayList<MBETrajectory> records = new ArrayList<>();
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
                if (gpsPoints.size() >= 10) {
                    records.add(new MBETrajectory(id, gpsPoints, MBEConfig.deltaRate, MBEConfig.epsilon));
                    ++id;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("trajectory count: " + records.size());
        return records;
    }
}
