package com.konfuse.dison;

import com.konfuse.util.TrajectoryUtils;
import com.konfuse.util.Tuple;

import java.util.*;

/**
 * @Auther todd
 * @Date 2020/4/20
 */
public class LocalIndex {
    private int partitionID;
    private HashMap<Long, List<Tuple<Integer, Double>>> InvertedIndex;

    public LocalIndex(int partitionID) {
        this.partitionID = partitionID;
        this.InvertedIndex = new HashMap<>();
    }

    public int getPartitionID() {
        return partitionID;
    }

    public void setPartitionID(int partitionID) {
        this.partitionID = partitionID;
    }

    public HashMap<Long, List<Tuple<Integer, Double>>> getInvertedIndex() {
        return InvertedIndex;
    }

    public void setInvertedIndex(HashMap<Long, List<Tuple<Integer, Double>>> invertedIndex) {
        InvertedIndex = invertedIndex;
    }

    public void buildLocalIndex(List<DISONTrajectory> trajectories, GlobalPartition globalPartition, double threshold) {
        List<Integer> trajectorySeqNums = globalPartition.getTrajectorySeqNums();
        for (Integer trajectorySeqNum : trajectorySeqNums) {
            double limitedDistance = (1 - threshold) * trajectories.get(trajectorySeqNum).getLength();
            List<DISONEdge> trajectoryData = trajectories.get(trajectorySeqNum).getTrajectoryData();
            double accumulatedDistance = 0;
            for (DISONEdge trajectoryDatum : trajectoryData) {
                if (accumulatedDistance <= limitedDistance) {
                    List<Tuple<Integer, Double>> newItem = InvertedIndex.getOrDefault(trajectoryDatum.getEdgeId(), new LinkedList<>());
                    newItem.add(new Tuple<>(trajectorySeqNum, accumulatedDistance));
                    InvertedIndex.put(trajectoryDatum.getEdgeId(), newItem);
                    accumulatedDistance += trajectoryDatum.getLength();
                } else {
                    break;
                }
            }
        }
    }

    public List<Integer> searchLocalIndex(List<DISONTrajectory> trajectories, DISONTrajectory query, double threshold) {
        double accumulatedDistance = 0.0;
        double limitedDistance = (1 - threshold) * query.getLength();
        Set<Integer> candidates = new HashSet<>();
        List<Integer> answers = new LinkedList<>();
        for (DISONEdge trajectoryEdge : query.getTrajectoryData()) {
            if (accumulatedDistance <= limitedDistance) {
                if(InvertedIndex.containsKey(trajectoryEdge.getEdgeId())){
                    List<Tuple<Integer, Double>> InvertedList = InvertedIndex.get(trajectoryEdge.getEdgeId());
                    for (Tuple<Integer, Double> integerDoubleTuple : InvertedList) {
                        if(!candidates.contains(integerDoubleTuple.f0)){
                            if (accumulatedDistance + integerDoubleTuple.f1 <= (1 - threshold) / (1 + threshold) * (query.getLength() + trajectories.get(integerDoubleTuple.f0).getLength())) {
                                candidates.add(integerDoubleTuple.f0);
                                if(TrajectoryUtils.calcLCRSDistance(query, trajectories.get(integerDoubleTuple.f0)) >= threshold) {
                                    answers.add(integerDoubleTuple.f0);
                                }
                            }
                        }
                    }
                }
            } else {
                break;
            }
            accumulatedDistance += trajectoryEdge.getLength();
        }
        return answers;
    }


    @Override
    public String toString() {
        return "LocalIndex{" +
                "partitionID=" + partitionID +
                ", InvertedIndex=" + InvertedIndex +
                '}';
    }
}
