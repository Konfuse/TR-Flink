package com.konfuse.spatial;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

/**
 * @Author: Konfuse
 * @Date: 2020/1/1 19:51
 */
public class Geography {
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
            d += distance(p.getPoint(i - 1), p.getPoint(i));
        }

        return d;
    }

    public double intercept(Polyline p, Point c) {
        double d = Double.MAX_VALUE;
        Point a = p.getPoint(0);
        double s = 0, sf = 0, ds = 0;

        for (int i = 1; i < p.getPointCount(); ++i) {
            Point b = p.getPoint(i);

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

    public Point interpolate(Polyline path, double f) {
        return interpolate(path, length(path), f);
    }

    public Point interpolate(Polyline p, double l, double f) {
        assert (f >= 0 && f <= 1);

        Point a = p.getPoint(0);
        double d = l * f;
        double s = 0, ds = 0;

        if (f < 0 + 1E-10) {
            return p.getPoint(0);
        }

        if (f > 1 - 1E-10) {
            return p.getPoint(p.getPointCount() - 1);
        }

        for (int i = 1; i < p.getPointCount(); ++i) {
            Point b = p.getPoint(i);
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

        Point a = p.getPoint(0);
        double d = l * f;
        double s = 0, ds = 0;

        if (f < 0 + 1E-10) {
            return azimuth(p.getPoint(0), p.getPoint(1), 0);
        }

        if (f > 1 - 1E-10) {
            return azimuth(p.getPoint(p.getPointCount() - 2), p.getPoint(p.getPointCount() - 1), f);
        }

        for (int i = 1; i < p.getPointCount(); ++i) {
            Point b = p.getPoint(i);
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
}
