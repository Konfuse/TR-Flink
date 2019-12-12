package com.konfuse.roadnetwork;

import com.konfuse.Index;
import com.konfuse.geometry.Line;
import org.apache.flink.shaded.curator.org.apache.curator.shaded.com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * @Auther todd
 * @Date 2019/12/12
 */
public class Map {
    // A projector to convert between lat,lon coordinates and xy coordinates.
    private GeoProjector projector;
    //index for map matching
    private Index<Line> index;
    // A mapping from all the vertex ids to corresponding vertexes
    private HashMap<Long, Vertex> vertexes;
    // A mapping from all the vertex ids to corresponding vertexes
    private HashMap<Long, Link> links;
    // Shortest travel-time path table.
    private ImmutableList<ImmutableList<PathTableEntry>> immutablePathTable;
    // A map from an intersection's path table index to the vertex itself.
    private HashMap<Integer, Vertex> vertexesByPathTableIndex;

    private class PathTableEntry {
        final double distance;
        final int predecessor;

        PathTableEntry(double distance, int predecessor) {
            this.distance = distance;
            this.predecessor = predecessor;
        }
    }

    private class DijkstraQueueEntry implements Comparable<DijkstraQueueEntry> {
        Vertex vertex;
        double cost = Double.MAX_VALUE;
        boolean inQueue = true;

        DijkstraQueueEntry(Vertex vertex) {
            this.vertex = vertex;
        }

