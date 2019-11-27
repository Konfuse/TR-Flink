package com.konfuse.bean;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @Author: Konfuse
 * @Date: 2019/11/25 23:33
 */
public class MBR implements Serializable {
    private double x1;
    private double y1;
    private double x2;
    private double y2;

    public MBR() {
        this.x1 = 0;
        this.y1 = 0;
        this.x2 = 0;
        this.y2 = 0;
    }

    public MBR(MBR mbr) {
        this.x1 = mbr.x1;
        this.y1 = mbr.y1;
        this.x2 = mbr.x2;
        this.y2 = mbr.y2;
    }

    public MBR(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public double getX1() {
        return x1;
    }

    public double getY1() {
        return y1;
    }

    public double getX2() {
        return x2;
    }

    public double getY2() {
        return y2;
    }

    public static class MBRComparator implements Comparator<Entry> {
        private int dimension;
        private boolean low;

        public MBRComparator(int dimension, boolean low) {
            this.dimension = dimension;
            this.low = low;
        }

        public int compare(Entry e1, Entry e2) {
            if (dimension == 1) {
                if (low) {
                    return Double.compare(e1.mbr.x1, e2.mbr.x1);
                } else {
                    return Double.compare(e1.mbr.x2, e2.mbr.x2);
                }
            } else {
                if (low) {
                    return Double.compare(e1.mbr.y1, e2.mbr.y1);
                } else {
                    return Double.compare(e1.mbr.y2, e2.mbr.y2);
                }
            }
        }
    }

    public double getArea() {
        return 1.0 * (y1 - y2) * (x1 - x2);
    }

    public double getMargin() {
        return 0.0F + (x2 - x1) + (y2 - y1);
    }

    public static MBR union(MBR... regions) {
        double x1 = regions[0].x1;
        double y1 = regions[0].y1;
        double x2 = regions[0].x2;
        double y2 = regions[0].y2;
        // for each dimension, find the lowest and highest values
        for (MBR region : regions) {
            if (region.x1 < x1) x1 = region.x1;
            if (region.x2 > x2) x2 = region.x2;
            if (region.y1 < y1) y1 = region.y1;
            if (region.y2 > y2) y2 = region.y2;
        }
        return new MBR(x1, y1, x2, y2);
    }

    public double enlargement(MBR mbr) {
        double xLine = x2 - x1;
        double yLine = y2 - y1;
        if (mbr.x2 > x2) xLine += (mbr.x2 - x2);
        if (mbr.x1 < x1) xLine += (x1 - mbr.x1);
        if (mbr.y2 > y2) yLine += (mbr.y2 - y2);
        if (mbr.y1 < y1) yLine += (y1 - mbr.y1);
        return xLine * yLine - getArea();
    }

    public static MBR intersection(MBR... regions) {
        double x1 = regions[0].x1;
        double y1 = regions[0].y1;
        double x2 = regions[0].x2;
        double y2 = regions[0].y2;
        for (MBR region : regions) {
            x1 = Math.max(region.x1, x1);
            x2 = Math.min(region.x2, x2);
            y1 = Math.max(region.y1, y1);
            y2 = Math.min(region.y2, y2);
        }
        return new MBR(x1, y1, x2, y2);
    }

    public static boolean intersects(MBR mbr1, MBR mbr2) {
        if (mbr2.x1 > mbr1.x2 || mbr1.x1 > mbr2.x2 || mbr2.y1 > mbr2.y2 || mbr1.y1 > mbr2.y2)
            return false;
        return true;
    }

    public boolean contains(MBR mbr) {
        if (mbr.x1 < x1 || mbr.x2 > x2 || mbr.y1 < y1 || mbr.y2 > y2)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MBR{" +
                "x1=" + x1 +
                ", y1=" + y1 +
                ", x2=" + x2 +
                ", y2=" + y2 +
                '}';
    }
}
