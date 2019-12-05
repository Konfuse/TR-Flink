package com.konfuse.geometry;

import com.konfuse.internal.MBR;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Point in geometric space extends DataObject and implements interface Serializable.
 * And attributes x, y are the coordinate of the point.
 *
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

    /**
     * Calculate the distance between two points using Euclidean distance.
     */
    @Override
    public double calDistance(Point point) {
        return Math.sqrt((x - point.x) * (x - point.x) + (y - point.y) * (y - point.y));
    }

    /**
     * Inner class PointComparator.
     * Compare points by specified dimensions.
     */
    public static class PointComparator implements Comparator<Point> {
        private int dimension;

        /**
         * @param dimension if 1, then compare points by x, else if 2, compare points by y.
         */
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

    /**
     * Union a list of points, calculate the smallest circumscribed rectangle of them.
     * @param points a list of points
     */
    public static MBR unionPoints(ArrayList<Point> points) {
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
