package com.konfuse.spatial;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Polyline;
import com.konfuse.geometry.Point;
import com.konfuse.strtree.MBR;
import com.konfuse.util.Tuple;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

/**
 * spatial operations on geometries
 *
 * @Author: Konfuse
 * @Date: 2020/1/1 19:51
 */
public class Geography {
    public Point getPointInPolyLine(Polyline polyline, int index) {
        return new Point(polyline.getPoint(index).getX(), polyline.getPoint(index).getY());
    }

    /**
     * Gets the distance between two {@link Point}s <i>a</i> and <i>b</i>.
     *
     * @param a First point.
     * @param b Second point.
     * @return Distance between points in meters.
     */
    public double distance(Point a, Point b) {
        return Geodesic.WGS84.Inverse(a.getY(), a.getX(), b.getY(), b.getX()).s12;
    }

    /**
     * Gets interception point of a straight line, defined by {@link Point}s <i>a</i> and <i>b</i>,
     * intercepted by {@link Point} <i>c</i>. The interception point is described as the linearly
     * interpolated fraction <i>f</i> in the interval <i>[0,1]</i> of the line from <i>a</i> to
     * <i>b</i>. A fraction of <i>f=0</i> is the same as {@link Point} at <i>a</i> and <i>f=1</i> is
     * the same as {@link Point} <i>b</i>.
     * <p>
     * <b>Note:</b> The coordinates of the interception point can be determined by interpolation of
     * the fraction along the straight line, e.g. with
     * {@link Geography#interpolate(Point, Point, double)}.
     *
     * @param a Start point of straight line <i>a</i> to <i>b</i>.
     * @param b End point of straight line <i>a</i> to <i>b</i>.
     * @param c {@link Point} that intercepts straight line <i>a</i> to <i>b</i>.
     * @return Interception point described as the linearly interpolated fraction <i>f</i> in the
     *         interval <i>[0,1]</i> of the line from <i>a</i> to <i>b</i>.
     */
    public double intercept(Point a, Point b, Point c) {
        if (a.getX() == b.getX() && a.getY() == b.getY()) {
            return 0;
        }
        Intercept inter = new Intercept(Geodesic.WGS84);
        GeodesicData ci =
                inter.intercept(a.getY(), a.getX(), b.getY(), b.getX(), c.getY(), c.getX());
        GeodesicData ai = Geodesic.WGS84.Inverse(a.getY(), a.getX(), ci.lat2, ci.lon2);
        GeodesicData ab = Geodesic.WGS84.Inverse(a.getY(), a.getX(), b.getY(), b.getX());

        return (Math.abs(ai.azi1 - ab.azi1) < 1) ? ai.s12 / ab.s12 : (-1) * ai.s12 / ab.s12;
    }

    /**
     * Gets {@link Point} from linear interpolation of a fraction <i>f</i>, in the interval
     * <i>[0,1]</i>, on a straight line, defined by two points <i>a</i> and <i>b</i>. A fraction of
     * <i>f=0</i> is the same as {@link Point} at <i>a</i> and <i>f=1</i> is the same as
     * {@link Point} <i>b</i>.
     *
     * @param a Start point of straight line <i>a</i> to <i>b</i>.
     * @param b End point of straight line from <i>a</i> to <i>b</i>.
     * @param f Fraction <i>f</i>, in the interval <i>[0,1]</i>, to be linearly interpolated on
     *        straight line from <i>a</i> to <i>b</i>.
     * @return {@link Point} linearly interpolated from fraction <i>f</i> on a straight line of
     *         points <i>a</i> and <i>b</i>.
     */
    public Point interpolate(Point a, Point b, double f) {
        GeodesicData inv = Geodesic.WGS84.Inverse(a.getY(), a.getX(), b.getY(), b.getX());
        GeodesicData pos = Geodesic.WGS84.Line(inv.lat1, inv.lon1, inv.azi1).Position(inv.s12 * f);

        return new Point(pos.lon2, pos.lat2);
    }

    public double azimuth(Point a, Point b, double f) {
        double azi = 0;
        if (f < 0 + 1E-10) {
            azi = Geodesic.WGS84.Inverse(a.getY(), a.getX(), b.getY(), b.getX()).azi1;
        } else if (f > 1 - 1E-10) {
            azi = Geodesic.WGS84.Inverse(a.getY(), a.getX(), b.getY(), b.getX()).azi2;
        } else {
            Point c = interpolate(a, b, f);
            azi = Geodesic.WGS84.Inverse(a.getY(), a.getX(), c.getY(), c.getX()).azi2;
        }
        return azi < 0 ? azi + 360 : azi;
    }

    public double length(Polyline p) {
        double d = 0;

        for (int i = 1; i < p.getPointCount(); ++i) {
            Point a = getPointInPolyLine(p, i - 1);
            Point b = getPointInPolyLine(p, i);
            d += distance(a, b);
        }

        return d;
    }

