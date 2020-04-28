package com.konfuse.mbe;

import com.konfuse.geometry.Point;
import com.konfuse.strtree.MBR;
import com.konfuse.util.Tuple;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/4/24
 */
public class MBETrajectory {
    private int id;
    private int num;
    private MBE mbe;
    private List<Point> trajectoryData;

    public MBETrajectory(int id, List<Point> trajectoryData, double deltaRate, double epsilon) {
        this.id = id;
        this.trajectoryData = trajectoryData;
        this.num = trajectoryData.size();
        this.mbe = new MBE(trajectoryData, deltaRate, epsilon);

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }


    public List<Point> getTrajectoryData() {
        return trajectoryData;
    }

    private List<MBR> createTrajectoryMbrs() {
        List<MBR> mbrs = new ArrayList<>(mbe.getTrajectoryEnvelope().size());
        for (Tuple<Point, Point> pointPointTuple : mbe.getTrajectoryEnvelope()) {
            mbrs.add(new MBR(pointPointTuple.f0.getX(), pointPointTuple.f0.getY(), pointPointTuple.f1.getX(), pointPointTuple.f1.getY()));
        }
        return mbrs;
    }

    private List<MBR> createMbeMbrs() {
        List<MBR> mbrs = new ArrayList<>(trajectoryData.size());
        for (Point trajectoryDatum : trajectoryData) {
            mbrs.add(new MBR(trajectoryDatum.getX(), trajectoryDatum.getY(), trajectoryDatum.getX(), trajectoryDatum.getY()));
        }
        return mbrs;
    }

    public List<MBR> greedySplitMBE(double splitPercentage) {
        List<MBR> mbrs = createMbeMbrs();
        int num = (int)(mbrs.size() * splitPercentage);
        while(mbrs.size() > num) {
            double minVolume = Double.MAX_VALUE;
            int index = 0;
            for(int i = 0; i < mbrs.size() - 1; i++){
                double temp = volumeIncrease(mbrs.get(i), mbrs.get(i + 1));
                if (temp < minVolume) {
                    minVolume = temp;
                    index = i;
                }
            }
            MBR mbr1 = mbrs.get(index);
            MBR mbr2 = mbrs.get(index + 1);
            mbrs.remove(index);
            mbrs.remove(index);
            mbrs.add(index, mbr1.union(mbr2));
        }
        return mbrs;
    }

    public List<MBR> greedySplitTrajectory(double splitPercentage) {
        List<MBR> mbrs = createTrajectoryMbrs();
        int num = (int)(mbrs.size() * splitPercentage);
        while(mbrs.size() > num) {
            double minVolume = Double.MAX_VALUE;
            int index = 0;
            for(int i = 0; i < mbrs.size() - 1; i++){
                double temp = volumeIncrease(mbrs.get(i), mbrs.get(i + 1));
                if (temp < minVolume) {
                    minVolume = temp;
                    index = i;
                }
            }
            MBR mbr1 = mbrs.get(index);
            MBR mbr2 = mbrs.get(index + 1);
            mbrs.remove(index);
            mbrs.remove(index);
            mbrs.add(index, mbr1.union(mbr2));
        }
        return mbrs;
    }

    private double volumeIncrease(MBR mbr1, MBR mbr2) {
        double lowLon = Math.min(mbr1.getX1(), mbr2.getX1());
        double lowLat = Math.min(mbr1.getY1(), mbr2.getY1());
        double highLon = Math.max(mbr1.getX2(), mbr2.getX2());
        double highLat = Math.max(mbr1.getY2(), mbr2.getY2());
        return (highLon - lowLon) * (highLat - lowLat) - mbr1.getArea() - mbr2.getArea();
    }

    @Override
    public String toString() {
        return "MBETrajectory{" +
                "id=" + id +
                ", num=" + num +
                ", mbe=" + mbe +
                ", trajectoryData=" + trajectoryData +
                '}';
    }

    private class MBE {
        private List<Tuple<Point, Point>> trajectoryEnvelope;

        public MBE(List<Point> trajectoryData,  double deltaRate, double epsilon) {
            trajectoryEnvelope = new LinkedList<>();
            int delta = (int) deltaRate * trajectoryData.size();
            int id1, id2;
            for(int i = 0; i < trajectoryData.size(); i++) {
                double MinLon = 180, MinLat = 90, MaxLon = -180, MaxLat = -90;
                if(i - delta < 0){
                    id1 = 0;
                } else {
                    id1 = i - delta;
                }
                if(i + delta >= trajectoryData.size()){
                    id2 = trajectoryData.size() - 1;
                } else {
                    id2 = i + delta;
                }
                for(int j = id1; j <= id2; j++) {
                    MinLon = Math.min(MinLon, trajectoryData.get(j).getX() - epsilon);
                    MinLat = Math.min(MinLat, trajectoryData.get(j).getY() - epsilon);
                    MaxLon = Math.max(MaxLon, trajectoryData.get(j).getX() + epsilon);
                    MaxLat = Math.max(MaxLat, trajectoryData.get(j).getY() + epsilon);
                }
                trajectoryEnvelope.add(new Tuple<>(new Point(MinLon, MinLat), new Point(MaxLon, MaxLat)));
            }
        }

        public List<Tuple<Point, Point>> getTrajectoryEnvelope() {
            return trajectoryEnvelope;
        }

        @Override
        public String toString() {
            return "MBE{" +
                    "trajectoryEnvelope=" + trajectoryEnvelope +
                    '}';
        }
    }

}
