package com.konfuse.geometry;

import com.konfuse.internal.MBR;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * @Author: Konfuse
 * @Date: 2019/11/28 21:13
 */
public class Point extends DataObject implements Serializable {
    private double x;
    private double y;

    public Point(long id, String name, double x, double y) {
        super(id, name);
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public static class PointComparator implements Comparator<Point> {
        private int dimension;

        public PointComparator(int dimension) {
            this.dimension = dimension;
        }


        @Override
        public int compare(Point p1, Point p2) {
            if (dimension == 1) {
                return Double.compare(p1.x, p2.x);
            } else {
                return Double.compare(p1.y, p2.y);
            }
        }
    }

    public static MBR mbr(ArrayList<Point> points) {
        if (points.size() == 0)
            return new MBR();

        double x1 = points.get(0).x;
        double y1 = points.get(0).y;
        double x2 = points.get(0).x;
        double y2 = points.get(0).y;
        for (Point point : points) {
            if (point.x < x1) x1 = point.x;
            if (point.x > x2) x2 = point.x;
            if (point.y < y1) y1 = point.y;
            if (point.y > y2) y2 = point.y;
        }
        return new MBR(x1, y1, x2, y2);
    }
}
