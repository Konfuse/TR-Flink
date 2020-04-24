package com.konfuse.util;

import java.io.Serializable;

/**
 * Generic 2-tuple (tuple).
 * @param <X> Type of first element.
 * @param <Y> Type of second element.
 *
 * @Author: Konfuse
 * @Date: 2020/1/2 10:54
 */
public class Tuple<X, Y> implements Serializable {
    public X f0 = null;
    public Y f1 = null;

    /**
     * Creates a {@link Tuple} object.
     *
     * @param value0 First element.
     * @param value1 Second element.
     */
    public Tuple(X value0, Y value1) {
        this.f0 = value0;
        this.f1 = value1;
    }

    /**
     * Gets first element.
     *
     * @return First element, may be null if set to null previously.
     */
    public X one() {
        return f0;
    }

    /**
     * Gets second element.
     *
     * @return Second element, may be null if set to null previously.
     */
    public Y two() {
        return f1;
    }

    /**
     * Sets first element.
     *
     * @param value First element.
     */
    public void one(X value) {
        this.f0 = value;
    }

    /**
     * Sets second element.
     *
     * @param value Second element.
     */
    public void two(Y value) {
        this.f1 = value;
    }
}
