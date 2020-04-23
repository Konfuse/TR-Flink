package com.konfuse.util;

import java.io.Serializable;

/**
 * @Author: Konfuse
 * @Date: 2020/1/2 16:14
 */
public class Quadruple<A, B, C, D> implements Serializable {
    public A f0 = null;
    public B f1 = null;
    public C f2 = null;
    public D f3 = null;

    /**
     * Creates a {@link Triple} object.
     *
     * @param value0 First element.
     * @param value1 Second element.
     * @param value2 Third element.
     */
    public Quadruple(A value0, B value1, C value2, D value3) {
        f0 = value0;
        f1 = value1;
        f2 = value2;
        f3 = value3;
    }

    /**
     * Gets first element.
     *
     * @return First element, may be null if set to null previously.
     */
    public A one() {
        return f0;
    }

    /**
     * Gets second element.
     *
     * @return Second element, may be null if set to null previously.
     */
    public B two() {
        return f1;
    }

    /**
     * Gets third element.
     *
     * @return third element, may be null if set to null previously.
     */
    public C three() {
        return f2;
    }

    /**
     * Gets fourth element.
     *
     * @return Fourth element, may be null if set to null previously.
     */
    public D four() {
        return f3;
    }

    /**
     * Sets first element.
     *
     * @param value First element.
     */
    public void one(A value) {
        this.f0 = value;
    }

    /**
     * Sets second element.
     *
     * @param value Second element.
     */
    public void two(B value) {
        this.f1 = value;
    }

    public void three(C value) {
        this.f2 = value;
    }

    public void four(D value) {
        this.f3 = value;
    }
}
