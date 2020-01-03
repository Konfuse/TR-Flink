package com.konfuse.road;

import com.konfuse.topology.AbstractLink;
import com.konfuse.topology.LocationOnEdge;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Path of edges in a graph.
 *
 * @Author: Konfuse
 * @Date: 2020/1/1 15:08
 */
public class Path<R extends AbstractLink<R>> {
    private final LocationOnEdge<R> source;
    private LocationOnEdge<R> target;
    private final LinkedList<R> roads;

    public Path(LocationOnEdge<R> single) {
        this.source = single;
        this.target = single;
        this.roads = new LinkedList<>(Arrays.asList(single.edge()));
        if (!valid()) {
            throw new RuntimeException("unvalid path");
        }
    }

    public Path(LocationOnEdge<R> source, LocationOnEdge<R> target, List<R> roads) {
        this.source = source;
        this.target = target;
        this.roads = new LinkedList<>(roads);
//        if (!valid()) {
//            throw new RuntimeException("unvalid path");
//        }
    }

    /**
     * Checks if the path is valid
     * @return True if the path is valid, false otherwise.
     */
    private boolean valid() {
        if (roads.getFirst().id() != source.edge().id()) {
            return false;
        }

        if (roads.getLast().id() != target.edge().id()) {
            return false;
        }

        if (source.edge().id() == target.edge().id() && source.fraction() > target.fraction()
                && roads.size() == 1) {
            return false;
        }

        for (int i = 0; i < roads.size() - 1; i++) {
            Iterator<R> successors = roads.get(i).successors();
            boolean successor = false;

            while (successors.hasNext()) {
                if (successors.next().id() == roads.get(i + 1).id()) {
                    successor = true;
                }
            }

            if (!successor) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the sequence of {@link AbstractLink}s that make the path.
     *
     * @return Sequence of {@link AbstractLink}s that make the path.
     */
    public List<R> path() {
        return roads;
    }

    /**
     * Gets the start/source {@link LocationOnEdge} of the path.
     *
     * @return Start/source {@link LocationOnEdge} of the path.
     */
    public LocationOnEdge<R> source() {
        return source;
    }

    /**
     * Gets end/target {@link LocationOnEdge} of the path.
     *
     * @return End/target {@link LocationOnEdge} of the path.
     */
    public LocationOnEdge<R> target() {
        return target;
    }

    /**
     * Adds a {@link Path} at the end of this path.
     *
     * @param other {@link Path} ot be added at the end of this path.
     * @return True if path can be added, i.e. result is a valid path, false otherwise.
     */
    public boolean add(Path<R> other) {
        if (target.edge().id() != other.source.edge().id()
                && target.edge().target() != other.source.edge().source()) {
            return false;
        }

        if (target.edge().id() == other.source.edge().id()
                && target.fraction() != other.source.fraction()) {
            return false;
        }

        if (target.edge().id() != other.source.edge().id()
                && (target.fraction() != 1 || other.source.fraction() != 0)) {
            return false;
        }

        if (target.edge().id() != other.source.edge().id()) {
            roads.add(other.roads.getFirst());
        }

        for (int i = 1; i < other.roads.size(); ++i) {
            roads.add(other.roads.get(i));
        }

        target = other.target;

        return true;
    }
}