        @Override
        public int compareTo(DijkstraQueueEntry j) {
            if (this.cost < j.cost) {
                return -1;
            } else if (this.cost > j.cost) {
                return 1;
            } else if (this.vertex.id < j.vertex.id) {
                return -1;
            } else if (this.vertex.id > j.vertex.id) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Create an empty CityMap object for making a copy.
     */
    public Map() {}

    /*
     * Constructor of CityMap
     */
    public Map(HashMap<Long, Vertex> vertexes, HashMap<Long, Link> links, GeoProjector projector, Index<Line> index) {
        this.vertexes = vertexes;
        this.projector = projector;
        this.index = index;
        this.links = links;

        // setup pathTableIndex for every intersection
        vertexesByPathTableIndex = new HashMap<Integer, Vertex>();
            Integer pathTableIndex = 0;
        for (Vertex vertex : vertexes.values()) {
            vertex.setPathTableIndex(pathTableIndex++);
            vertexesByPathTableIndex.put(vertex.getPathTableIndex(), vertex);
        }

    }

    /**
     * Gets the time it takes to move from one vertex to the next
     * vertex. This assumes traversal at pre-defined travel speed of the roads.
     *
     * @param source The vertex to depart from
     * @param destination The vertex to arrive at
     * @return the distance it takes to go from source to destination
     */
    public double shortestRouteBetween (Vertex source, Vertex destination) {
        return immutablePathTable.get(source.pathTableIndex).get(destination.pathTableIndex).distance;
    }

    /**
     * Gets the time it takes to move from a location on a first road to a location on a second road.
     * This assumes traversal at speedlimit of the roads.
     *
     * @param source The location to depart from
     * @param destination The location to arrive at
     * @return the time in seconds it takes to go from source to destination
     */
    public double shortestDistanceBetween (LocationOnRoad source, LocationOnRoad destination) {
        double routeDistance = 0;
        if (source.linkId  == destination.linkId && source.fraction <= destination.fraction) {
            // If the two locations are on the same road and source is closer to the start intersection than destination,
            // then the distance is the difference of distance between source and destination.
            routeDistance = (destination.fraction - source.fraction)* links.get(source.linkId).length;
        } else {
            double distanceToEndVertexOfSource = (1 - source.fraction) * links.get(source.linkId).length;
            double distanceFromStartVertexOfDestination = destination.fraction * links.get(destination.linkId).length;
            double travelTimeFromEndVertexOfSourceToStartVertexOfDestination = shortestRouteBetween(links.get(source.linkId).to, links.get(destination.linkId).start);
            routeDistance = distanceToEndVertexOfSource + distanceFromStartVertexOfDestination + travelTimeFromEndVertexOfSourceToStartVertexOfDestination;
        }
        return routeDistance;
    }

    /**
     * @return { @code projector }
     */
    public GeoProjector projector() {
        return projector;
    }

    /**
     * Compute all-pair distances. This is done by computing one-to-all shortest distance
     * from each intersection using Dijkstra.
     */
    public void calcAllDistances() {
        ArrayList<ArrayList<PathTableEntry>> pathTable;

        // initialize path table
        pathTable = new ArrayList<ArrayList<PathTableEntry>>();
        for (int i = 0; i < vertexes.size(); i++) {
            ArrayList<PathTableEntry> aList = new ArrayList<PathTableEntry>();
            for (int j = 0; j < vertexes.size(); j++) {
                aList.add(null);
            }
            pathTable.add(aList);
        }

        // creates a queue entry for each intersection
        HashMap<Vertex, DijkstraQueueEntry> queueEntry = new HashMap<>();
        for (Vertex i : vertexes.values()) {
            queueEntry.put(i, new DijkstraQueueEntry(i));
        }

        for (Vertex source : vertexes.values()) {
            // 'reset' every queue entry
            for (DijkstraQueueEntry entry : queueEntry.values()) {
                entry.cost = Double.MAX_VALUE;
                entry.inQueue = true;
            }

            // source is set at distance 0
            DijkstraQueueEntry sourceEntry = queueEntry.get(source);
            sourceEntry.cost = 0;
            pathTable.get(source.pathTableIndex).set(source.pathTableIndex, new PathTableEntry(0.0, source.pathTableIndex));

            PriorityQueue<DijkstraQueueEntry> queue = new PriorityQueue<>(queueEntry.values());

            while (!queue.isEmpty()) {
                DijkstraQueueEntry entry = queue.poll();
                entry.inQueue = false;

                for (Link r : entry.vertex.getLinksStart()) {
                    DijkstraQueueEntry v = queueEntry.get(r.to);
                    if (!v.inQueue){
                        continue;
                    }
                    double ncost = entry.cost + r.length;
                    if (v.cost > ncost) {
                        queue.remove(v);
                        v.cost = ncost;
                        pathTable.get(source.pathTableIndex).set(v.vertex.pathTableIndex, new PathTableEntry(v.cost, entry.vertex.pathTableIndex));
                        queue.add(v);
                    }
                }
            }
        }

        // Make the path table unmodifiable
        makePathTableUnmodifiable(pathTable);
    }

    /**
     * Make a path table unmodifiable.
     * @param pathTable
     */
    public void makePathTableUnmodifiable(ArrayList<ArrayList<PathTableEntry>> pathTable) {
        ArrayList<ImmutableList<PathTableEntry>> aListOfImmutableList = new ArrayList<ImmutableList<PathTableEntry>>();
        for (int i = 0; i < vertexes.size(); i++) {
            ArrayList<PathTableEntry> aListOfPathTableEntries = pathTable.get(i);
            aListOfImmutableList.add(ImmutableList.copyOf(aListOfPathTableEntries));
            aListOfPathTableEntries.clear();
        }
        pathTable.clear();

        immutablePathTable = ImmutableList.copyOf(aListOfImmutableList);
    }

    /**
     * Get the shortest path between a given source and a given destination
     * @param source the source intersection
     * @param destination the destination intersection
     * @return LinkedList<Intersection> an ordered list of vertexes forming the path
     */
    public LinkedList<Vertex> shortestDistancePath(Vertex source, Vertex destination) {
        LinkedList<Vertex> path = new LinkedList<Vertex>();
        path.addFirst(destination);
        int current = destination.pathTableIndex;
        while (current != source.pathTableIndex) {
            int pred = immutablePathTable.get(source.pathTableIndex).get(current).predecessor;
            path.addFirst(vertexesByPathTableIndex.get(pred));
            current = pred;
        }
        return path;
    }

    public GeoProjector getProjector() {
        return projector;
    }

    public HashMap<Long, Vertex> getVertexes() {
        return vertexes;
    }

    public HashMap<Long, Link> getLinks() {
        return links;
    }
}
