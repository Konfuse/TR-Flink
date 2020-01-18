package com.konfuse.emm;

import com.konfuse.road.Road;

import java.util.HashSet;
import java.util.Set;

/**
 * @Auther todd
 * @Date 2020/1/11
 */
public class Vertex {
    public Long id;
    public double x;
    public double y;
    public HashSet<Long> relateEdges;

    public Vertex(Long id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        relateEdges = new HashSet<>();
    }

    public void addRelateEdges(Road road){
        relateEdges.add(road.id());
    }

    public Set<Long> getRelateEdges() {
        return relateEdges;
    }
}
