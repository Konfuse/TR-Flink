package com.konfuse.util;

import java.io.Serializable;

/**
 * Generic 3-tuple (triple).
 *
 * @param <X> Type of first element.
 * @param <Y> Type of second element.
 * @param <Z> Type of third element.
 *
 * @Author: Konfuse
 * @Date: 2020/1/2 12:40
 */
public class Triple<X, Y, Z> implements Serializable {
    public X f0 = null;
    public Y f1 = null;
    public Z f2 = null;

    /**
     * Creates a {@link Triple} object.
     *
     * @param value0 First element.
     * @param value1 Second element.
     * @param value2 Third element.
     */
    public Triple(X value0, Y value1, Z value2) {
        f0 = value0;
        f1 = value1;
        f2 = value2;
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
     * Gets third element.
     *
     * @return third element, may be null if set to null previously.
     */
    public Z three() {
        return f2;
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

    public void three(Z value) {
        this.f2 = value;
    }
}
