package com.konfuse.HMM;

import com.konfuse.bean.Point;

import java.util.Date;

/**
 * @Auther todd
 * @Date 2019/12/11
 */
public class GPSMeasurement {
    public final Date time;

    public Point position;

    public GPSMeasurement(Date time, Point position) {
        this.time = time;
        this.position = position;
    }

    public GPSMeasurement(double lon, double lat, Date time) {
        this(time, new Point(lon, lat));
    }

    @Override
    public String toString() {
        return "GpsMeasurement [time=" + time + ", position=" + position + "]";
    }


}
