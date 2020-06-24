package com.konfuse.dita;

import com.konfuse.geometry.Point;
import com.konfuse.strtree.MBR;
import com.konfuse.util.TrajectoryUtils;
import com.konfuse.util.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.konfuse.dita.TrieNodeType.*;

/**
 * @Auther todd
 * @Date 2020/4/17
 */
public class DITALocalIndex implements Serializable {
    private final int localIndexedPivotSize;
    private final int localMinNodeSize;

    private int partitionID;
    private TrieNode root;

    public DITALocalIndex(int partitionID, int localIndexedPivotSize, int localMinNodeSize) {
        this.partitionID = partitionID;
        this.root = new TrieInternalNode(0, TRIE_ROOT_NODE, null, 0);
        this.localIndexedPivotSize = localIndexedPivotSize;
        this.localMinNodeSize = localMinNodeSize;
    }

    public DITALocalIndex(int localIndexedPivotSize, int localMinNodeSize, int partitionID, TrieNode root) {
        this.localIndexedPivotSize = localIndexedPivotSize;
        this.localMinNodeSize = localMinNodeSize;
        this.partitionID = partitionID;
        this.root = root;
    }

    public int getPartitionID() {
        return partitionID;
    }

    public void setPartitionID(int partitionID) {
        this.partitionID = partitionID;
    }

    public TrieNode getRoot() {
        return root;
    }

    public void setRoot(TrieNode root) {
        this.root = root;
    }

    public int getLocalIndexedPivotSize() {
        return localIndexedPivotSize;
    }

    public int getLocalMinNodeSize() {
        return localMinNodeSize;
    }

    public void buildLocalIndexDirectly(List<DITATrajectory> trajectories) {
        root.setCurrentCapacity(trajectories.size());
        buildIndexRecursivelyDirectly(trajectories, this.root, 0);
    }

    public List<Tuple2<Integer, Double>> search(DITATrajectory query, double threshold) {
        ArrayList<Point> newQueryData = new ArrayList<>();
        newQueryData.addAll(new ArrayList<>(query.getGlobalIndexedPivot()));
        newQueryData.addAll(new ArrayList<>(query.getTrajectoryData()));
        LinkedList<DITATrajectory> results = new LinkedList<>();
        DITASearchFilter(newQueryData, this.root, threshold, results);
        List<Tuple2<Integer, Double>> finalResults = DITASearchVerify(query, results, threshold);
        return finalResults;
    }


    private void DITASearchFilter(ArrayList<Point> query, TrieNode root, double threshold, LinkedList<DITATrajectory> results) {
        if (root.getLevel() == 0) {
            for (TrieNode child : ((TrieInternalNode) root).getChildes()) {
                DITASearchFilter((ArrayList<Point>) query.clone(), child, threshold, results);
            }
        } else if (root.getLevel() == 1 || root.getLevel() == 2) {
            Point firstPoint = query.get(0);
            query.remove(0);
            double distance = root.getMbr().calculateDistanceToBorder(firstPoint);
            if (distance < threshold) {
                for (TrieNode child : ((TrieInternalNode) root).getChildes()) {
                    DITASearchFilter((ArrayList<Point>) query.clone(), child, threshold - distance, results);
                }
            }
        } else {
            if (root.getLevel() == DITAConfig.localIndexedPivotSize) {
                double distance = calcMinDistanceToMBR(query, root.getMbr());
                if (distance < threshold) {
                    for (DITATrajectory data : ((TrieLeafNode) root).getData()) {
                        results.add(data);
                    }
                }
            } else {
                double distance = Double.MAX_VALUE;
                while (!query.isEmpty() && root.getMbr().calculateDistanceToBorder(query.get(0)) > threshold) {
                    query.remove(0);
                }
                if (!query.isEmpty()) {
                    distance = calcMinDistanceToMBR(query, root.getMbr());
                } else {
                    return;
                }
                if (distance < threshold) {
                    for (TrieNode child : ((TrieInternalNode) root).getChildes()) {
                        DITASearchFilter((ArrayList<Point>) query.clone(), child, threshold - distance, results);
                    }
                }
            }
        }
    }

