package com.konfuse.road;

import java.util.Set;
import java.util.TreeSet;

/**
 * @Auther todd
 * @Date 2020/1/11
 */
public class Vertex {
    public Long id;
    public double x;
    public double y;
    public TreeSet<Long> relateEdges;

    public Vertex(Long id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        relateEdges = new TreeSet<>();
    }

    public void addRelateEdges(Road road){
        relateEdges.add(road.id());
    }

    public Set<Long> getRelateEdges() {
        return relateEdges;
    }
}
