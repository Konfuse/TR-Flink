package com.konfuse.topology;

/**
 * @Author: Konfuse
 * @Date: 2019/12/31 15:19
 */
public class Edge extends AbstractLink<Edge> {
    private final long id;
    private final long source;
    private final long target;
    private final double length;

    /**
     * Creates an {@link Edge} object.
     *
     * @param id Edge identifier.
     * @param source Identifier of the edge's source vertex.
     * @param target Identifier of the edge's target vertex.
     */
    public Edge(long id, long source, long target, double length) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.length = length;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public long source() {
        return source;
    }

    @Override
    public long target() {
        return target;
    }

    public double length() {
        return length;
    }
}