    private List<Tuple2<Integer, Double>> DITASearchVerify(DITATrajectory query, LinkedList<DITATrajectory> results, double threshold) {
        ArrayList<Tuple2<Integer, Double>> verifiedResults = new ArrayList<>();
        LinkedList<DITATrajectory> filteredResults = new LinkedList<>();
        for (DITATrajectory trajectory : results) {
            if (trajectory.getExtendedMBR().contains(query.getMbr()) || query.getExtendedMBR().contains(trajectory.getMbr())) {
                filteredResults.add(trajectory);
            }
        }
        LinkedList<DITATrajectory> filteredResultsSecond = new LinkedList<>();
        for (DITATrajectory trajectory : filteredResults) {
            if (TrajectoryUtils.calcDTWCellsEstimation(query, trajectory, threshold) > threshold && TrajectoryUtils.calcDTWCellsEstimation(trajectory, query, threshold) > threshold) {
                continue;
            } else {
                filteredResultsSecond.add(trajectory);
            }
        }
        for (DITATrajectory trajectory : filteredResultsSecond) {
            double dtwDistance = TrajectoryUtils.calcDTWDistanceWithThreshold(trajectory.getTrajectoryData(), query.getTrajectoryData(), threshold);
            if (dtwDistance < threshold) {
                verifiedResults.add(new Tuple2<>(trajectory.getId(), dtwDistance));
            }
        }
        return verifiedResults;
    }

    public double calcMinDistanceToMBR(List<Point> trajectoryData, MBR mbr) {
        int num = trajectoryData.size();
        double min_dist = Double.MAX_VALUE;
        for (Point trajectoryDatum : trajectoryData) {
            if (judgeZero(min_dist)) {
                break;
            }
            min_dist = Math.min(min_dist, mbr.calculateDistanceToBorder(trajectoryDatum));
        }
        return min_dist;
    }

    public boolean judgeZero(double x) {
        return x >= -1e-10 && x <= 1e-10;
    }

//    private void buildIndexRecursively(List<DITATrajectory> trajectories, TrieNode root, int level) {
//        LinkedList<List<Integer>> StrPartitionTrajectorySeqNums = localSTRPartition(trajectories, trajectorySeqNums, level);
//        int pSize = StrPartitionTrajectorySeqNums.size();
//        TrieInternalNode curNode = (TrieInternalNode) root;
//        if (level < localIndexedPivotSize - 1) {
//            for (List<Integer> strPartitionTrajectorySeqNum : StrPartitionTrajectorySeqNums) {
//                int subPSize = strPartitionTrajectorySeqNum.size();
//                double lowLon = 180.0, lowLat = 90, highLon = -180.0, highLat = 90.0;
//                for (Integer seqNum : strPartitionTrajectorySeqNum) {
//                    double lon = trajectories.get(seqNum).getLocalIndexedPivot().get(level).getX();
//                    double lat = trajectories.get(seqNum).getLocalIndexedPivot().get(level).getY();
//                    lowLon = Math.min(lon, lowLon);
//                    lowLat = Math.min(lat, lowLat);
//                    highLon = Math.max(lon, highLon);
//                    highLat = Math.max(lat, highLat);
//                }
//                MBR curNodeMBR = new MBR(lowLon, lowLat, highLon, highLat);
//                TrieNode newNode = new TrieInternalNode(level + 1, TRIE_INTERNAL_NODE, curNodeMBR, subPSize);
//                curNode.addChild(newNode);
//                buildIndexRecursively(trajectories, strPartitionTrajectorySeqNum, newNode, level + 1);
//            }
//        } else {
//            for (List<Integer> strPartitionTrajectorySeqNum : StrPartitionTrajectorySeqNums) {
//                int subPSize = strPartitionTrajectorySeqNum.size();
//                double lowLon = 180.0, lowLat = 90, highLon = -180.0, highLat = 90.0;
//                for (Integer seqNum : strPartitionTrajectorySeqNum) {
//                    double lon = trajectories.get(seqNum).getLocalIndexedPivot().get(level).getX();
//                    double lat = trajectories.get(seqNum).getLocalIndexedPivot().get(level).getY();
//                    lowLon = Math.min(lon, lowLon);
//                    lowLat = Math.min(lat, lowLat);
//                    highLon = Math.max(lon, highLon);
//                    highLat = Math.max(lat, highLat);
//                }
//                MBR curNodeMBR = new MBR(lowLon, lowLat, highLon, highLat);
//                TrieNode newNode = new TrieLeafNode(level + 1, TRIE_LEAF_NODE, curNodeMBR, subPSize, strPartitionTrajectorySeqNum);
//                curNode.addChild(newNode);
//            }
//        }
//
//    }

