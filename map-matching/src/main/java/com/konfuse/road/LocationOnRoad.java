package com.konfuse.road;

import com.konfuse.topology.AbstractLink;

/**
 * Point in a directed {@link com.konfuse.topology.Graph} which is a point on an {@link AbstractLink} with a fraction in
 * the interval <i>[0,1]</i> that defines the exact position as linear interpolation along the
 * {@link AbstractLink} from its source to target.
 *
 * @Author: todd
 * @Date: 2019/12/31 14:15
 */
public class LocationOnRoad<E extends AbstractLink<E>> {
    private final E road;
    private final double fraction;

    /**
     * Creates a {@link LocationOnRoad} object by reference to an {@link AbstractLink} and an exact position
     * defined by a fraction.
     *
     * @param road {@link AbstractLink} of the point in the graph.
     * @param fraction Fraction that defines the exact position on the {@link AbstractLink}.
     */
    public LocationOnRoad(E road, double fraction) {
        this.road = road;
        this.fraction = fraction;
    }

    /**
     * Gets the {@link AbstractLink} of the point.
     *
     * @return {@link AbstractLink} of the point.
     */
    public E road() {
        return road;
    }

    /**
     * Gets the fraction of the point.
     *
     * @return Fraction of the point.
     */
    public double fraction() {
        return fraction;
    }
}
