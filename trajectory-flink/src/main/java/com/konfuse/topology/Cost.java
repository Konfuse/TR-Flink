package com.konfuse.topology;

/**
 * @Author: Konfuse
 * @Date: 2020/1/2 14:09
 */
public abstract class Cost<E extends AbstractLink<E>> {
    public abstract double cost(E edge);

    public double cost(E edge, double fraction) {
        return cost(edge) * fraction;
    }
}
