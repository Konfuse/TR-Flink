package com.konfuse.bean;

/**
 * @Auther todd
 * @Date 2019/11/24
 */
public class Road {
    public long osm_id;
    public Point source;
    public Point target;

    public Road(long osm_id, Point source, Point target) {
        this.osm_id = osm_id;
        this.source = source;
        this.target = target;
    }

    public long getOsm_id() {
        return osm_id;
    }

    public void setOsm_id(long osm_id) {
        this.osm_id = osm_id;
    }

    public Point getSource() {
        return source;
    }

    public void setSource(Point source) {
        this.source = source;
    }

    public Point getTarget() {
        return target;
    }

    public void setTarget(Point target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "RoadMap{" +
                "osm_id=" + osm_id +
                ", source=" + source +
                ", target=" + target +
                '}';
    }
}
