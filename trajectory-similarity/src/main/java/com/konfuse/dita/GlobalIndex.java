package com.konfuse.dita;

import akka.stream.javadsl.Partition;
import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.Point;
import com.konfuse.geometry.Rectangle;
import com.konfuse.strtree.IndexBuilder;
import com.konfuse.strtree.MBR;
import com.konfuse.strtree.RTree;
import com.konfuse.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther todd
 * @Date 2020/4/18
 */
public class GlobalIndex {

    private final int globalPartitionNum;

    private ArrayList<GlobalPartition> globalPartitions;
    private RTree STRtreeFirst;
    private RTree STRtreeLast;

    public GlobalIndex(int globalPartitionNum) {
        this.globalPartitionNum = globalPartitionNum;
        this.globalPartitions = new ArrayList<>();
    }

    public RTree getSTRtreeFirst() {
        return STRtreeFirst;
    }

    public RTree getSTRtreeLast() {
        return STRtreeLast;
    }

    public ArrayList<GlobalPartition> getGlobalPartitions() {
        return globalPartitions;
    }

    public void buildGlobalIndex(List<DITATrajectory> trajectories) {
        ArrayList<LinkedList<Integer>> allPartitionResults = new ArrayList<>();
        LinkedList<LinkedList<Integer>> buckets = STRFirstPartition(trajectories);
        for (LinkedList<Integer> bucket : buckets) {
            System.out.println("Bucket size: " + bucket.size());
            LinkedList<LinkedList<Integer>> subBuckets = STRLastPartition(trajectories, bucket);
            System.out.println("subBucket num: " + subBuckets.size());
            for (LinkedList<Integer> subBucket : subBuckets) {
                allPartitionResults.add(subBucket);
                System.out.println("subBucket size: " + subBucket.size());
            }
        }
        ArrayList<Rectangle> firstPointRectangles = new ArrayList<>();
        ArrayList<Rectangle> lastPointRectangles = new ArrayList<>();
        for (int i = 0; i < allPartitionResults.size(); i++) {
            Rectangle[] mbrs = calcPartitionBound(trajectories, allPartitionResults.get(i), i);
            firstPointRectangles.add(mbrs[0]);
            lastPointRectangles.add(mbrs[1]);
            MBR firstPointMbr = new MBR(mbrs[0].getMBR().getX1(), mbrs[0].getMBR().getY1(), mbrs[0].getMBR().getX2(), mbrs[0].getMBR().getY2());
            MBR lastPointMbr = new MBR(mbrs[1].getMBR().getX1(), mbrs[1].getMBR().getY1(), mbrs[1].getMBR().getX2(), mbrs[1].getMBR().getY2());
            GlobalPartition curPartition = new GlobalPartition(i, firstPointMbr, lastPointMbr, allPartitionResults.get(i));
            globalPartitions.add(curPartition);
        }
        STRtreeFirst = new IndexBuilder().createRTreeBySTR(firstPointRectangles.toArray(new Rectangle[firstPointRectangles.size()]));
        STRtreeLast = new IndexBuilder().createRTreeBySTR(lastPointRectangles.toArray(new Rectangle[lastPointRectangles.size()]));
    }

    public LinkedList<Integer> search(DITATrajectory query, double threshold) {
        HashSet<Integer> results = new HashSet<>();
        MBR query1 = new MBR(query.getGlobalIndexedPivot().get(0).getX() - threshold, query.getGlobalIndexedPivot().get(0).getY() - threshold, query.getGlobalIndexedPivot().get(0).getX() + threshold, query.getGlobalIndexedPivot().get(0).getY() + threshold);
        MBR query2 = new MBR(query.getGlobalIndexedPivot().get(1).getX() - threshold, query.getGlobalIndexedPivot().get(1).getY() - threshold, query.getGlobalIndexedPivot().get(1).getX() + threshold, query.getGlobalIndexedPivot().get(1).getY() + threshold);
        ArrayList<DataObject> dataObjectsFirstPoint = STRtreeFirst.boxRangeQuery(query1);
        ArrayList<DataObject> dataObjectsLastPoint = STRtreeLast.boxRangeQuery(query2);
        for (int i = 0; i < dataObjectsFirstPoint.size(); i++) {
            if (((Rectangle) dataObjectsFirstPoint.get(i)).calDistance(query.getGlobalIndexedPivot().get(0)) < threshold) {
                results.add((int)dataObjectsFirstPoint.get(i).getId());
            }
        }
        for (int i = 0; i < dataObjectsLastPoint.size(); i++) {
            if (((Rectangle) dataObjectsLastPoint.get(i)).calDistance(query.getGlobalIndexedPivot().get(1)) < threshold) {
                results.add((int)dataObjectsLastPoint.get(i).getId());
            }
        }
        return new LinkedList<>(results);
    }

