package com.konfuse.roadnetwork;

import com.konfuse.geometry.Point;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @Auther todd
 * @Date 2019/12/12
 */
public class Vertex implements Comparable<Vertex>{
    //longitude, latitude
    final public double longitude, latitude;
    // projected 2D coordinates
    final public Point xy;
    // a unique id
    public long id;
    // The links that start at this vertex, i.e., the links for which this vertex is
    // the upstream vertex, also called outgoing links.
    public Map<Vertex, Link> linksStart = new TreeMap<>();

    // The links that end at this vertex, i.e., the links for which this vertex is
    // the downstream vertex, also called incoming roads.
    public Map<Vertex, Link> linksEnd = new TreeMap<>();

    // the index used to look up the shortest travel time path table (pathTable) in Map
    public Integer pathTableIndex;

    public Vertex(double longitude, double latitude, double x, double y, long osm_id) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.xy = new Point(osm_id, x, y);
        this.id = osm_id;
    }

    public Vertex(Vertex vertex){
        this.latitude = vertex.latitude;
        this.longitude = vertex.longitude;
        this.xy = new Point(vertex.xy.getId(), vertex.xy.getX(), vertex.xy.getY());
        this.id = vertex.id;
        this.pathTableIndex = vertex.pathTableIndex;
    }

    public int getPathTableIndex() {
        return pathTableIndex;
    }

    public void setPathTableIndex(int pathTableIndex) {
        this.pathTableIndex = pathTableIndex;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Adds an edge () from this vertex to a specified vertex
     * with a specified distance and speed limit.
     * @param id the unique id
     * @param to The vertex the  goes to
     * @param distance The distance of the  (generally just the distance between the two vertices
     * @param speed The speed limit on the  between the vertices
     */
    public void addEdge (long id, Vertex to, double distance, double speed) {
        if (this.id == to.id) {
            return;
        }
        Link link = new Link(id, this, to, distance, speed);
        linksStart.put(to, link);
        to.linksEnd.put(this, link);
    }

    /**
     * Removes the edge () between this vertex and the specified vertex
     *
     * @param inter The vertex that the removed  goes to
     * @throws IllegalArgumentException if there is not  between this
     *          vertex and the specified vertex
     */
    public void removeEdge (Vertex inter) throws IllegalArgumentException {
        for (Vertex i : linksStart.keySet()) {
            if (i.equals(inter)) {
                linksStart.remove(i);
                i.linksEnd.remove(this);
                return;
            }
        }
        throw new IllegalArgumentException("Trying to remove " +
                " that doesn't exist.");
    }

    /**
     * Checks if this vertex and the specified vertex are neighbors,
     * i.e. if there is a  from this to the other or from the other to this.
     *
     * @param i The vertex to check
     * @return true if there is a  between this and i and false otherwise.
     */
    public boolean isAdjacent (Vertex i) {
        return (linksStart.keySet().contains(i) || linksEnd.keySet().contains(i));
    }

    /**
     * Return the from this vertex to the specified vertex.
     *
     * @param i The vertex that the  goes to
     * @return The  between this and the other vertex
     * @throws IllegalArgumentException if there is no  between this and
     *          the other
     */
    public Link To (Vertex i) throws IllegalArgumentException {
        if (linksStart.keySet().contains(i)) {
            return linksStart.get(i);
        }
        throw new IllegalArgumentException("no  between " +
                "this and i");
    }

    /**
     * Return a set of all the links going from this vertex to some other vertex
     *
     * @return a set of links from this vertex to other vertices
     */
    public Set<Link> getLinksStart () {
        return new TreeSet<>(linksStart.values());
    }

    /**
     * Return a set of all the links going from some vertex to this vertex
     *
     * @return a set of links going to this vertex
     */
    public Set<Link> getLinksTo () {
        return new TreeSet<>(linksEnd.values());
    }

    /**
     * Return a set of Vertices that you can directly go to from
     * this vertex, i.e. there exists a  from this vertex
     * to every vertex in the returned set
     *
     * @return a set of vertices that you can go to from this vertex
     */
    public Set<Vertex> getAdjacentFrom () {
        return linksStart.keySet();
    }

    /**
     * Return a set of Vertices from which you can directly go to
     * this vertex, i.e. there exists a  from every vertex
     * in the returned set to this vertex
     *
     * @return a set of vertices from which you can directly go to
     *          this vertex
     */
    public Set<Vertex> getAdjacentTo () {
        return linksEnd.keySet();
    }

    /**
     * Checks if the given vertex is the same as this vertex
     *
     * @param inter The given vertex to check
     * @return true if the given vertex is the same as this vertex
     */
    public boolean equals(Vertex inter) {
        return (inter.id == this.id);
    }

    /**
     * returns the Euclidean distance from this vertex to the specified vertex
     *
     * @param vertex specified vertex
     * @return distance between this vertex and specified vertex
     */
    public double distanceTo(Vertex vertex) {
        return xy.calDistance(vertex.xy);
    }

    @Override
    public String toString() {
        return "Vertex{" +
                "longitude=" + longitude +
                ", latitude=" + latitude +
                ", xy=" + xy +
                ", id=" + id +
                ", linksStart=" + linksStart +
                ", linksEnd=" + linksEnd +
                '}';
    }

    @Override
    public int compareTo(Vertex other) {
        if (this.id == other.id){
            return 0;
        }
        else if (this.id < other.id){
            return -1;
        }
        else{
            return 1;
        }
    }


}
