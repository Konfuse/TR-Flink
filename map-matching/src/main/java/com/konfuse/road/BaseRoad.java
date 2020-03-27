package com.konfuse.road;

import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.*;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * Road data structure for a edge segment.
 *
 * Provides topological information, i.e. {@link BaseRoad#source()} and {@link BaseRoad#target()}),
 * edge type information, i.e. {@link BaseRoad#oneway()},
 * {@link BaseRoad#priority()} and {@link BaseRoad#maxspeed(Heading)}), and geometrical information
 * (e.g. {@link BaseRoad#length()} and {@link BaseRoad#geometry()}).
 *
 * @Author: todd
 * @Date: 2019/12/31
 */
public class BaseRoad {
    private final long id;
    private final long refId;
    private final long source;
    private final long target;
    private final int oneway;
    private final float priority;
    private final float maxSpeedForward;
    private final float maxSpeedBackward;
    private final double length;
    private final byte[] geometry;

    /**
     * Constructs {@link BaseRoad} object.
     *
     * @param id Unique edge identifier.
     * @param source Source vertex identifier (in edge topology representation).
     * @param target Target vertex identifier (in edge topology representation).
     * @param refId Identifier of edge referring to some source data.
     * @param oneway Indicator if this edge is a one-way edge.
     * @param priority BaseRoad priority factor, which is greater or equal than one.
     * @param maxSpeedForward Maximum speed limit for passing this edge from source to target.
     * @param maxSpeedBackward Maximum speed limit for passing this edge from target to source.
     * @param length Length of edge geometry in meters.
     * @param geometry BaseRoad's geometry from source to target as {@link Polyline} object.
     */
    public BaseRoad(long id, long source, long target, long refId, int oneway,
                    float priority, float maxSpeedForward, float maxSpeedBackward, double length,
                    Polyline geometry) {
        this.id = id;
        this.refId = refId;
        this.priority = priority;
        this.maxSpeedForward = maxSpeedForward;
        this.maxSpeedBackward = maxSpeedBackward;
        this.length = length;
        if(oneway == -1){
            this.oneway = 1;
            this.source = target;
            this.target = source;
            Polyline poly = invert(geometry);
            this.geometry = OperatorExportToWkb.local().execute(WkbExportFlags.wkbExportLineString, poly, null).array();
        }else{
            this.oneway = oneway;
            this.source = source;
            this.target = target;
            this.geometry = OperatorExportToWkb.local()
                    .execute(WkbExportFlags.wkbExportLineString, geometry, null).array();
        }
    }

    /**
     * Constructs {@link BaseRoad} object.
     *
     * @param id Unique edge identifier.
     * @param source Source vertex identifier (in edge topology representation).
     * @param target Target vertex identifier (in edge topology representation).
     * @param osmId Identifier of corresponding OpenStreetMap edge.
     * @param oneway Indicator if this edge is a one-way edge.
     * @param priority BaseRoad priority factor, which is greater or equal than one.
     * @param maxSpeedForward Maximum speed limit for passing this edge from source to target.
     * @param maxSpeedBackward Maximum speed limit for passing this edge from target to source.
     * @param length Length of edge geometry in meters.
     * @param wkb BaseRoad's geometry in WKB format from source to target.
     */
    public BaseRoad(long id, long source, long target, long osmId, int oneway,
                    float priority, float maxSpeedForward, float maxSpeedBackward, double length,
                    byte[] wkb) {
        this.id = id;
        this.refId = osmId;
        this.priority = priority;
        this.maxSpeedForward = maxSpeedForward;
        this.maxSpeedBackward = maxSpeedBackward;
        this.length = length;
        if(oneway == -1) {
            this.oneway = 1;
            this.source = target;
            this.target = source;
            Polyline poly = invert((Polyline) OperatorImportFromWkb.local().execute(WkbImportFlags.wkbImportDefaults,
                    Type.Polyline, ByteBuffer.wrap(wkb), null));
            this.geometry = OperatorExportToWkb.local().execute(WkbExportFlags.wkbExportLineString, poly, null).array();
        } else {
            this.oneway = oneway;
            this.source = source;
            this.target = target;
            this.geometry = wkb;
        }
    }

    /**
     * Gets unique edge identifier.
     *
     * @return Unique edge identifier.
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
     * Gets identifier of edge reference from the source.
     * <p>
     * <b>Note:</b> A routable edge map requires splitting of roads into segments to build a edge
     * topology (graph). Since OpenStreetMap roads span often roads over multiple intersections,
     * they must be split into multiple edge segments. Hence, it is a one-to-many relationship.
     *
     * @return Identifier of referred OpenStreetMap edge.
     */
    public long refid() {
        return refId;
    }

    /**
     * Gets a boolean if this is a one-way.
     *
     * @return True if this edge is a one-way edge, false otherwise.
     */
    public int oneway() {
        return oneway;
    }

    /**
     * Gets edge's priority factor, i.e. an additional cost factor for routing, and must be greater
     * or equal to one. Higher priority factor means higher costs.
     *
     * @return BaseRoad's priority factor.
     */
    public float priority() {
        return priority;
    }

    public float maxspeedforward() {
        return maxSpeedForward;
    }

    public float maxspeedbackward() {
        return maxSpeedBackward;
    }

    /**
     * Gets edge's maximum speed for respective heading in kilometers per hour.
     *
     * @param heading {@link Heading} for which maximum speed must be returned.
     * @return Maximum speed in kilometers per hour.
     */
    public float maxspeed(Heading heading) {
        return heading == Heading.forward ? maxSpeedForward : maxSpeedBackward;
    }

    /**
     * Gets edge length in meters.
     *
     * @return BaseRoad length in meters.
     */
    public double length() {
        return length;
    }

    /**
     * Gets edge's geometry as a {@link Polyline} from the edge's source to its target.
     *
     * @return BaseRoad's geometry as {@link Polyline} from source to target.
     */
    public Polyline geometry() {
        return (Polyline) OperatorImportFromWkb.local().execute(WkbImportFlags.wkbImportDefaults,
                Type.Polyline, ByteBuffer.wrap(geometry), null);
    }

    /**
     * Gets edge's geometry as a {@link ByteBuffer} in WKB format from the edge's source to its
     * target.
     *
     * @return BaseRoad's geometry as a {@link ByteBuffer} in WKB format from the edge's source to its
     *         target.
     */
    public byte[] wkb() {
        return geometry;
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

}
