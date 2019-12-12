package com.konfuse.roadnetwork;

import com.konfuse.geometry.Point;

/**
 * @Auther todd
 * @Date 2019/12/12
 */
public class LocationOnRoad {
    /**
     * ID of the edge, on which the vehicle is positioned.
     */
    public final long linkId;
    /**
     * Position on the edge from beginning as a number in the interval [0,1].
     */
    public final double fraction;

    public final Point position;

    public LocationOnRoad(long linkId, double fraction, Point position) {
        if (fraction < 0.0 || fraction > 1.0) {
            throw new IllegalArgumentException();
        }
        this.linkId = linkId;
        this.fraction = fraction;
        this.position = position;
    }



    public LocationOnRoad(long edgeId, double fraction, double x, double y) {
        this(edgeId, fraction, new Point(0, x, y));
    }

    @Override
    public String toString() {
        return "LocationOnRoad{" +
                "linkId=" + linkId +
                ", fraction=" + fraction +
                ", position=" + position +
                '}';
    }
}
