package com.konfuse.road;

import com.konfuse.topology.Cost;

/**
 * @Author: Konfuse
 * @Date: 2020/1/2 16:25
 */
public class DistanceCost extends Cost<Road> {
    @Override
    public double cost(Road road) {
        return road.length();
    }
}