    /**
     * Gets interception point of a {@link Polyline} intercepted by {@link Point} <i>c</i>. This is
     * analog to {@link Geography#intercept(Point, Point, Point)}. The fraction <i>f</i>
     * refers to the full length of the {@link Polyline}.
     *
     * @param p {@link Polyline} to be intercepted.
     * @param c {@link Point} that intercepts straight line <i>a</i> to <i>b</i>.
     * @return Interception point described as the linearly interpolated fraction <i>f</i> in the
     *         interval <i>[0,1]</i> of the {@link Polyline}.
     */
    public double intercept(Polyline p, Point c) {
        double d = Double.MAX_VALUE;
        Point a = getPointInPolyLine(p, 0);
        double s = 0, sf = 0, ds = 0;

        for (int i = 1; i < p.getPointCount(); ++i) {
            Point b = getPointInPolyLine(p, i);

            ds = distance(a, b);

            double f_ = intercept(a, b, c);
            f_ = (f_ > 1) ? 1 : (f_ < 0) ? 0 : f_;
            Point x = interpolate(a, b, f_);
            double d_ = distance(c, x);

            if (d_ < d) {
                sf = (f_ * ds) + s;
                d = d_;
            }

            s = s + ds;
            a = b;
        }

        return s == 0 ? 0 : sf / s;
    }

    public Tuple<Double, Double> getDistanceAndIntercept(Polyline p, Point c) {
        double d = Double.MAX_VALUE, distance = Double.MAX_VALUE;
        Point a = getPointInPolyLine(p, 0);
        double s = 0, sf = 0, ds = 0;

        for (int i = 1; i < p.getPointCount(); ++i) {
            Point b = getPointInPolyLine(p, i);

            ds = distance(a, b);

            double f_ = intercept(a, b, c);
            f_ = (f_ > 1) ? 1 : (f_ < 0) ? 0 : f_;
            Point x = interpolate(a, b, f_);
            double d_ = distance(c, x);
            double distance_ = c.calDistance(x);

            if (d_ < d) {
                sf = (f_ * ds) + s;
                d = d_;
                distance = distance_;
            }

            s = s + ds;
            a = b;
        }

        double fraction = s == 0 ? 0 : sf / s;
        return new Tuple<>(fraction, distance);
    }

    public double[] closestPointOnSegment(double x, double y, double x1, double y1, double x2, double y2) {
        double distance, offset;

        double L2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        if (L2 == 0.0) {
            distance = Math.sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));
            offset = 0.0;
            return new double[]{distance, offset, x1, y1};
        }
        double x1_x = x - x1;
        double y1_y = y - y1;
        double x1_x2 = x2 - x1;
        double y1_y2 = y2 - y1;
        double ratio = (x1_x * x1_x2 + y1_y * y1_y2) / L2;
        ratio = (ratio > 1) ? 1 : ratio;
        ratio = (ratio < 0) ? 0 : ratio;
        double prj_x = x1 + ratio * (x1_x2);
        double prj_y = y1 + ratio * (y1_y2);
