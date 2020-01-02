package com.konfuse.topology;

import com.konfuse.road.Road;

/**
 * @Author: Konfuse
 * @Date: 2020/1/2 14:09
 */
public class Cost {
    public double cost(Road road){
        return road.length();
    }

    public double cost(Road road, double fraction) {
        return cost(road) * fraction;
    }
}
