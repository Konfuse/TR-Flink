package com.konfuse.dita;

import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.Point;
import com.konfuse.geometry.Rectangle;
import com.konfuse.strtree.IndexBuilder;
import com.konfuse.strtree.MBR;
import com.konfuse.strtree.RTree;
import com.konfuse.util.Tuple;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/4/18
 */
public class DITAGlobalIndex implements Serializable{

    private final int globalPartitionNum;
    private RTree STRtreeFirst;
    private RTree STRtreeLast;

    public DITAGlobalIndex(int globalPartitionNum) {
        this.globalPartitionNum = globalPartitionNum;
    }

    public DITAGlobalIndex(int globalPartitionNum, RTree STRtreeFirst, RTree STRtreeLast) {
        this.globalPartitionNum = globalPartitionNum;
        this.STRtreeFirst = STRtreeFirst;
        this.STRtreeLast = STRtreeLast;
    }



    public int getGlobalPartitionNum() {
        return globalPartitionNum;
    }

    public RTree getSTRtreeFirst() {
        return STRtreeFirst;
    }

    public RTree getSTRtreeLast() {
        return STRtreeLast;
    }


    public ArrayList<Tuple<Integer, LinkedList<DITATrajectory>>> buildGlobalIndex(List<DITATrajectory> trajectories) {
        ArrayList<Tuple<Integer, LinkedList<DITATrajectory>>> partitionResults = new ArrayList<>();
        LinkedList<LinkedList<DITATrajectory>> buckets = STRPartition(trajectories, 0);
        int count = 0;
        for (LinkedList<DITATrajectory> bucket : buckets) {
            System.out.println("Bucket size: " + bucket.size());
            LinkedList<LinkedList<DITATrajectory>> subBuckets = STRPartition(bucket, 1);
            System.out.println("subBucket num: " + subBuckets.size());
            for (LinkedList<DITATrajectory> subBucket : subBuckets) {
                partitionResults.add(new Tuple<>(count, subBucket));
                count++;
                System.out.println("subBucket size: " + count + " " + subBucket.size());
            }
        }

        ArrayList<Rectangle> firstPointRectangles = new ArrayList<>();
        ArrayList<Rectangle> lastPointRectangles = new ArrayList<>();
        for (int i = 0; i < partitionResults.size(); i++) {
            Rectangle[] mbrs = calcPartitionBound(partitionResults.get(i).f1, partitionResults.get(i).f0);
            firstPointRectangles.add(mbrs[0]);
            lastPointRectangles.add(mbrs[1]);
            MBR firstPointMbr = new MBR(mbrs[0].getMBR().getX1(), mbrs[0].getMBR().getY1(), mbrs[0].getMBR().getX2(), mbrs[0].getMBR().getY2());
            MBR lastPointMbr = new MBR(mbrs[1].getMBR().getX1(), mbrs[1].getMBR().getY1(), mbrs[1].getMBR().getX2(), mbrs[1].getMBR().getY2());
        }
        STRtreeFirst = new IndexBuilder().createRTreeBySTR(firstPointRectangles.toArray(new Rectangle[firstPointRectangles.size()]));
        STRtreeLast = new IndexBuilder().createRTreeBySTR(lastPointRectangles.toArray(new Rectangle[lastPointRectangles.size()]));
        return partitionResults;
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

    private Rectangle[] calcPartitionBound(List<DITATrajectory> trajectories, int id) {
        double firstPointMinLon = 180, firstPointMinLat = 90, firstPointMaxLon = -180, firstPointMaxLat = -90;
        double lastPointMinLon = 180, lastPointMinLat = 90, lastPointMaxLon = -180, lastPointMaxLat = -90;
        for (DITATrajectory trajectory : trajectories) {
            double firstPointLon = trajectory.getGlobalIndexedPivot().get(0).getX();
            double firstPointLat = trajectory.getGlobalIndexedPivot().get(0).getY();
            double lastPointLon = trajectory.getGlobalIndexedPivot().get(1).getX();
            double lastPointLat = trajectory.getGlobalIndexedPivot().get(1).getY();
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

    private LinkedList<LinkedList<DITATrajectory>> STRPartition(List<DITATrajectory> trajectories, int globalPivotNum) {
        int trajectoriesSize = trajectories.size();
        ArrayList<Tuple<DITATrajectory, Point>> pivotTuples = new ArrayList<>(trajectoriesSize);
        for (DITATrajectory trajectory : trajectories) {
            if (globalPivotNum == 0) {
                pivotTuples.add(new Tuple<>(trajectory, trajectory.getGlobalIndexedPivot().get(0)));
            } else {
                pivotTuples.add(new Tuple<>(trajectory, trajectory.getGlobalIndexedPivot().get(1)));
            }
        }
        int[] dim = new int[2];
        dim[0] = (int) Math.ceil(Math.sqrt(globalPartitionNum));
        dim[1] = globalPartitionNum / dim[0];
        int nodeCapacityLon = (int) Math.ceil(trajectoriesSize * 1.0 / dim[0]);
        pivotTuples.sort((o1, o2) -> new Double(o1.f1.getX()).compareTo(o2.f1.getX()));
        LinkedList<LinkedList<DITATrajectory>> buckets = new LinkedList<>();
        int p1 = 0, p2 = 0;
        do {
            int sliceTo = Math.min(p1 + nodeCapacityLon, trajectoriesSize);
            ArrayList<Tuple<DITATrajectory, Point>> subFirstPoint = new ArrayList<>();
            for (int i = p1; i < sliceTo; i++) {
                subFirstPoint.add(pivotTuples.get(i));
            }
            int sliceSize = subFirstPoint.size();
            int nodeCapacityLat = (int) Math.ceil(sliceSize * 1.0 / dim[1]);
            p2 = 0;
            do {
                int packTo = Math.min(p2 + nodeCapacityLat, sliceSize);
                subFirstPoint.sort((o1, o2) -> new Double(o1.f1.getY()).compareTo(o2.f1.getY()));
                LinkedList<DITATrajectory> subBuckets = new LinkedList<>();
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

//    private LinkedList<LinkedList<Integer>> STRLastPartition(List<DITATrajectory> trajectories) {
//        int dataSize = SeqData.size();
//        ArrayList<Tuple<DITATrajectory, Point>> lastPoints = new ArrayList<>(dataSize);
//        for (Integer seqDatum : SeqData) {
//            lastPoints.add(new Tuple<>(seqDatum, trajectories.get(seqDatum).getGlobalIndexedPivot().get(1)));
//        }
//        int[] dim = new int[2];
//        dim[0] = (int) Math.ceil(Math.sqrt(globalPartitionNum));
//        dim[1] = globalPartitionNum / dim[0];
//        int nodeCapacityLon = (int) Math.ceil(dataSize * 1.0 / dim[0]);
//        lastPoints.sort((o1, o2) -> new Double(o1.f1.getX()).compareTo(o2.f1.getX()));
//        LinkedList<LinkedList<Integer>> buckets = new LinkedList<>();
//        int p1 = 0, p2 = 0;
//        do {
//            int sliceTo = Math.min(p1 + nodeCapacityLon, dataSize);
//            ArrayList<Tuple<Integer, Point>> subFirstPoint = new ArrayList<>();
//            for (int i = p1; i < sliceTo; i++) {
//                subFirstPoint.add(lastPoints.get(i));
//            }
//            int sliceSize = subFirstPoint.size();
//            int nodeCapacityLat = (int) Math.ceil(sliceSize * 1.0 / dim[1]);
//            p2 = 0;
//            do {
//                int packTo = Math.min(p2 + nodeCapacityLat, sliceSize);
//                subFirstPoint.sort((o1, o2) -> new Double(o1.f1.getY()).compareTo(o2.f1.getY()));
//                LinkedList<Integer> subBuckets = new LinkedList<>();
//                for (int i = p2; i < packTo; i++) {
//                    subBuckets.add(subFirstPoint.get(i).f0);
//                }
//                buckets.add(subBuckets);
//                p2 = packTo;
//            } while (p2 < sliceSize);
//            p1 = sliceTo;
//        } while (p1 < dataSize);
//        return buckets;
//    }

    /**
     * Method to serialize this GlobalIndex
     */
    public void save(String file) throws IOException {
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
        outputStream.writeObject(this);
        outputStream.close();
    }

    /**
     * Method to deserialize the file to GlobalIndex
     *
     * @param file r-tree model path
     * @return r-tree object
     */
    public static DITAGlobalIndex load(String file) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
        DITAGlobalIndex DITAGlobalIndex = (DITAGlobalIndex) inputStream.readObject();
        inputStream.close();
        return DITAGlobalIndex;
    }
}
