package com.konfuse.dita;

import com.konfuse.geometry.Point;
import com.konfuse.strtree.MBR;
import com.konfuse.util.Tuple;

import java.util.*;

/**
 * @Auther todd
 * @Date 2020/4/16
 */
public class DITATrajectory {
    private int id;
    private int num;
    private MBR mbr;
    private MBR extendedMBR;
    private List<Tuple<MBR, Integer>> cells;
    private List<Point> trajectoryData;
    private List<Point> localIndexedPivot;
    private List<Point> globalIndexedPivot;

    public DITATrajectory(int id, List<Point> trajectoryData) {
        this.id = id;
        this.trajectoryData = trajectoryData;
        this.num = trajectoryData.size();
        this.mbr = calcMAR();
        this.cells = calcCells(DITAConfig.cellThreshold);
        this.extendedMBR = calcExtendedMBR(DITAConfig.threshold);
        this.globalIndexedPivot = calcGlobalPivots();
        this.localIndexedPivot = calcLocalPivotsNeighbor(DITAConfig.localIndexedPivotSize);
    }

    public int getId() {
        return id;
    }

    public int getNum() {
        return num;
    }

    public MBR getMbr() {
        return mbr;
    }

    public MBR getExtendedMBR() {
        return extendedMBR;
    }

    public List<Tuple<MBR, Integer>> getCells() {
        return cells;
    }

    public List<Point> getTrajectoryData() {
        return trajectoryData;
    }

    public List<Point> getLocalIndexedPivot() {
        return localIndexedPivot;
    }

    public List<Point> getGlobalIndexedPivot() {
        return globalIndexedPivot;
    }

    private MBR calcMAR() {
        double minLon = -180.0, maxLon = 180.0, minLat = -90.0, maxLat = 90.0;
        for (Point point : this.trajectoryData) {
            minLon = Math.min(minLon, point.getX());
            minLat = Math.min(minLat, point.getY());
            maxLon = Math.max(maxLon, point.getX());
            maxLat = Math.max(maxLat, point.getY());

        }
        return new MBR(minLon, minLat, maxLat, maxLon);
    }

    private List<Tuple<MBR, Integer>> calcCells(double cellSize) {
        boolean findCells = true;
        ArrayList<Tuple<MBR, Integer>> Cells = new ArrayList<>();
        int countNum = 0;
        for (int i = 0; i < num; i++) {
            if (i == 0) {
                MBR initRect = new MBR(trajectoryData.get(i).getX() - cellSize / 2, trajectoryData.get(i).getY() - cellSize / 2, trajectoryData.get(i).getX() + cellSize / 2, trajectoryData.get(i).getY() + cellSize / 2);
                Cells.add(new Tuple<MBR, Integer>(initRect, 1));
            } else {
                if (Cells.get(countNum).f0.contains(trajectoryData.get(i))) {
                    Cells.get(countNum).f1++;
                } else {
                    findCells = false;
                }
                if (!findCells) {
                    MBR rect = new MBR(trajectoryData.get(i).getX() - cellSize / 2, trajectoryData.get(i).getY() - cellSize / 2, trajectoryData.get(i).getX() + cellSize / 2, trajectoryData.get(i).getY() + cellSize / 2);
                    Cells.add(new Tuple<MBR, Integer>(rect, 1));
                    countNum++;
                    findCells = true;
                }
            }
        }
        return Cells;
    }

    private MBR calcExtendedMBR(double threshold) {
        return new MBR(mbr.getX1() - threshold, mbr.getY1() - threshold, mbr.getX2() + threshold, mbr.getY2() + threshold);
    }

    private List<Point> calcGlobalPivots() {
        ArrayList<Point> globalPivots = new ArrayList<>();
        globalPivots.add(trajectoryData.get(0));
        globalPivots.add(trajectoryData.get(num - 1));
        return globalPivots;
    }

    private List<Point> calcLocalPivotsFL(int count) {
        ArrayList<Point> localPivots = new ArrayList<>();
        localPivots.add(trajectoryData.get(0));
        localPivots.add(trajectoryData.get(num - 1));
        ArrayList<IntermediateResult> results = new ArrayList<>();
        for (int i = 1; i < num - 1; i++) {
            double value = Math.max(trajectoryData.get(0).calDistance(trajectoryData.get(i)), trajectoryData.get(num - 1).calDistance(trajectoryData.get(i)));
            results.add(new IntermediateResult(i, value));
        }

        Collections.sort(results, (o1, o2) -> new Double(o2.rest).compareTo(o1.rest));

        ArrayList<IntermediateResult> subResults;
        if (num <= count) {
            subResults = results;
        } else {
            subResults = (ArrayList<IntermediateResult>) results.subList(0, count - 2);
        }

        Collections.sort(subResults, (o1, o2) -> o1.id - o2.id);

        for (IntermediateResult subResult : subResults) {
            localPivots.add(trajectoryData.get(subResult.id));
        }
        return localPivots;
    }

