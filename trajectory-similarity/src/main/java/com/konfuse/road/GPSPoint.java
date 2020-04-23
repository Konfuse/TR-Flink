package com.konfuse.road;

import com.konfuse.geometry.Point;

/**
 * @Author: todd
 * @Date: 2019/12/31 15:22
 */
public class GPSPoint {
    private final long time;
    private Point position;

    public GPSPoint(long time, Point position) {
        this.time = time;
        this.position = position;
    }

    public GPSPoint(long time, double x, double y) {
        this.time = time;
        this.position = new Point(0, x, y);
    }

    public long getTime() {
        return time;
    }

    public Point getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "GPSPoint{" +
                "time=" + time +
                ", position=" + position +
                '}';
    }
}