//        offset = Geodesic.WGS84.Inverse(prj_y, prj_x, y1, x1).s12;
        offset = Math.sqrt((prj_x - x1) * (prj_x - x1) + (prj_y - y1) * (prj_y - y1));
        distance = Math.sqrt((prj_x - x) * (prj_x - x) + (prj_y - y) * (prj_y - y));
        return new double[]{distance, offset, prj_x, prj_y};
    }

    public double[] getDistanceAndInterceptWithLngLon(Polyline p, Point q) {
        int NPoints = p.getPointCount();
        double min_dist = Double.MAX_VALUE;
        double final_offset = Double.MAX_VALUE;
        double length_parsed = 0;
        double x = 0, y = 0;
        int i = 0;
        while (i < NPoints - 1) {
            double x1 = p.getPoint(i).getX();
            double y1 = p.getPoint(i).getY();
            double x2 = p.getPoint(i + 1).getX();
            double y2 = p.getPoint(i + 1).getY();
            double[] temp_message = closestPointOnSegment(q.getX(), q.getY(), x1, y1, x2, y2);
            double temp_min_dist = temp_message[0];
            double temp_min_offset = temp_message[1];
            if (temp_min_dist < min_dist) {
                min_dist = temp_min_dist;
                final_offset = length_parsed + temp_min_offset;
                x = temp_message[2]; y = temp_message[3];
            }
//            length_parsed += Geodesic.WGS84.Inverse(y1, x1, y2, x2).s12;
            length_parsed += Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
            ++i;
        };
        return new double[]{min_dist, final_offset, x, y};
    }

    /**
     * Gets the distance between a {@link Polyline} and a {@link Point} <i>c</i>.
     *
     * @param p {@link Polyline} .
     * @param c {@link Point}
     * @return the distance between a {@link Polyline} and a {@link Point} <i>c</i>
     */
    public double distanceBetweenPolylineAndPoint(Polyline p, Point c) {
        double d = Double.MAX_VALUE;
        Point a = getPointInPolyLine(p, 0);

        for (int i = 1; i < p.getPointCount(); ++i) {
            Point b = getPointInPolyLine(p, i);

            double f_ = intercept(a, b, c);
            f_ = (f_ > 1) ? 1 : (f_ < 0) ? 0 : f_;
            Point x = interpolate(a, b, f_);
            double d_ = distance(c, x);

            if (d_ < d) {
                d = d_;
            }
            a = b;
        }

        return d;
    }

    /**
     * Gets {@link Point} from linear interpolation of a fraction <i>f</i>, in the interval
     * <i>[0,1]</i>, on a {@link Polyline}. This is analog to
     * {@link Geography#interpolate(Point, Point, double)}.The fraction refers to the full
     * length of the {@link Polyline}.
     *
     * @param path {@link Polyline} of interpolation.
     * @param f Fraction <i>f</i>, in the interval <i>[0,1]</i>, to be linearly interpolated on
     *        {@link Polyline}.
     * @return {@link Point} linearly interpolated from fraction <i>f</i> on a {@link Polyline}.
     */
    public Point interpolate(Polyline path, double f) {
        return interpolate(path, length(path), f);
    }

    /**
     * Gets {@link Point} from linear interpolation of a fraction <i>f</i>, in the interval
     * <i>[0,1]</i>, on a {@link Polyline}. This is an extension of
     * {@link Geography#interpolate(Polyline, double)} and takes the length of the
     * {@link Polyline} as parameter reduce computational effort.
     *
     * @param p {@link Polyline} of interpolation.
     * @param l Length of the {@link Polyline} in meters.
     * @param f Fraction <i>f</i>, in the interval <i>[0,1]</i>, to be linearly interpolated on
     *        {@link Polyline}.
     * @return {@link Point} linearly interpolated from fraction <i>f</i> on a {@link Polyline}.
     */
    public Point interpolate(Polyline p, double l, double f) {
        assert (f >= 0 && f <= 1);

        Point a = getPointInPolyLine(p, 0);
        double d = l * f;
        double s = 0, ds = 0;

        if (f < 0 + 1E-10) {
            return getPointInPolyLine(p, 0);
        }

        if (f > 1 - 1E-10) {
            return getPointInPolyLine(p, p.getPointCount() - 1);
        }

        for (int i = 1; i < p.getPointCount(); ++i) {
            Point b = getPointInPolyLine(p, i);
            ds = distance(a, b);

            if ((s + ds) >= d) {
                return interpolate(a, b, (d - s) / ds);
            }

            s = s + ds;
            a = b;
        }

        return null;
    }

    public double azimuth(Polyline p, double f) {
        return azimuth(p, length(p), f);
    }

    public double azimuth(Polyline p, double l, double f) {
        assert (f >= 0 && f <= 1);

        Point a = getPointInPolyLine(p, 0);
        double d = l * f;
        double s = 0, ds = 0;

        if (f < 0 + 1E-10) {
            return azimuth(getPointInPolyLine(p, 0), getPointInPolyLine(p, 1), 0);
        }

        if (f > 1 - 1E-10) {
            return azimuth(getPointInPolyLine(p, p.getPointCount() - 2), getPointInPolyLine(p, p.getPointCount() - 1), f);
        }

        for (int i = 1; i < p.getPointCount(); ++i) {
            Point b = getPointInPolyLine(p, i);
            ds = distance(a, b);

            if ((s + ds) >= d) {
                return azimuth(a, b, (d - s) / ds);
            }

            s = s + ds;
            a = b;
        }

        return Double.NaN;
    }

    public Envelope2D envelope(Point c, double radius) {
        Envelope2D env = new Envelope2D();

        double ymax = Geodesic.WGS84.Direct(c.getY(), c.getX(), 0, radius).lat2;
        double ymin = Geodesic.WGS84.Direct(c.getY(), c.getX(), -180, radius).lat2;
        double xmax = Geodesic.WGS84.Direct(c.getY(), c.getX(), 90, radius).lon2;
        double xmin = Geodesic.WGS84.Direct(c.getY(), c.getX(), -90, radius).lon2;

        env.setCoords(xmin, ymin, xmax, ymax);

        return env;
    }

    public MBR envelopeToMBR(double x, double y, double radius) {
        double ymax = Geodesic.WGS84.Direct(y, x, 0, radius).lat2;
        double ymin = Geodesic.WGS84.Direct(y, x, -180, radius).lat2;
        double xmax = Geodesic.WGS84.Direct(y, x, 90, radius).lon2;
        double xmin = Geodesic.WGS84.Direct(y, x, -90, radius).lon2;

        return new MBR(xmin, ymin, xmax, ymax);
    }

    public double convertRadius(double x, double y, double radius) {
        double[] azis = new double[]{0, 90, -90, -180};
        double r = Double.MIN_VALUE;

        for (double azi : azis) {
            GeodesicData pos = Geodesic.WGS84.Direct(y, x, azi, radius);
            double dis = (pos.lon2 - x) * (pos.lon2 - x) + (pos.lat2 - y) * (pos.lat2 - y);
            r = Math.max(dis, r);
        }

        return Math.sqrt(r);
    }
}
