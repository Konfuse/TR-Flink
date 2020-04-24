package com.konfuse.dita;

import com.konfuse.geometry.Point;
import com.konfuse.strtree.MBR;
import com.konfuse.util.TrajectoryUtils;
import com.konfuse.util.Tuple;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.konfuse.dita.TrieNodeType.*;

/**
 * @Auther todd
 * @Date 2020/4/17
 */
public class LocalIndex {
    private final int localIndexedPivotSize;
    private final int localMinNodeSize;

    private int partitionID;
    private TrieNode root;

    public LocalIndex(int partitionID, int localIndexedPivotSize, int localMinNodeSize) {
        this.partitionID = partitionID;
        this.root = new TrieInternalNode(0, TRIE_ROOT_NODE, null, 0);
        this.localIndexedPivotSize = localIndexedPivotSize;
        this.localMinNodeSize = localMinNodeSize;
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

    public void buildLocalIndex(List<DITATrajectory> trajectories, List<Integer> trajectorySeqNums) {
        root.setCurrentCapacity(trajectorySeqNums.size());
        buildIndexRecursively(trajectories, trajectorySeqNums, this.root, 0);
    }

    public List<Tuple<Integer, Double>> search(ArrayList<DITATrajectory> trajectories, DITATrajectory query, double threshold) {
        ArrayList<Point> newQueryData = new ArrayList<>();
        newQueryData.addAll(new ArrayList<>(query.getGlobalIndexedPivot()));
        newQueryData.addAll(new ArrayList<>(query.getTrajectoryData()));
        LinkedList<Integer> results = new LinkedList<>();
        DITASearchFilter(newQueryData, this.root, threshold, results);
        List<Tuple<Integer, Double>> finalResults = DITASearchVerify(trajectories, query, results, threshold);
        return finalResults;
    }

    private void DITASearchFilter(ArrayList<Point> query, TrieNode root, double threshold, LinkedList<Integer> results) {
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
                    for (int data : ((TrieLeafNode) root).getData()) {
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

    private List<Tuple<Integer, Double>> DITASearchVerify(ArrayList<DITATrajectory> trajectories, DITATrajectory query, LinkedList<Integer> results, double threshold) {
        ArrayList<Tuple<Integer, Double>> verifiedResults = new ArrayList<>();
        LinkedList<Integer> filteredResults = new LinkedList<>();
        for (Integer result : results) {
            if (trajectories.get(result).getExtendedMBR().contains(query.getMbr()) || query.getExtendedMBR().contains(trajectories.get(result).getMbr())) {
                filteredResults.add(result);
            }
        }
        LinkedList<Integer> filteredResultsSecond = new LinkedList<>();
        for (Integer result : filteredResults) {
            if (TrajectoryUtils.calcDTWCellsEstimation(query, trajectories.get(result), threshold) > threshold && TrajectoryUtils.calcDTWCellsEstimation(trajectories.get(result), query, threshold) > threshold) {
                continue;
            } else {
                filteredResultsSecond.add(result);
            }
        }
        for (Integer filteredSeqNum : filteredResultsSecond) {
            double dtwDistance = TrajectoryUtils.calcDTWDistanceWithThreshold(trajectories.get(filteredSeqNum), query, threshold);
            if (dtwDistance < threshold) {
                verifiedResults.add(new Tuple<>(filteredSeqNum, dtwDistance));
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

    private void buildIndexRecursively(List<DITATrajectory> trajectories, List<Integer> trajectorySeqNums, TrieNode root, int level) {
        LinkedList<List<Integer>> StrPartitionTrajectorySeqNums = localSTRPartition(trajectories, trajectorySeqNums, level);
        int pSize = StrPartitionTrajectorySeqNums.size();
        TrieInternalNode curNode = (TrieInternalNode) root;
        if (level < localIndexedPivotSize - 1) {
            for (List<Integer> strPartitionTrajectorySeqNum : StrPartitionTrajectorySeqNums) {
                int subPSize = strPartitionTrajectorySeqNum.size();
                double lowLon = 180.0, lowLat = 90, highLon = -180.0, highLat = 90.0;
                for (Integer seqNum : strPartitionTrajectorySeqNum) {
                    double lon = trajectories.get(seqNum).getLocalIndexedPivot().get(level).getX();
                    double lat = trajectories.get(seqNum).getLocalIndexedPivot().get(level).getY();
                    lowLon = Math.min(lon, lowLon);
                    lowLat = Math.min(lat, lowLat);
                    highLon = Math.max(lon, highLon);
                    highLat = Math.max(lat, highLat);
                }
                MBR curNodeMBR = new MBR(lowLon, lowLat, highLon, highLat);
                TrieNode newNode = new TrieInternalNode(level + 1, TRIE_INTERNAL_NODE, curNodeMBR, subPSize);
                curNode.addChild(newNode);
                buildIndexRecursively(trajectories, strPartitionTrajectorySeqNum, newNode, level + 1);
            }
        } else {
            for (List<Integer> strPartitionTrajectorySeqNum : StrPartitionTrajectorySeqNums) {
                int subPSize = strPartitionTrajectorySeqNum.size();
                double lowLon = 180.0, lowLat = 90, highLon = -180.0, highLat = 90.0;
                for (Integer seqNum : strPartitionTrajectorySeqNum) {
                    double lon = trajectories.get(seqNum).getLocalIndexedPivot().get(level).getX();
                    double lat = trajectories.get(seqNum).getLocalIndexedPivot().get(level).getY();
                    lowLon = Math.min(lon, lowLon);
                    lowLat = Math.min(lat, lowLat);
                    highLon = Math.max(lon, highLon);
                    highLat = Math.max(lat, highLat);
                }
                MBR curNodeMBR = new MBR(lowLon, lowLat, highLon, highLat);
                TrieNode newNode = new TrieLeafNode(level + 1, TRIE_LEAF_NODE, curNodeMBR, subPSize, strPartitionTrajectorySeqNum);
                curNode.addChild(newNode);
            }
        }

    }

    private LinkedList<List<Integer>> localSTRPartition(List<DITATrajectory> trajectories, List<Integer> trajectorySeqNums, int level) {
        LinkedList<List<Integer>> StrPartitions = new LinkedList<>();
        ArrayList<Tuple<Integer, Point>> part = new ArrayList<>(trajectorySeqNums.size());
        for (Integer trajectorySeqNum : trajectorySeqNums) {
            part.add(new Tuple<>(trajectorySeqNum, trajectories.get(trajectorySeqNum).getLocalIndexedPivot().get(level)));
        }
        int trajectorySize = trajectorySeqNums.size();
        if (trajectorySize <= localMinNodeSize) {
            for (Integer trajectorySeqNum : trajectorySeqNums) {
                LinkedList<Integer> temp = new LinkedList<>();
                temp.add(trajectorySeqNum);
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
                ArrayList<Tuple<Integer, Point>> subPart = new ArrayList<>(sliceTo - p1);
                for (int i = p1; i < sliceTo; i++) {
                    subPart.add(part.get(i));
                }
                int sliceSize = subPart.size();
                int nodeCapacityY = (int) Math.ceil(sliceSize * 1.0 / dim[1]);
                p2 = 0;
                do {
                    int packTo = Math.min(p2 + nodeCapacityY, sliceSize);
                    subPart.sort((o1, o2) -> new Double(o1.f1.getY()).compareTo(o2.f1.getY()));
                    LinkedList<Integer> subTrajectorySeqNums = new LinkedList<>();
                    for (int i = p2; i < packTo; i++) {
                        subTrajectorySeqNums.add(subPart.get(i).f0);
                    }
                    StrPartitions.add(subTrajectorySeqNums);
                    p2 = packTo;
                } while (p2 < sliceSize);
                p1 = sliceTo;
                subPart.clear();
            } while (p1 < trajectorySize);
        }
        return StrPartitions;
    }

}
