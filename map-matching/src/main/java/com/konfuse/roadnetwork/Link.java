package com.konfuse.roadnetwork;

import com.konfuse.geometry.Point;

/**
 * @Auther todd
 * @Date 2019/12/12
 */
public class Link implements Comparable<Link>{
    // The start (upstream) vertex of the link
    public Vertex start;
    // The end (downstream) vertex of the link
    public Vertex to;
    // length of the link segment in meters
    public final double length;
    // travel speed of the link segment in meters per second
    public final double speed;
    // a unique id
    public final long id;

    /**
     * Constructor for Link.
     * @param gid a unique id
     * @param from The start vertex
     * @param to The end vertex
     * @param length the length of the link
     * @param speed The speed limit on the link
     */
    public Link (long gid, Vertex from, Vertex to, double length, double speed) {
        this.id = gid;
        this.start = from;
        this.to = to;
        this.length = length;
        this.speed = speed;
    }

    public Link(Link link){
        this.start = link.start;
        this.to = link.to;
        this.id = link.id;
        this.length = link.length;
        this.speed = link.speed;
    }

    public Vertex getStart() {
        return start;
    }

    public void setStart(Vertex start) {
        this.start = start;
    }

    public Vertex getTo() {
        return to;
    }

    public void setTo(Vertex to) {
        this.to = to;
    }

    public double getLength() {
        return length;
    }

    public double getSpeed() {
        return speed;
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Link{" +
                "start=" + start +
                ", to=" + to +
                ", length=" + length +
                ", speed=" + speed +
                ", id=" + id +
                '}';
    }

    @Override
    public int compareTo(Link other) {
        if (id == other.id){
            return 0;
        }
        else if (id < other.id){
            return -1;
        }
        else{
            return 1;
        }
    }

    /**
     * squared distance between a point and the link
     * @param p a point
     * @return distance square
     */
    public double distanceSquared(Point p) {
        double distSq;
        double x1 = this.start.xy.getX();
        double y1 = this.start.xy.getY();
        double x2 = this.to.xy.getX();
        double y2 = this.to.xy.getY();
        double x = p.getX();
        double y = p.getY();
        double length = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);

        if (length == 0.0) {
            distSq = distanceSquared(x1, y1, x, y);
        } else {
            double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / length;
            if (t < 0.0) {
                distSq = distanceSquared(x1, y1, x, y);
            } else if (t > 1.0) {
                distSq = distanceSquared(x2, y2, x, y);
            } else {
                double proj_x = x1 + t * (x2 - x1);
                double proj_y = y1 + t * (y2 - y1);
                distSq = distanceSquared(proj_x, proj_y, x, y);
            }
        }
        return distSq;
    }

    /**
     * squared distance between two points
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public double distanceSquared(double x1, double y1, double x2, double y2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

}
