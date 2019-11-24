package com.konfuse.bean;

/**
 * @Auther todd
 * @Date 2019/11/24
 */

public class RoadMap {
    public long osmuid;
    public String geom;

    public RoadMap(long osmuid, String geom) {
        this.osmuid = osmuid;
        this.geom = geom;
    }

    public long getOsmuid() {
        return osmuid;
    }

    public void setOsmuid(long osmuid) {
        this.osmuid = osmuid;
    }

    public String getGeom() {
        return geom;
    }

    public void setGeom(String geom) {
        this.geom = geom;
    }

    @Override
    public String toString() {
        return "RoadMap{" +
                "osmid=" + osmuid +
                ", geom='" + geom + '\'' +
                '}';
    }
}