    private void buildIndexRecursivelyDirectly(List<DITATrajectory> trajectories, TrieNode root, int level) {
        LinkedList<List<DITATrajectory>> strPartitions = localSTRPartitionDirectly(trajectories, level);

        TrieInternalNode curNode = (TrieInternalNode) root;
        if (level < localIndexedPivotSize - 1) {
            for (List<DITATrajectory> strPartitionTrajectories : strPartitions) {
                int subPSize = strPartitionTrajectories.size();
                double lowLon = 180.0, lowLat = 90, highLon = -180.0, highLat = 90.0;
                for (DITATrajectory trajectory : strPartitionTrajectories) {
                    double lon = trajectory.getLocalIndexedPivot().get(level).getX();
                    double lat = trajectory.getLocalIndexedPivot().get(level).getY();
                    lowLon = Math.min(lon, lowLon);
                    lowLat = Math.min(lat, lowLat);
                    highLon = Math.max(lon, highLon);
                    highLat = Math.max(lat, highLat);
                }
                MBR curNodeMBR = new MBR(lowLon, lowLat, highLon, highLat);
                TrieNode newNode = new TrieInternalNode(level + 1, TRIE_INTERNAL_NODE, curNodeMBR, subPSize);
                curNode.addChild(newNode);
                buildIndexRecursivelyDirectly(strPartitionTrajectories, newNode, level + 1);
            }
        } else {
            for (List<DITATrajectory> strPartitionTrajectories : strPartitions) {
                int subPSize = strPartitionTrajectories.size();
                double lowLon = 180.0, lowLat = 90, highLon = -180.0, highLat = 90.0;
                for (DITATrajectory trajectory : strPartitionTrajectories) {
                    double lon = trajectory.getLocalIndexedPivot().get(level).getX();
                    double lat = trajectory.getLocalIndexedPivot().get(level).getY();
                    lowLon = Math.min(lon, lowLon);
                    lowLat = Math.min(lat, lowLat);
                    highLon = Math.max(lon, highLon);
                    highLat = Math.max(lat, highLat);
                }
                MBR curNodeMBR = new MBR(lowLon, lowLat, highLon, highLat);
                TrieNode newNode = new TrieLeafNode(level + 1, TRIE_LEAF_NODE, curNodeMBR, subPSize, strPartitionTrajectories);
                curNode.addChild(newNode);
            }
        }

    }

//    private LinkedList<List<Integer>> localSTRPartition(List<DITATrajectory> trajectories, int level) {
//        LinkedList<List<DITATrajectory>> StrPartitions = new LinkedList<>();
//        ArrayList<Tuple<DITATrajectory, Point>> part = new ArrayList<>(trajectories.size());
//        for (Integer trajectorySeqNum : trajectorySeqNums) {
//            part.add(new Tuple<>(trajectorySeqNum, trajectories.get(trajectorySeqNum).getLocalIndexedPivot().get(level)));
//        }
//        int trajectorySize = trajectorySeqNums.size();
//        if (trajectorySize <= localMinNodeSize) {
//            for (Integer trajectorySeqNum : trajectorySeqNums) {
//                LinkedList<Integer> temp = new LinkedList<>();
//                temp.add(trajectorySeqNum);
//                StrPartitions.add(temp);
//            }
//        } else {
//            int expectedPartNum = localMinNodeSize;
//            int[] dim = new int[2];
//            for (int i = 0; i < 2; i++) {
//                dim[i] = (int) Math.ceil(Math.pow(expectedPartNum, 1.0 / (2 - i)));
//                expectedPartNum /= dim[i];
//            }
//            int nodeCapacityX = (int) Math.ceil(trajectorySize * 1.0 / dim[0]);
//            part.sort((o1, o2) -> new Double(o1.f1.getX()).compareTo(o2.f1.getX()));
//            int p1 = 0;
//            int p2 = 0;
//            do {
//                int sliceTo = Math.min(p1 + nodeCapacityX, trajectorySize);
//                ArrayList<Tuple<Integer, Point>> subPart = new ArrayList<>(sliceTo - p1);
//                for (int i = p1; i < sliceTo; i++) {
//                    subPart.add(part.get(i));
//                }
//                int sliceSize = subPart.size();
//                int nodeCapacityY = (int) Math.ceil(sliceSize * 1.0 / dim[1]);
//                p2 = 0;
//                do {
//                    int packTo = Math.min(p2 + nodeCapacityY, sliceSize);
//                    subPart.sort((o1, o2) -> new Double(o1.f1.getY()).compareTo(o2.f1.getY()));
//                    LinkedList<Integer> subTrajectorySeqNums = new LinkedList<>();
//                    for (int i = p2; i < packTo; i++) {
//                        subTrajectorySeqNums.add(subPart.get(i).f0);
//                    }
//                    StrPartitions.add(subTrajectorySeqNums);
//                    p2 = packTo;
//                } while (p2 < sliceSize);
//                p1 = sliceTo;
//                subPart.clear();
//            } while (p1 < trajectorySize);
//        }
//        return StrPartitions;
//    }

