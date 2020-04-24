package com.konfuse.dita;

import com.konfuse.strtree.MBR;

import java.util.List;

/**
 * @Author todd
 * @Date 2020/4/17
 */
public class GlobalPartition {
    private int id;
    private MBR firstPointMbr;
    private MBR lastPointMbr;
    private List<Integer> trajectorySeqNums;

    public GlobalPartition(int id, MBR firstPointMbr, MBR lastPointMbr, List<Integer> trajectorySeqNums) {
        this.id = id;
        this.firstPointMbr = firstPointMbr;
        this.lastPointMbr = lastPointMbr;
        this.trajectorySeqNums = trajectorySeqNums;
    }

    public int getId() {
        return id;
    }

    public MBR getFirstPointMbr() {
        return firstPointMbr;
    }

    public MBR getLastPointMbr() {
        return lastPointMbr;
    }

    public List<Integer> getTrajectorySeqNums() {
        return trajectorySeqNums;
    }

    @Override
    public String toString() {
        return "GlobalPartition{" +
                "id=" + id +
                ", firstPointMbr=" + firstPointMbr +
                ", lastPointMbr=" + lastPointMbr +
                ", trajectorySeqNums=" + trajectorySeqNums +
                '}';
    }
}
