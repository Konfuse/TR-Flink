package com.konfuse.geometry;

import com.konfuse.internal.MBR;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.types.Key;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Point in geometric space extends DataObject and implements interface Serializable.
 * And attributes x, y are the coordinate of the point.
 *
 * Point should also implements interface org.apache.flink.types.Key.
 * It makes a point object as a key in KeySelector when divide partitions
 *
 * @Author: Konfuse
 * @Date: 2019/11/28 21:13
 */
public class Point extends DataObject implements Key<Point>, Serializable {
    private double x;
    private double y;

    public Point() {
        super(0);
    }

    public Point(long id, double x, double y) {
        super(id);
        this.x = x;
        this.y = y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
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

    @Override
    public String toString() {
        return "Point{" +
                "x=" + x +
                ", y=" + y +
                ", id=" + id +
                '}';
    }

    /**
     * If two points are the same position in space, return 0,
     * else return -1.
     */
    @Override
    public int compareTo(Point point) {
        if (point.getX() == x && point.getY() == y)
            return 0;
        else return -1;
    }

    @Override
    public void write(DataOutputView dataOutputView) throws IOException {
        dataOutputView.writeLong(this.id);
        dataOutputView.writeDouble(x);
        dataOutputView.writeDouble(y);
    }

    @Override
    public void read(DataInputView dataInputView) throws IOException {
        this.id = dataInputView.readLong();
        this.x = dataInputView.readDouble();
        this.y = dataInputView.readDouble();
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