    private LinkedList<List<DITATrajectory>> localSTRPartitionDirectly(List<DITATrajectory> trajectories, int level) {
        LinkedList<List<DITATrajectory>> StrPartitions = new LinkedList<>();
        ArrayList<Tuple<DITATrajectory, Point>> part = new ArrayList<>(trajectories.size());
        for (DITATrajectory trajectory : trajectories) {
            part.add(new Tuple<>(trajectory, trajectory.getLocalIndexedPivot().get(level)));
        }
        int trajectorySize = trajectories.size();
        if (trajectorySize <= localMinNodeSize) {
            for (DITATrajectory trajectory : trajectories) {
                LinkedList<DITATrajectory> temp = new LinkedList<>();
                temp.add(trajectory);
                StrPartitions.add(temp);
            }
        } else {
            int expectedPartNum = localMinNodeSize;
            int[] dim = new int[2];
            for (int i = 0; i < 2; i++) {
                dim[i] = (int) Math.ceil(Math.pow(expectedPartNum, 1.0 / (2 - i)));
                expectedPartNum /= dim[i];
            }
            int nodeCapacityX = (int) Math.ceil(trajectorySize * 1.0 / dim[0]);
            part.sort((o1, o2) -> new Double(o1.f1.getX()).compareTo(o2.f1.getX()));
            int p1 = 0;
            int p2 = 0;
            do {
                int sliceTo = Math.min(p1 + nodeCapacityX, trajectorySize);
                ArrayList<Tuple<DITATrajectory, Point>> subPart = new ArrayList<>(sliceTo - p1);
                for (int i = p1; i < sliceTo; i++) {
                    subPart.add(part.get(i));
                }
                int sliceSize = subPart.size();
                int nodeCapacityY = (int) Math.ceil(sliceSize * 1.0 / dim[1]);
                p2 = 0;
                do {
                    int packTo = Math.min(p2 + nodeCapacityY, sliceSize);
                    subPart.sort((o1, o2) -> new Double(o1.f1.getY()).compareTo(o2.f1.getY()));
                    LinkedList<DITATrajectory> subTrajectories = new LinkedList<>();
                    for (int i = p2; i < packTo; i++) {
                        subTrajectories.add(subPart.get(i).f0);
                    }
                    StrPartitions.add(subTrajectories);
                    p2 = packTo;
                } while (p2 < sliceSize);
                p1 = sliceTo;
                subPart.clear();
            } while (p1 < trajectorySize);
        }
        return StrPartitions;
    }

    /**
     * Method to serialize this LocalIndex
     */
    public void save(String file) throws IOException {
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
        outputStream.writeObject(this);
        outputStream.close();
    }

    /**
     * Method to deserialize the file to LocalIndex
     *
     * @param file r-tree model path
     * @return r-tree object
     */
    public static DITALocalIndex load(String file) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
        DITALocalIndex DITALocalIndex = (DITALocalIndex) inputStream.readObject();
        inputStream.close();
        return DITALocalIndex;
    }

}
