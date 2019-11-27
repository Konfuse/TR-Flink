package com.konfuse.bean;

import java.io.Serializable;

/**
 * @Author: Konfuse
 * @Date: 2019/11/27 12:17
 */
public class Line implements Serializable {
    private String name;
    private double x1;
    private double y1;
    private double x2;
    private double y2;

    public Line(String name, double x1, double y1, double x2, double y2) {
        this.name = name;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public MBR mbr() {
        return new MBR(Math.min(this.x1, this.x2), Math.min(this.y1, this.y2), Math.max(this.x1, this.x2), Math.max(this.y1, this.y2));
    }

    public LeafNode toLeafNode() {
        return new LeafNode(name, mbr());
    }
}
