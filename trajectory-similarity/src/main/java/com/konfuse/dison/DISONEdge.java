package com.konfuse.dison;

import com.konfuse.geometry.Point;

/**
 * @Auther todd
 * @Date 2020/4/20
 */
public class DISONEdge {
    private long edgeId;
    private Point start;
    private Point end;
    private double length;

    public DISONEdge(long edgeId, Point start, Point end, double length) {
        this.edgeId = edgeId;
        this.start = start;
        this.end = end;
        this.length = length;
    }

    public long getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(long edgeId) {
        this.edgeId = edgeId;
    }

    public Point getStart() {
        return start;
    }

    public void setStart(Point start) {
        this.start = start;
    }

    public Point getEnd() {
        return end;
    }

    public void setEnd(Point end) {
        this.end = end;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    @Override
    public String toString() {
        return "DISONEdge{" +
                "edgeId=" + edgeId +
                ", start=" + start +
                ", end=" + end +
                ", length=" + length +
                '}';
    }
}
