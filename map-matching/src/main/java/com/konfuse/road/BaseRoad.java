package com.konfuse.road;

import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.*;
import com.konfuse.geometry.Point;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * Road data structure for a road segment.
 *
 * Provides topological information, i.e. {@link BaseRoad#source()} and {@link BaseRoad#target()}),
 * road type information, i.e. {@link BaseRoad#oneway()},
 * {@link BaseRoad#priority()} and {@link BaseRoad#maxspeed(Heading)}), and geometrical information
 * (e.g. {@link BaseRoad#length()} and {@link BaseRoad#geometry()}).
 *
 * @Author: todd
 * @Date: 2019/12/31 14:16
 */
public class BaseRoad {
    private final long id;
    private final long refId;
    private final long source;
    private final long target;
    private final boolean oneway;
    private final float priority;
    private final float maxSpeedForward;
    private final float maxSpeedBackward;
    private final double length;
    private final byte[] geometry;

    /**
     * Constructs {@link BaseRoad} object.
     *
     * @param id Unique road identifier.
     * @param source Source vertex identifier (in road topology representation).
     * @param target Target vertex identifier (in road topology representation).
     * @param refId Identifier of road referring to some source data.
     * @param oneway Indicator if this road is a one-way road.
     * @param priority BaseRoad priority factor, which is greater or equal than one.
     * @param maxSpeedForward Maximum speed limit for passing this road from source to target.
     * @param maxSpeedBackward Maximum speed limit for passing this road from target to source.
     * @param length Length of road geometry in meters.
     * @param geometry BaseRoad's geometry from source to target as {@link Polyline} object.
     */
    public BaseRoad(long id, long source, long target, long refId, boolean oneway,
                    float priority, float maxSpeedForward, float maxSpeedBackward, double length,
                    Polyline geometry) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.refId = refId;
        this.oneway = oneway;
        this.priority = priority;
        this.maxSpeedForward = maxSpeedForward;
        this.maxSpeedBackward = maxSpeedBackward;
        this.length = length;
        this.geometry = OperatorExportToWkb.local()
                .execute(WkbExportFlags.wkbExportLineString, geometry, null).array();
    }

    /**
     * Constructs {@link BaseRoad} object.
     *
     * @param id Unique road identifier.
     * @param source Source vertex identifier (in road topology representation).
     * @param target Target vertex identifier (in road topology representation).
     * @param osmId Identifier of corresponding OpenStreetMap road.
     * @param oneway Indicator if this road is a one-way road.
     * @param priority BaseRoad priority factor, which is greater or equal than one.
     * @param maxSpeedForward Maximum speed limit for passing this road from source to target.
     * @param maxSpeedBackward Maximum speed limit for passing this road from target to source.
     * @param length Length of road geometry in meters.
     * @param wkb BaseRoad's geometry in WKB format from source to target.
     */
    public BaseRoad(long id, long source, long target, long osmId, boolean oneway,
                    float priority, float maxSpeedForward, float maxSpeedBackward, double length,
                    byte[] wkb) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.refId = osmId;
        this.oneway = oneway;
        this.priority = priority;
        this.maxSpeedForward = maxSpeedForward;
        this.maxSpeedBackward = maxSpeedBackward;
        this.length = length;
        this.geometry = wkb;
    }

    /**
     * Gets unique road identifier.
     *
     * @return Unique road identifier.
     */
    public long id() {
        return id;
    }

    /**
     * Gets source vertex identifier.
     *
     * @return Source vertex identifier.
     */
    public long source() {
        return source;
    }

    /**
     * Gets target vertex identifier.
     *
     * @return Target vertex identifier.
     */
    public long target() {
        return target;
    }

    /**
     * Gets identifier of road reference from the source.
     * <p>
     * <b>Note:</b> A routable road map requires splitting of roads into segments to build a road
     * topology (graph). Since OpenStreetMap roads span often roads over multiple intersections,
     * they must be split into multiple road segments. Hence, it is a one-to-many relationship.
     *
     * @return Identifier of referred OpenStreetMap road.
     */
    public long refid() {
        return refId;
    }

    /**
     * Gets a boolean if this is a one-way.
     *
     * @return True if this road is a one-way road, false otherwise.
     */
    public boolean oneway() {
        return oneway;
    }

    /**
     * Gets road's priority factor, i.e. an additional cost factor for routing, and must be greater
     * or equal to one. Higher priority factor means higher costs.
     *
     * @return BaseRoad's priority factor.
     */
    public float priority() {
        return priority;
    }

    /**
     * Gets road's maximum speed for respective heading in kilometers per hour.
     *
     * @param heading {@link Heading} for which maximum speed must be returned.
     * @return Maximum speed in kilometers per hour.
     */
    public float maxspeed(Heading heading) {
        return heading == Heading.forward ? maxSpeedForward : maxSpeedBackward;
    }

    /**
     * Gets road length in meters.
     *
     * @return BaseRoad length in meters.
     */
    public double length() {
        return length;
    }

    /**
     * Gets road's geometry as a {@link Polyline} from the road's source to its target.
     *
     * @return BaseRoad's geometry as {@link Polyline} from source to target.
     */
    public Polyline geometry() {
        return (Polyline) OperatorImportFromWkb.local().execute(WkbImportFlags.wkbImportDefaults,
                Type.Polyline, ByteBuffer.wrap(geometry), null);
    }

    /**
     * Gets road's geometry as a {@link ByteBuffer} in WKB format from the road's source to its
     * target.
     *
     * @return BaseRoad's geometry as a {@link ByteBuffer} in WKB format from the road's source to its
     *         target.
     */
    public byte[] wkb() {
        return geometry;
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
