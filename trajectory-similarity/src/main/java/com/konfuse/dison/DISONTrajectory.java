package com.konfuse.dison;

import com.konfuse.geometry.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/4/20
 */
public class DISONTrajectory implements Serializable {
    private int id;
    private double length;
    private List<Point> globalIndexedPivot;
    private List<DISONEdge> trajectoryData;

    public DISONTrajectory(int id) {
        this.id = id;
        this.length = 0;
        this.globalIndexedPivot = new ArrayList<>();
        this.trajectoryData = new ArrayList<>();
    }

    public DISONTrajectory(int id, double length, List<DISONEdge> trajectoryData, List<Point> globalIndexedPivot) {
        this.id = id;
        this.length = length;
        this.globalIndexedPivot = globalIndexedPivot;
        this.trajectoryData = trajectoryData;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLength() {
        return length;
    }

    public List<Point> getGlobalIndexedPivot() {
        return globalIndexedPivot;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public List<DISONEdge> getTrajectoryData() {
        return trajectoryData;
    }

    public void setTrajectoryData(List<DISONEdge> trajectoryData) {
        this.trajectoryData = trajectoryData;
        globalIndexedPivot.add(trajectoryData.get(0).getStart());
        globalIndexedPivot.add(trajectoryData.get(trajectoryData.size() - 1).getEnd());
        for (DISONEdge trajectoryDatum : trajectoryData) {
            this.length += trajectoryDatum.getLength();
        }
    }

    @Override
    public String toString() {
        return "DISONTrajectory{" +
                "id=" + id +
                ", trajectoryData=" + trajectoryData +
                '}';
    }
}
