package com.konfuse.bean;

/**
 * @Author: Konfuse
 * @Date: 19-3-14 下午3:39
 */
public class Point {
    private double x;
    private double y;
    private String timestamp;

    public Point() {
    }

    public Point(double x, double y, String timestamp) {
        this.x = x;
        this.y = y;
        this.timestamp = timestamp;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Point{" +
                "x=" + x +
                ", y=" + y +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
