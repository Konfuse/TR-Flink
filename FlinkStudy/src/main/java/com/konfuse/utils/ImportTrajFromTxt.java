package com.konfuse.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @Author: Konfuse
 * @Date: 19-3-13 下午10:17
 */
public class ImportTrajFromTxt {
    public void putRows() {
        String[] columnFamilies = {"position"};
//        com.traj.Util.HBaseUtil.createTable("points", columnFamilies);
        String path = "/home/konfuse/Documents/SF/trajectory";

        File[] fileList = new File(path).listFiles();

//        Connection conn = null;
        BufferedReader reader = null;
        String line = null;
        String fileId = null;
        String[] item = null;
        String timestamp = null;
        double x;
        double y;
        int count;

        try {
//            conn = com.traj.Util.HBaseUtil.init();
            for (int i = 0; i < fileList.length; i++) {
                fileId = fileList[i].toString().split("_")[1].split("\\.")[0];
                if (fileId == null)
                    continue;

                count = 0;
                reader = new BufferedReader(new FileReader(fileList[i]));
                while ((line = reader.readLine()) != null) {
                    item = line.split(";");
                    if (item.length != 3 || item[0] == null || item[1] == null)
                        continue;
                    x = Double.parseDouble(item[0]);
                    y = Double.parseDouble(item[1]);
                    timestamp = item[2];
////                  System.out.println("x = " + x + "; y = " + y + "; timestamp = " + timestamp);
//                    HBaseUtil.insertData(conn, "points", fileId, "position", "x" + count, String.valueOf(x));
//                    HBaseUtil.insertData(conn, "points", fileId, "position", "y" + count, String.valueOf(y));
//                    HBaseUtil.insertData(conn, "points", fileId, "position", "time" + count, timestamp);
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
//                HBaseUtil.closeAll(conn);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        ImportTrajFromTxt test = new ImportTrajFromTxt();
        test.putRows();
    }
}
