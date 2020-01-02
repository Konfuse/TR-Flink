package com.konfuse.road;

import com.konfuse.topology.AbstractLink;

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
    private final LocationOnRoad<R> source;
    private LocationOnRoad<R> target;
    private final LinkedList<R> roads;

    public Path(LocationOnRoad<R> single) {
        this.source = single;
        this.target = single;
        this.roads = new LinkedList<>(Arrays.asList(single.road()));
        if (!valid()) {
            throw new RuntimeException("unvalid path");
        }
    }

    public Path(LocationOnRoad<R> source, LocationOnRoad<R> target, List<R> roads) {
        this.source = source;
        this.target = target;
        this.roads = new LinkedList<>(roads);
        if (!valid()) {
            throw new RuntimeException("unvalid path");
        }
    }

    /**
     * Checks if the path is valid
     * @return True if the path is valid, false otherwise.
     */
    private boolean valid() {
        if (roads.getFirst().id() != source.road().id()) {
            return false;
        }

        if (roads.getLast().id() != target.road().id()) {
            return false;
        }

        if (source.road().id() == target.road().id() && source.fraction() > target.fraction()
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
     * Gets the start/source {@link LocationOnRoad} of the path.
     *
     * @return Start/source {@link LocationOnRoad} of the path.
     */
    public LocationOnRoad<R> source() {
        return source;
    }

    /**
     * Gets end/target {@link LocationOnRoad} of the path.
     *
     * @return End/target {@link LocationOnRoad} of the path.
     */
    public LocationOnRoad<R> target() {
        return target;
    }

    /**
     * Adds a {@link Path} at the end of this path.
     *
     * @param other {@link Path} ot be added at the end of this path.
     * @return True if path can be added, i.e. result is a valid path, false otherwise.
     */
    public boolean add(Path<R> other) {
        if (target.road().id() != other.source.road().id()
                && target.road().target() != other.source.road().source()) {
            return false;
        }

        if (target.road().id() == other.source.road().id()
                && target.fraction() != other.source.fraction()) {
            return false;
        }

        if (target.road().id() != other.source.road().id()
                && (target.fraction() != 1 || other.source.fraction() != 0)) {
            return false;
        }

        if (target.road().id() != other.source.road().id()) {
            roads.add(other.roads.getFirst());
        }

        for (int i = 1; i < other.roads.size(); ++i) {
            roads.add(other.roads.get(i));
        }

        target = other.target;

        return true;
    }
}
