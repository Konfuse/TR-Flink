package com.konfuse.road;

import com.esri.core.geometry.Polyline;
import com.konfuse.geometry.Point;
import com.konfuse.topology.AbstractLink;

import java.util.LinkedList;
import java.util.List;

/**
 * Directed edge wrapper of {@link BaseRoad} objects in a directed edge map ({@link RoadMap}). *
 * <p>
 * <b>Note:</b> Since {@link Road} objects are directional representations of {@link BaseRoad}
 * objects, each {@link BaseRoad} is split into two {@link Road} objects. For that purpose, it uses
 * the identifier <i>i</i> of each {@link BaseRoad} to define identifiers of the respective
 * {@link Road} objects, where <i>i * 2</i> is the identifier of the forward directed {@link Road}
 * and <i>i * 2 + 1</i> of the backward directed {@link Road}.
 *
 * @Author: todd
 * @Date: 2020/1/1
 */
public class Road extends AbstractLink<Road> {
    private final BaseRoad base;
    private final Heading heading;

    public Road(BaseRoad base, Heading heading) {
        this.base = base;
        this.heading = heading;
    }

    public static Polyline invert(Polyline geometry) {
        Polyline reverse = new Polyline();
        int last = geometry.getPointCount() - 1;
        reverse.startPath(geometry.getPoint(last));

        for (int i = last - 1; i >= 0; --i) {
            reverse.lineTo(geometry.getPoint(i));
        }

        return reverse;
    }

    @Override
    public long id() {
        return heading == Heading.forward ? base.id() * 2 : base.id() * 2 + 1;
    }

    @Override
    public long source() {
        return heading == Heading.forward ? base.source() : base.target();
    }

    @Override
    public long target() {
        return heading == Heading.forward ? base.target() : base.source();
    }

    /**
     * Gets edge's priority factor, i.e. an additional cost factor for routing, and must be greater
     * or equal to one. Higher priority factor means higher costs.
     *
     * @return Road's priority factor.
     */
    public float priority() {
        return base.priority();
    }

    /**
     * Gets edge's maximum speed in kilometers per hour.
     *
     * @return Maximum speed in kilometers per hour.
     */
    public float maxspeed() {
        return base.maxspeed(heading);
    }

    /**
     * Gets edge length in meters.
     *
     * @return Road length in meters.
     */
    public double length() {
        return base.length();
    }

    /**
     * Gets edge {@link Heading} relative to its {@link BaseRoad}.
     *
     * @return Road's {@link Heading} relative to its {@link BaseRoad}.
     */
    public Heading heading() {
        return heading;
    }

    /**
     * Gets edge's geometry as a {@link Polyline} from the edge's source to its target.
     *
     * @return Road's geometry as {@link Polyline} from source to target.
     */
    public Polyline geometry() {
        return heading == Heading.forward ? base.geometry() : invert(base.geometry());
    }

    /**
     * Gets referred {@link BaseRoad} object.
     *
     * @return {@link BaseRoad} object.
     */
    public BaseRoad base() {
        return base;
    }

    public List<Point> getPoints() {
        Polyline polyline = geometry();
        List<Point> pointList = new LinkedList<>();
        int pointSize = polyline.getPointCount();
        for(int i = 0; i < pointSize; i++){
            pointList.add(new Point(polyline.getPoint(i).getX(), polyline.getPoint(i).getY()));
        }
        return pointList;
    }
}
