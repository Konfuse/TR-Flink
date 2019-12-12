package com.konfuse.roadnetwork;

import com.konfuse.geometry.Point;

import java.util.Date;

/**
 * @Auther todd
 * @Date 2019/12/12
 */
public class GpsPoint {
    public final Date time;

    public Point position;

    public GpsPoint(Date time, Point position) {
        this.time = time;
        this.position = position;
    }

    public GpsPoint(Date time, double lon, double lat) {
        this(time, new Point(0, lon, lat));
    }

    @Override
    public String toString() {
        return "GpsPoint{" +
                "time=" + time +
                ", position=" + position +
                '}';
    }
}