    private Rectangle[] calcPartitionBound(List<DITATrajectory> trajectories, LinkedList<Integer> data, int id) {
        double firstPointMinLon = 180, firstPointMinLat = 90, firstPointMaxLon = -180, firstPointMaxLat = -90;
        double lastPointMinLon = 180, lastPointMinLat = 90, lastPointMaxLon = -180, lastPointMaxLat = -90;
        for (Integer trajectorySeqNum : data) {
            double firstPointLon = trajectories.get(trajectorySeqNum).getGlobalIndexedPivot().get(0).getX();
            double firstPointLat = trajectories.get(trajectorySeqNum).getGlobalIndexedPivot().get(0).getY();
            double lastPointLon = trajectories.get(trajectorySeqNum).getGlobalIndexedPivot().get(1).getX();
            double lastPointLat = trajectories.get(trajectorySeqNum).getGlobalIndexedPivot().get(1).getY();
            firstPointMinLon = Math.min(firstPointMinLon, firstPointLon);
            firstPointMinLat = Math.min(firstPointMinLat, firstPointLat);

            lastPointMinLon = Math.min(lastPointMinLon, lastPointLon);
            lastPointMinLat = Math.min(lastPointMinLat, lastPointLat);

            firstPointMaxLon = Math.max(firstPointMaxLon, firstPointLon);
            firstPointMaxLat = Math.max(firstPointMaxLat, firstPointLat);

            lastPointMaxLon = Math.max(lastPointMaxLon, firstPointLon);
            lastPointMaxLat = Math.max(lastPointMaxLat, firstPointLat);
        }
        Rectangle rectFirst = new Rectangle(id, firstPointMinLon, firstPointMinLat, firstPointMaxLon, firstPointMaxLat);
        Rectangle rectLast = new Rectangle(id, lastPointMinLon, lastPointMinLat, lastPointMaxLon, lastPointMaxLat);
        return new Rectangle[]{rectFirst, rectLast};
    }

    private LinkedList<LinkedList<Integer>> STRFirstPartition(List<DITATrajectory> trajectories) {
        int trajectoriesSize = trajectories.size();
        ArrayList<Tuple<Integer, Point>> firstPoints = new ArrayList<>(trajectoriesSize);
        for (DITATrajectory trajectory : trajectories) {
            firstPoints.add(new Tuple<Integer, Point>(trajectory.getId(), trajectory.getGlobalIndexedPivot().get(0)));
        }
        int[] dim = new int[2];
        dim[0] = (int) Math.ceil(Math.sqrt(globalPartitionNum));
        dim[1] = globalPartitionNum / dim[0];
        int nodeCapacityLon = (int) Math.ceil(trajectoriesSize * 1.0 / dim[0]);
        firstPoints.sort((o1, o2) -> new Double(o1.f1.getX()).compareTo(o2.f1.getX()));
        LinkedList<LinkedList<Integer>> buckets = new LinkedList<>();
        int p1 = 0, p2 = 0;
        do {
            int sliceTo = Math.min(p1 + nodeCapacityLon, trajectoriesSize);
            ArrayList<Tuple<Integer, Point>> subFirstPoint = new ArrayList<>();
            for (int i = p1; i < sliceTo; i++) {
                subFirstPoint.add(firstPoints.get(i));
            }
            int sliceSize = subFirstPoint.size();
            int nodeCapacityLat = (int) Math.ceil(sliceSize * 1.0 / dim[1]);
            p2 = 0;
            do {
                int packTo = Math.min(p2 + nodeCapacityLat, sliceSize);
                subFirstPoint.sort((o1, o2) -> new Double(o1.f1.getY()).compareTo(o2.f1.getY()));
                LinkedList<Integer> subBuckets = new LinkedList<>();
                for (int i = p2; i < packTo; i++) {
                    subBuckets.add(subFirstPoint.get(i).f0);
                }
                buckets.add(subBuckets);
                p2 = packTo;
            } while (p2 < sliceSize);
            p1 = sliceTo;
        } while (p1 < trajectoriesSize);
        return buckets;
    }

    private LinkedList<LinkedList<Integer>> STRLastPartition(List<DITATrajectory> trajectories, LinkedList<Integer> SeqData) {
        int dataSize = SeqData.size();
        ArrayList<Tuple<Integer, Point>> lastPoints = new ArrayList<>(dataSize);
        for (Integer seqDatum : SeqData) {
            lastPoints.add(new Tuple<>(seqDatum, trajectories.get(seqDatum).getGlobalIndexedPivot().get(1)));
        }
        int[] dim = new int[2];
        dim[0] = (int) Math.ceil(Math.sqrt(globalPartitionNum));
        dim[1] = globalPartitionNum / dim[0];
        int nodeCapacityLon = (int) Math.ceil(dataSize * 1.0 / dim[0]);
        lastPoints.sort((o1, o2) -> new Double(o1.f1.getX()).compareTo(o2.f1.getX()));
        LinkedList<LinkedList<Integer>> buckets = new LinkedList<>();
        int p1 = 0, p2 = 0;
        do {
            int sliceTo = Math.min(p1 + nodeCapacityLon, dataSize);
            ArrayList<Tuple<Integer, Point>> subFirstPoint = new ArrayList<>();
            for (int i = p1; i < sliceTo; i++) {
                subFirstPoint.add(lastPoints.get(i));
            }
            int sliceSize = subFirstPoint.size();
            int nodeCapacityLat = (int) Math.ceil(sliceSize * 1.0 / dim[1]);
            p2 = 0;
            do {
                int packTo = Math.min(p2 + nodeCapacityLat, sliceSize);
                subFirstPoint.sort((o1, o2) -> new Double(o1.f1.getY()).compareTo(o2.f1.getY()));
                LinkedList<Integer> subBuckets = new LinkedList<>();
                for (int i = p2; i < packTo; i++) {
                    subBuckets.add(subFirstPoint.get(i).f0);
                }
                buckets.add(subBuckets);
                p2 = packTo;
            } while (p2 < sliceSize);
            p1 = sliceTo;
        } while (p1 < dataSize);
        return buckets;
    }
}
