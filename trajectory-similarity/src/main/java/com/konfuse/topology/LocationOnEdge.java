package com.konfuse.topology;

import java.io.Serializable;

/**
 * Point in a directed {@link Graph} which is a point on an {@link AbstractLink} with a fraction in
 * the interval <i>[0,1]</i> that defines the exact position as linear interpolation along the
 * {@link AbstractLink} from its source to target.
 *
 * @Author: todd
 * @Date: 2019/12/31 14:15
 */
public class LocationOnEdge<E extends AbstractLink<E>> implements Serializable {
    private final E edge;
    private final double fraction;

    /**
     * Creates a {@link LocationOnEdge} object by reference to an {@link AbstractLink} and an exact position
     * defined by a fraction.
     *
     * @param edge {@link AbstractLink} of the point in the graph.
     * @param fraction Fraction that defines the exact position on the {@link AbstractLink}.
     */
    public LocationOnEdge(E edge, double fraction) {
        this.edge = edge;
        this.fraction = fraction;
    }

    /**
     * Gets the {@link AbstractLink} of the point.
     *
     * @return {@link AbstractLink} of the point.
     */
    public E edge() {
        return edge;
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
