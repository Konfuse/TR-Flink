package com.konfuse.topology;

import java.io.Serializable;
import java.util.Iterator;

/**
 * @Author: todd
 * @Date: 2019/12/30
 */
public abstract class AbstractLink<E extends AbstractLink<E>> implements Serializable {
    private transient E successor = null;
    private transient E neighbor = null;

    /**
     * Gets the link's identifier.
     *
     * @return Edge identifier.
     */
    public abstract long id();

    /**
     * Gets the link's source vertex.
     *
     * @return Identifier of the edge's source vertex.
     */
    public abstract long source();

    /**
     * Gets the link's target vertex.
     *
     * @return Identifier of the edge's target vertex.
     */
    public abstract long target();

    /**
     * Gets the link's successor.
     *
     * @return An edge's successor edge.
     */
    protected E successor() {
        return successor;
    }

    /**
     * Sets the link's successor.
     *
     * @param successor An edge's successor edge.
     */
    protected void successor(E successor) {
        this.successor = successor;
    }

    /**
     * Gets the link's neighbor.
     *
     * @return The edge's neighbor edge.
     */
    protected E neighbor() {
        return neighbor;
    }

    /**
     * Sets the link's neighbor.
     *
     * @param neighbor The edge's neighbor edge.
     */
    protected void neighbor(E neighbor) {
        this.neighbor = neighbor;
    }

    /**
     * Gets iterator over the link's successor edges.
     *
     * @return Iterator over the edge's successor edges.
     */
    public Iterator<E> successors() {
        return new Iterator<E>() {
            E successor = successor();
            E iterator = successor;

            @Override
            public boolean hasNext() {
                return (iterator != null);
            }

            @Override
            public E next() {
                if (iterator == null){
                    return null;
                }
                E next = iterator;
                iterator = iterator.neighbor() == successor ? null : iterator.neighbor();

                return next;
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Gets iterator over the link's successor edges.
     *
     * @return Iterator over the edge's successor edges.
     */
    public Iterator<E> nextEdges() {
        return new Iterator<E>() {
            E neighbor = neighbor();
            E iterator = neighbor;

            @Override
            public boolean hasNext() {
                return (iterator != null);
            }

            @Override
            public E next() {
                if (iterator == null){
                    return null;
                }
                E next = iterator;
                iterator = iterator.neighbor() == neighbor ? null : iterator.neighbor();

                return next;
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
