package com.konfuse.geometry;

import java.io.Serializable;

/**
 * @Author: Konfuse
 * @Date: 2019/11/28 17:54
 */
public abstract class DataObject implements Serializable {
    long id;
    String name;

    public DataObject(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract double calDistance(Point point);
}
