package com.konfuse.road;


import com.konfuse.geometry.Point;
import com.konfuse.spatial.Geography;
import com.konfuse.topology.LocationOnEdge;

/**
 * @Author: Konfuse
 * @Date: 2020/1/2 15:38
 */
public class RoadPoint extends LocationOnEdge<Road> {
    private static final Geography geography = new Geography();
    private final Point point;
//    private final double azimuth;

    /**
     * Creates a {@link RoadPoint}.
     *
     * @param road {@link Road} object of the point.
     * @param fraction Exact position on the {@link Road} defined as fraction <i>f</i>, with <i>0
     *        &le; f &le; 1</i>.
     */
    public RoadPoint(Road road, double fraction) {
        super(road, fraction);
        this.point = geography.interpolate(road.geometry(), fraction);
//        this.azimuth = geography.azimuth(road.geometry(), fraction);
    }

    public RoadPoint(Road road, double fraction, double x, double y) {
        super(road, fraction);
        this.point = new Point(x, y);
//        this.azimuth = geography.azimuth(road.geometry(), fraction);
    }

    /**
     * Gets the geometry of the point on the {@link Road}.
     *
     * @return Geometry of the point on the edge.
     */
    public Point point() {
        return point;
    }

//    public double azimuth() {
//        return azimuth;
//    }
}
