package com.konfuse.dison;

import com.konfuse.util.TrajectoryUtils;
import org.apache.flink.api.java.tuple.Tuple2;

import java.io.*;
import java.util.*;

/**
 * @Auther todd
 * @Date 2020/4/20
 */
public class DISONLocalIndex implements Serializable{
    private int partitionID;
    private HashMap<Long, List<Tuple2<DISONTrajectory, Double>>> InvertedIndex;

    public DISONLocalIndex(int partitionID) {
        this.partitionID = partitionID;
        this.InvertedIndex = new HashMap<>();
    }

    public DISONLocalIndex(int partitionID, HashMap<Long, List<Tuple2<DISONTrajectory, Double>>> invertedIndex) {
        this.partitionID = partitionID;
        InvertedIndex = invertedIndex;
    }

    public int getPartitionID() {
        return partitionID;
    }

    public void setPartitionID(int partitionID) {
        this.partitionID = partitionID;
    }

    public HashMap<Long, List<Tuple2<DISONTrajectory, Double>>> getInvertedIndex() {
        return InvertedIndex;
    }

    public void setInvertedIndex(HashMap<Long, List<Tuple2<DISONTrajectory, Double>>> invertedIndex) {
        InvertedIndex = invertedIndex;
    }

    @Override
    public String toString() {
        return "DISONLocalIndex{" +
                "partitionID=" + partitionID +
                ", InvertedIndex=" + InvertedIndex +
                '}';
    }

    public void buildLocalIndex(List<DISONTrajectory> trajectories, double threshold) {
        for (DISONTrajectory trajectory : trajectories) {
            double limitedDistance = (1 - threshold) * trajectory.getLength();
            List<DISONEdge> trajectoryData = trajectory.getTrajectoryData();
            double accumulatedDistance = 0;
            for (DISONEdge trajectoryDatum : trajectoryData) {
                if (accumulatedDistance <= limitedDistance) {
                    List<Tuple2<DISONTrajectory, Double>> newItem = InvertedIndex.getOrDefault(trajectoryDatum.getEdgeId(), new LinkedList<>());
                    newItem.add(new Tuple2<>(trajectory, accumulatedDistance));
                    InvertedIndex.put(trajectoryDatum.getEdgeId(), newItem);
                    accumulatedDistance += trajectoryDatum.getLength();
                } else {
                    break;
                }
            }
        }
    }

    public List<Tuple2<Integer, Double>> searchLocalIndex(DISONTrajectory query, double threshold) {
        double accumulatedDistance = 0.0;
        double limitedDistance = (1 - threshold) * query.getLength();
        Set<Integer> candidates = new HashSet<>();
        List<Tuple2<Integer, Double>> answers = new LinkedList<>();
        for (DISONEdge trajectoryEdge : query.getTrajectoryData()) {
            if (accumulatedDistance <= limitedDistance) {
                if(InvertedIndex.containsKey(trajectoryEdge.getEdgeId())){
                    List<Tuple2<DISONTrajectory, Double>> InvertedList = InvertedIndex.get(trajectoryEdge.getEdgeId());
                    for (Tuple2<DISONTrajectory, Double> tuple : InvertedList) {
                        if(!candidates.contains(tuple.f0.getId())){
                            if (accumulatedDistance + tuple.f1 <= (1 - threshold) / (1 + threshold) * (query.getLength() + tuple.f0.getLength())) {
                                candidates.add(tuple.f0.getId());
                                double distance = TrajectoryUtils.calcLCRSDistance(query, tuple.f0);
                                if(distance >= threshold) {
                                    answers.add(new Tuple2<>(tuple.f0.getId(), distance));
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
    public static DISONLocalIndex load(String file) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
        DISONLocalIndex DISONLocalIndex = (DISONLocalIndex) inputStream.readObject();
        inputStream.close();
        return DISONLocalIndex;
    }
}
