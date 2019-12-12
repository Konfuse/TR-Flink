package com.konfuse.roadnetwork;

/**
 * @Auther todd
 * @Date 2019/12/12
 */
public class GeoUtils {

    // Map without any data added to it
    public Map map;

    public GeoUtils(Map map) {
        this.map = map;
    }

    /**
     * Match a point to the closest location on the map
     * @param longitude
     * @param latitude
     * @return
     */
    public LocationOnRoad mapMatch(double longitude, double latitude, Link link) {
        //Link link = link;//Search
        double xy[] = map.projector().fromLatLon(latitude, longitude);
        double [] snapResult = snap(link.start.xy.getX(), link.start.xy.getY(), link.to.xy.getX(), link.to.xy.getY(), xy[0], xy[1]);
        double distanceFromStartVertex = this.distance(snapResult[0], snapResult[1], link.start.xy.getX(), link.start.xy.getY());
        double fraction = distanceFromStartVertex / link.length;
        return new LocationOnRoad(link.id, fraction, xy[0], xy[1]);
    }
    /**
     * Find the closest point on a line segment with end points (x1, y1) and
     * (x2, y2) to a point (x ,y), a procedure called snap.
     *
     * @param x1 x-coordinate of an end point of the line segment
     * @param y1 y-coordinate of an end point of the line segment
     * @param x2 x-coordinate of another end point of the line segment
     * @param y2 y-coordinate of another end point of the line segment
     * @param x x-coordinate of the point to snap
     * @param y y-coordinate of the point to snap
     *
     * @return the closest point on the line segment.
     */
    public double[] snap(double x1, double y1, double x2, double y2, double x, double y) {
        double[] snapResult = new double[3];
        double dist;
        double length = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);

        if (length == 0.0) {
            dist = this.distance(x1, y1, x, y);
            snapResult[0] = x1;
            snapResult[1] = y1;
            snapResult[2] = dist;
        } else {
            double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / length;
            if (t < 0.0) {
                dist = distance(x1, y1, x, y);
                snapResult[0] = x1;
                snapResult[1] = y1;
                snapResult[2] = dist;
            } else if (t > 1.0) {
                dist = distance(x2, y2, x, y);
                snapResult[0] = x2;
                snapResult[1] = y2;
                snapResult[2] = dist;
            } else {
                double proj_x = x1 + t * (x2 - x1);
                double proj_y = y1 + t * (y2 - y1);
                dist = distance(proj_x, proj_y, x, y);
                snapResult[0] = proj_x;
                snapResult[1] = proj_y;
                snapResult[2] = dist;
            }
        }
        return snapResult;
    }

    /**
     * Compute the Euclidean distance between point (x1, y1) and point (x2, y2).
     */
    public double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }
}
