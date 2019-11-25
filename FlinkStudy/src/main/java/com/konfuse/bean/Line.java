package com.konfuse.bean;

/**
 * @Author: Konfuse
 * @Date: 2019/11/25 15:46
 */
public class Line {
    private final String name;
    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;

    public Line(String name, double x1, double y1, double x2, double y2) {
        this.name = name;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public String toString() {
        return "Line{" +
                "name='" + name + '\'' +
                ", x1=" + x1 +
                ", y1=" + y1 +
                ", x2=" + x2 +
                ", y2=" + y2 +
                '}';
    }
}