    private List<Point> calcLocalPivotsNeighbor(int count) {
        ArrayList<Point> localPivots = new ArrayList<>();
        localPivots.add(trajectoryData.get(0));
        localPivots.add(trajectoryData.get(num - 1));
        List<IntermediateResult> results = new ArrayList<>();
        for (int i = 1; i < num - 1; i++) {
            double value = trajectoryData.get(i - 1).calDistance(trajectoryData.get(i));
            results.add(new IntermediateResult(i, value));
        }

        Collections.sort(results, (o1, o2) -> new Double(o2.rest).compareTo(o1.rest));

        List<IntermediateResult> subResults;
        if (num <= count) {
            subResults = results;
        } else {
            subResults = (List<IntermediateResult>) results.subList(0, count - 2);
        }

        Collections.sort(subResults, (o1, o2) -> o1.id - o2.id);

        for (IntermediateResult subResult : subResults) {
            localPivots.add(trajectoryData.get(subResult.id));
        }
        return localPivots;
    }

    private List<Point> calcLocalPivotsInflection(int count) {
        ArrayList<Point> localPivots = new ArrayList<>();
        localPivots.add(trajectoryData.get(0));
        localPivots.add(trajectoryData.get(num - 1));
        ArrayList<IntermediateResult> results = new ArrayList<>();
        for (int i = 1; i < num - 1; i++) {
            double lineP1P2 = trajectoryData.get(i - 1).calDistance(trajectoryData.get(i));
            double lineP1P3 = trajectoryData.get(i).calDistance(trajectoryData.get(i + 1));
            double lineP2P3 = trajectoryData.get(i - 1).calDistance(trajectoryData.get(i + 1));
            double angleP1 = judgeZero(lineP2P3) ? 0 : Math.acos((lineP1P2 * lineP1P2 + lineP1P3 * lineP1P3 - lineP2P3 * lineP2P3) / (2 * lineP1P2 * lineP1P3));
            results.add(new IntermediateResult(i, Math.PI - angleP1));
        }

        Collections.sort(results, (o1, o2) -> new Double(o2.rest).compareTo(o1.rest));

        ArrayList<IntermediateResult> subResults;
        if (num <= count) {
            subResults = results;
        } else {
            subResults = (ArrayList<IntermediateResult>) results.subList(0, count - 2);
        }

        Collections.sort(subResults, (o1, o2) -> o1.id - o2.id);

        for (IntermediateResult subResult : subResults) {
            localPivots.add(trajectoryData.get(subResult.id));
        }
        return localPivots;
    }

    public double calcMinDistanceToMBR(MBR mbr) {
        double min_dist = Double.MAX_VALUE;
        for (int i = 0; i < num; i++) {
            if (judgeZero(min_dist)) {
                break;
            }
            min_dist = Math.min(min_dist, mbr.calculateDistanceToBorder(trajectoryData.get(i)));
        }
        return min_dist;
    }

    public boolean judgeZero(double x) {
        return x >= -1e-10 && x <= 1e-10;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DITATrajectory that = (DITATrajectory) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, num, mbr, extendedMBR, cells, trajectoryData, localIndexedPivot, globalIndexedPivot);
    }

    @Override
    public String toString() {
        return "DITATrajectory{" +
                "id=" + id +
                ", num=" + num +
                ", mbr=" + mbr +
                ", extendedMBR=" + extendedMBR +
                ", cells=" + cells +
                ", traj=" + trajectoryData +
                ", localIndexedPivot=" + localIndexedPivot +
                ", globalIndexedPivot=" + globalIndexedPivot +
                '}';
    }

    public static class IntermediateResult {
        private int id;
        private double rest;

        public IntermediateResult() {
            this.id = -1;
            this.rest = 0.0;
        }

        public IntermediateResult(int id, double rest) {
            this.id = id;
            this.rest = rest;
        }

        public long getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public double getRest() {
            return rest;
        }

        public void setRest(double rest) {
            this.rest = rest;
        }

        @Override
        public int hashCode() {
            return Objects.hash(rest);
        }

        @Override
        public String toString() {
            return "IntermediateResult{" +
                    "id=" + id +
                    ", rest=" + rest +
                    '}';
        }
    }
}
