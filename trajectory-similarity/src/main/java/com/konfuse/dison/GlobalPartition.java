package com.konfuse.dison;

import java.io.Serializable;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/4/20
 */
public class GlobalPartition implements Serializable {
    private int id;
    private List<Integer> trajectorySeqNums;

    public GlobalPartition(int id, List<Integer> trajectorySeqNums) {
        this.id = id;
        this.trajectorySeqNums = trajectorySeqNums;
    }

    public int getId() {
        return id;
    }

    public List<Integer> getTrajectorySeqNums() {
        return trajectorySeqNums;
    }

    @Override
    public String toString() {
        return "GlobalPartition{" +
                "id=" + id +
                ", trajectorySeqNums=" + trajectorySeqNums +
                '}';
    }
}
