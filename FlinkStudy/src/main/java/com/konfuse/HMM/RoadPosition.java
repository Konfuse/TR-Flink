package com.konfuse.HMM;

import com.konfuse.bean.Point;

import java.util.Objects;

/**
 * @Auther todd
 * @Date 2019/12/11
 */
public class RoadPosition {
    public final long edgeId;

    /**
     * Position on the edge from beginning as a number in the interval [0,1].
     */
    public final double fraction;

    public final Point position;

    public RoadPosition(long edgeId, double fraction, Point position) {
        if (fraction < 0.0 || fraction > 1.0) {
            throw new IllegalArgumentException();
        }

        this.edgeId = edgeId;
        this.fraction = fraction;
        this.position = position;
    }

    public RoadPosition(long edgeId, double fraction, double x, double y) {
        this(edgeId, fraction, new Point(x, y));
    }

    @Override
    public String toString() {
        return "RoadPosition{" +
                "edgeId=" + edgeId +
                ", fraction=" + fraction +
                ", position=" + position +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        RoadPosition that = (RoadPosition) o;
        return edgeId == that.edgeId &&
                Double.compare(that.fraction, fraction) == 0 &&
                position.equals(that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(edgeId, fraction, position);
    }
}
