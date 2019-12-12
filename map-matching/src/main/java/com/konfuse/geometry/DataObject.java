package com.konfuse.geometry;

import java.io.Serializable;

/**
 * Data records stored in the r-tree should extend DataObject class.
 * Every DataObject should contain two attributes: id and name.
 * If a temporary DataObject is to be instantiated, the value of id could be
 * set to 0 and name could be "".
 *
 * @Author: Konfuse
 * @Date: 2019/11/28 17:54
 */
public abstract class DataObject implements Serializable {
    long id;

    public DataObject(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Inherited class overrides this method to calculate
     * the distance from this object to a point
     * @param point query point to be calculated the distance
     */
    public abstract double calDistance(Point point);
}
