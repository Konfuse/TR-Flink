package com.konfuse.geometry;

import com.konfuse.internal.LeafNode;
import com.konfuse.internal.MBR;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Class representing the road segment extends DataObject and implements interface Serializable.
 * x1, y1 is an end point of the line, x2, y2 is the other end point of the line.
 *
 * @Author: Konfuse
 * @Date: 2019/11/27 12:17
 */
public class Line extends DataObject implements Serializable {
    private double x1;
    private double y1;
    private double x2;
    private double y2;

    public Line(long id, String name, double x1, double y1, double x2, double y2) {
        super(id, name);
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /**
     * Calculate the smallest circumscribed rectangle of the line.
     * minimum of x, y as the bottom left coordinate. And the maximum of x, y as
     * the upper right coordinate.
     */
    public MBR mbr() {
        return new MBR(Math.min(this.x1, this.x2), Math.min(this.y1, this.y2), Math.max(this.x1, this.x2), Math.max(this.y1, this.y2));
    }

    /**
     * Calculate the smallest circumscribed rectangles of a list of lines, respectively.
     * @param lines the list of lines
     * @return mbr[] an array of mbrs
     */
    public static MBR[] linesToMBRs(ArrayList<Line> lines) {
        MBR[] mbrs = new MBR[lines.size()];
        int i = 0;
        for (Line line : lines) {
            mbrs[i++] = line.mbr();
        }
        return mbrs;
    }

    /**
     * Union a list of lines, calculate the smallest circumscribed rectangle of them.
     * @param lines the list of lines
     */
    public static MBR unionLines(ArrayList<Line> lines) {
        return MBR.union(linesToMBRs(lines));
    }

    /**
     * Use vector cross product to judge whether two line segments intersect
     * @return true if they intersects, else false.
     */
    public static boolean intersects(Line line1, Line line2) {
        // Calculate the line segments projected on the x and y axes.
        // If any of them do not intersect, they do not intersect.
        if (
                Math.max(line1.x1, line1.x2) < Math.min(line2.x1, line2.x2) ||
                Math.max(line1.y1, line1.y2) < Math.min(line2.y1, line2.y2) ||
                Math.max(line2.x1, line2.x2) < Math.min(line1.x1, line1.x2) ||
                Math.max(line2.y1, line2.y2) < Math.min(line1.y1, line1.y2)
        )
            return false;

        //Note that that vector cross can help judge whether two line segments intersect
        double cross1 = (((line1.x1 - line2.x1) * (line2.y2 - line2.y1) - (line1.y1 - line2.y1) * (line2.x2 - line1.x1)) *
                ((line1.x2 - line2.x1) * (line2.y2 - line2.y1) - (line1.y2 - line2.y1) * (line2.x2 - line2.x1)));

        double cross2 = (((line2.x1 - line1.x1) * (line1.y2 - line1.y1) - (line2.y1 - line1.y1) * (line1.x2 - line1.x1)) *
                ((line2.x2 - line1.x1) * (line1.y2 - line1.y1) - (line2.y2 - line1.y1) * (line1.x2 - line1.x1)));

        if (cross1 > 0 || cross2 > 0) {
            return false;
        }
        return true;
    }

    /**
     * Get the endpoint of a road segment.
     * @return point[] a 2-size array containing two end points
     */
    public Point[] getEndPoints() {
        Point[] points = new Point[2];

        Point endPoint = new Point(0, "", x1, y1);
        points[0] = endPoint;

        endPoint = new Point(0, "", x2, y2);
        points[1] = endPoint;

        return points;
    }

    /**
     * The distance between a point and a line segment.
     */
    @Override
    public double calDistance(Point point) {
        double cross = (x2 - x1) * (point.getX() - x1) + (y2 - y1) * (point.getY() - y1);
        double distanceSquared = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        double r = cross / distanceSquared;
        if (r <= 0) return Math.sqrt((point.getX() - x1) * (point.getX() - x1) + (point.getY() - y1) * (point.getY() - y1));
        else if (cross >= distanceSquared) return Math.sqrt((point.getX() - x2) * (point.getX() - x2) + (point.getY() - y2) * (point.getY() - y2));
        else {
            double px = x1 + (x2 - x1) * r;
            double py = y1 + (y2 - y1) * r;
            return Math.sqrt((point.getX() - px) * (point.getX() - px) + (point.getY() - py) * (point.getY() - py));
        }
    }

    @Override
    public String toString() {
        return "Line{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", x1=" + x1 +
                ", y1=" + y1 +
                ", x2=" + x2 +
                ", y2=" + y2 +
                '}';
    }
}
