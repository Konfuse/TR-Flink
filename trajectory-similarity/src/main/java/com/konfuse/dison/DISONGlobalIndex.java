package com.konfuse.dison;

import com.konfuse.geometry.Point;
import com.konfuse.road.Road;
import com.konfuse.util.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;

import java.io.*;
import java.util.*;

/**
 * @Auther todd
 * @Date 2020/4/20
 */
public class DISONGlobalIndex implements Serializable {
    private int globalPartitionNum;
    private int partitionSize;
    private HashMap<Long, Integer> firstLevelMap;
    private HashMap<Integer, HashMap<Long, Integer>> secondLevelMap;
    private HashMap<Integer ,Set<Long>> partitionedVertices;
    private HashMap<Integer, Double> longestLengthOfPartition;

    public DISONGlobalIndex(int globalPartitionNum) {
        this.globalPartitionNum = globalPartitionNum;
        this.partitionSize = 0;
        this.firstLevelMap = new HashMap<>();
        this.secondLevelMap = new HashMap<>();
        this.partitionedVertices = new HashMap<>();
        this.longestLengthOfPartition = new HashMap<>();
    }

    public DISONGlobalIndex(int globalPartitionNum, HashMap<Long, Integer> firstLevelMap, HashMap<Integer, HashMap<Long, Integer>> secondLevelMap, HashMap<Integer, Set<Long>> partitionedVertices, HashMap<Integer, Double> longestLengthOfPartition) {
        this.globalPartitionNum = globalPartitionNum;
        this.firstLevelMap = firstLevelMap;
        this.secondLevelMap = secondLevelMap;
        this.partitionedVertices = partitionedVertices;
        this.longestLengthOfPartition = longestLengthOfPartition;
    }

    public int getGlobalPartitionNum() {
        return globalPartitionNum;
    }

    public void setGlobalPartitionNum(int globalPartitionNum) {
        this.globalPartitionNum = globalPartitionNum;
    }

    public int getPartitionSize() {
        return partitionSize;
    }

    public void setPartitionSize(int partitionSize) {
        this.partitionSize = partitionSize;
    }

    public HashMap<Long, Integer> getFirstLevelMap() {
        return firstLevelMap;
    }

    public void setFirstLevelMap(HashMap<Long, Integer> firstLevelMap) {
        this.firstLevelMap = firstLevelMap;
    }

    public HashMap<Integer, HashMap<Long, Integer>> getSecondLevelMap() {
        return secondLevelMap;
    }

    public void setSecondLevelMap(HashMap<Integer, HashMap<Long, Integer>> secondLevelMap) {
        this.secondLevelMap = secondLevelMap;
    }

    public HashMap<Integer, Set<Long>> getPartitionedVertices() {
        return partitionedVertices;
    }

    public void setPartitionedVertices(HashMap<Integer, Set<Long>> partitionedVertices) {
        this.partitionedVertices = partitionedVertices;
    }

    public HashMap<Integer, Double> getLongestLengthOfPartition() {
        return longestLengthOfPartition;
    }

    public void setLongestLengthOfPartition(HashMap<Integer, Double> longestLengthOfPartition) {
        this.longestLengthOfPartition = longestLengthOfPartition;
    }

    @Override
    public String toString() {
        return "DISONGlobalIndex{" +
                "globalPartitionNum=" + globalPartitionNum +
                ", partitionSize=" + partitionSize +
                '}';
    }

    public ArrayList<Tuple<Integer, LinkedList<DISONTrajectory>>> buildGlobalIndex(List<DISONTrajectory> trajectories) {
        ArrayList<Tuple<Integer, LinkedList<DISONTrajectory>>> partitionResults = new ArrayList<>();
        LinkedList<LinkedList<DISONTrajectory>> buckets = STRPartition(trajectories, 0);
        int indexFirstCount = 0;

        int partitionNum = 0;
        for (LinkedList<DISONTrajectory> bucket : buckets) {
            System.out.println("Bucket size: " + bucket.size());
            for (DISONTrajectory trajectory : bucket) {
                firstLevelMap.put(trajectory.getGlobalIndexedPivot().get(0).getId(), indexFirstCount);
            }

            LinkedList<LinkedList<DISONTrajectory>> subBuckets = STRPartition(bucket, 1);
            System.out.println("SubBucket size: " + subBuckets.size());

            HashMap<Long, Integer> Map = new HashMap<>(subBuckets.size());

            for (LinkedList<DISONTrajectory> subBucket : subBuckets) {
                LinkedList<DISONTrajectory> partitionTrajectories = new LinkedList<>();
                Set<Long> subBucketVertexIds = new HashSet<>();
                double longestLength = -1.0;
                for (DISONTrajectory trajectory : subBucket) {
                    partitionTrajectories.add(trajectory);
                    Map.put(trajectory.getGlobalIndexedPivot().get(1).getId(), partitionNum);
                    subBucketVertexIds.add(trajectory.getGlobalIndexedPivot().get(1).getId());
                    longestLength = Math.max(longestLength, trajectory.getLength());
                }

                partitionResults.add(new Tuple<>(partitionNum, partitionTrajectories));
                longestLengthOfPartition.put(partitionNum, longestLength);
                partitionedVertices.put(partitionNum, subBucketVertexIds);
                partitionNum++;
            }
            secondLevelMap.put(indexFirstCount, Map);
            indexFirstCount++;
        }

        this.partitionSize = partitionNum;
        return partitionResults;
    }

    public void buildGlobalIndexDistributed(List<Tuple3<Integer, Integer, DISONTrajectory>> trajectoriesWithIndex) {
        for (Tuple3<Integer, Integer, DISONTrajectory> trajectoryWithIndex : trajectoriesWithIndex) {
            firstLevelMap.put(trajectoryWithIndex.f2.getGlobalIndexedPivot().get(0).getId(), trajectoryWithIndex.f0);
            if (secondLevelMap.get(trajectoryWithIndex.f0) == null) {
                HashMap<Long, Integer> subPartitionIndex = new HashMap<>();
                secondLevelMap.put(trajectoryWithIndex.f0, subPartitionIndex);
            }
            secondLevelMap.get(trajectoryWithIndex.f0).put(trajectoryWithIndex.f2.getGlobalIndexedPivot().get(1).getId(), trajectoryWithIndex.f1);

            if (partitionedVertices.get(trajectoryWithIndex.f1) == null) {
                HashSet<Long> subPartitionIdSet = new HashSet<>();
                partitionedVertices.put(trajectoryWithIndex.f1, subPartitionIdSet);
            }
            partitionedVertices.get(trajectoryWithIndex.f1).add(trajectoryWithIndex.f2.getGlobalIndexedPivot().get(1).getId());

            if (longestLengthOfPartition.get(trajectoryWithIndex.f1) == null) {
                partitionSize++;
                longestLengthOfPartition.put(trajectoryWithIndex.f1, - 1.0);
            }

            if (longestLengthOfPartition.get(trajectoryWithIndex.f1) < trajectoryWithIndex.f2.getLength()) {
                longestLengthOfPartition.put(trajectoryWithIndex.f1, trajectoryWithIndex.f2.getLength());
            }
        }
    }

    public List<Integer> searchGlobalIndex(DISONTrajectory query, HashMap<Long, Road> nodes, double threshold) {

        List<Point> globalIndexedPivots = query.getGlobalIndexedPivot();
        double limitedDistance = query.getLength() * (1 - threshold) / threshold;

        long start = System.currentTimeMillis();
        HashMap<Long, Double> firstPointRangeDistanceMap = dijkstraSearch(globalIndexedPivots.get(0).getId(), nodes, limitedDistance);
        HashMap<Long, Double> lastPointRangeDistanceMap = dijkstraSearch(globalIndexedPivots.get(1).getId(), nodes, limitedDistance);
        long end = System.currentTimeMillis();
//        System.out.println("dijk " + (end - start));


//        long start2 = System.currentTimeMillis();
        Set<Long> firstSet = new HashSet<>();
        firstSet.addAll(firstPointRangeDistanceMap.keySet());
        firstSet.retainAll(firstLevelMap.keySet());

        Set<Integer> firstPartitionSet = new HashSet<>();

        for (Long vertexFirstId : firstSet) {
            firstPartitionSet.add(firstLevelMap.get(vertexFirstId));
        }

        Set<Tuple2<Integer, Integer>> candidatePartitions = new HashSet<>();

        for (Integer firstPartitionId : firstPartitionSet) {
            Set<Long> secondSet = new HashSet<>();
            HashMap<Long, Integer> secondPartitionHashMap = secondLevelMap.get(firstPartitionId);
            secondSet.addAll(lastPointRangeDistanceMap.keySet());
            secondSet.retainAll(secondPartitionHashMap.keySet());
            Set<Integer> secondPartitionID = new HashSet<>();
            for (Long vertexLastId : secondSet) {
                if(!secondPartitionID.contains(secondPartitionHashMap.get(vertexLastId))) {
                    candidatePartitions.add(new Tuple2<>(firstPartitionId, secondPartitionHashMap.get(vertexLastId)));
                }
            }
        }

//        for (Long vertexFirstId : firstSet) {
//            Integer firstPartitionId = firstLevelMap.get(vertexFirstId);
//            Set<Long> secondSet = new HashSet<>();
//            secondSet.addAll(lastPointRangeDistanceMap.keySet());
//            HashMap<Long, Integer> secondPartitionHashMap = secondLevelMap.get(firstPartitionId);
//            secondSet.retainAll(secondPartitionHashMap.keySet());
//            for (Long vertexLastId : secondSet) {
//                candidatePartitions.add(new Tuple2<>(firstPartitionId, secondPartitionHashMap.get(vertexLastId)));
//            }
//        }

//        long end2 = System.currentTimeMillis();
//        System.out.println("candidatePartitions size: " + candidatePartitions.size() + " time " + (end2 - start2));

        List<Integer> C = new LinkedList<>();

        long start3 = System.currentTimeMillis();
        for (Tuple2<Integer, Integer> gij : candidatePartitions) {
            Integer gi = gij.f0;
            Set<Long> vf = new HashSet<>();
            vf.addAll(firstPointRangeDistanceMap.keySet());
            vf.retainAll(secondLevelMap.get(gi).keySet());
            double df = Double.MAX_VALUE;
            for (Long vertexId : vf) {
                df = Math.min(df, firstPointRangeDistanceMap.get(vertexId));
            }

            Set<Long> vl = new HashSet<>();
            vl.addAll(lastPointRangeDistanceMap.keySet());
            vl.retainAll(partitionedVertices.get(gij.f1));
            double dl = Double.MAX_VALUE;
            for (Long vertexId : vl) {
                dl = Math.min(dl, lastPointRangeDistanceMap.get(vertexId));
            }

            if (df + dl <= limitedDistance && df + dl <= ((1 - threshold) / (1 + threshold)) * (longestLengthOfPartition.get(gij.f1) + query.getLength())) {
                C.add(gij.f1);
            }

        }
//        long end3 = System.currentTimeMillis();
//        System.out.println("part3 " + (end3 - start3));

        return C;
    }


    private LinkedList<LinkedList<DISONTrajectory>> STRPartition(List<DISONTrajectory> trajectories, int pivotNum) {
        int trajectoriesSize = trajectories.size();
        ArrayList<Tuple<DISONTrajectory, Point>> pivotTuples = new ArrayList<>(trajectoriesSize);

        for (DISONTrajectory trajectory : trajectories) {
            if (pivotNum == 0) {
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
        LinkedList<LinkedList<DISONTrajectory>> buckets = new LinkedList<>();
        int p1 = 0, p2 = 0;
        do {
            int sliceTo = Math.min(p1 + nodeCapacityLon, trajectoriesSize);
            ArrayList<Tuple<DISONTrajectory, Point>> subPoint = new ArrayList<>();
            for (int i = p1; i < sliceTo; i++) {
                subPoint.add(pivotTuples.get(i));
            }
            Long flag = pivotTuples.get(sliceTo - 1).f1.getId();
            while (sliceTo < trajectoriesSize && flag.equals(pivotTuples.get(sliceTo).f1.getId())) {
                subPoint.add(pivotTuples.get(sliceTo));
                sliceTo++;
            }
            int sliceSize = subPoint.size();
            int nodeCapacityLat = (int) Math.ceil(sliceSize * 1.0 / dim[1]);
            p2 = 0;
            do {
                int packTo = Math.min(p2 + nodeCapacityLat, sliceSize);
                subPoint.sort((o1, o2) -> new Double(o1.f1.getY()).compareTo(o2.f1.getY()));
                LinkedList<DISONTrajectory> subBuckets = new LinkedList<>();
                for (int i = p2; i < packTo; i++) {
                    subBuckets.add(subPoint.get(i).f0);
                }
                Long subFlag = subPoint.get(packTo - 1).f1.getId();
                while (packTo < sliceSize && subFlag.equals(subPoint.get(packTo).f1.getId())) {
                    subBuckets.add(subPoint.get(packTo).f0);
                    packTo++;
                }
                buckets.add(subBuckets);
                p2 = packTo;
            } while (p2 < sliceSize);
            p1 = sliceTo;
        } while (p1 < trajectoriesSize);
        return buckets;
    }

    private HashMap<Long, Double> dijkstraSearch(Long source, HashMap<Long, Road> nodes, double limitedDistance) {
        class DijkstraQueueEntry implements Comparable<DijkstraQueueEntry> {
            long nodeId;
            double cost;
            boolean inQueue;

            DijkstraQueueEntry(long nodeId, double dist) {
                this.nodeId = nodeId;
                this.cost = dist;
                this.inQueue = true;
            }

            @Override
            public int compareTo(DijkstraQueueEntry j) {
                if (this.cost < j.cost) {
                    return -1;
                } else if (this.cost > j.cost) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

        HashMap<Long, Double> records = new HashMap<>();

        HashMap<Long, DijkstraQueueEntry> queueEntry = new HashMap<>(nodes.keySet().size());

        HashMap<Long, Double> distRecords = new HashMap<>(nodes.keySet().size());

        DijkstraQueueEntry sourceEntry = new DijkstraQueueEntry(source, 0.0);
        queueEntry.put(source, sourceEntry);
        distRecords.put(source, 0.0);

        PriorityQueue<DijkstraQueueEntry> queue = new PriorityQueue<>();

        queue.add(sourceEntry);

        while (!queue.isEmpty()) {
            DijkstraQueueEntry entry = queue.poll();
            entry.inQueue = false;

            if (entry.cost > limitedDistance) {
                continue;
            }

            Double distance = distRecords.get(entry.nodeId);
            records.put(entry.nodeId, distance);

            Iterator<Road> roads = null;
            if (nodes.get(entry.nodeId) == null) {
                continue;
            } else {
                roads = nodes.get(entry.nodeId).neighbors();
            }


            while (roads.hasNext()) {
                Road next = roads.next();
                if (queueEntry.containsKey(next.target())) {
                    DijkstraQueueEntry v = queueEntry.get(next.target());
                    if (!v.inQueue) {
                        continue;
                    }
                    double cost = entry.cost + next.length();

                    if (v.cost > cost) {
                        queue.remove(v);
                        v.cost = cost;
                        distRecords.put(next.target(), v.cost);
                        queue.add(v);
                    }
                } else {
                    double cost = entry.cost + next.length();
                    DijkstraQueueEntry v = new DijkstraQueueEntry(next.target(), cost);
                    queueEntry.put(next.target(), v);
                    distRecords.put(next.target(), cost);
                    queue.add(v);
                }
            }
        }

        return records;
    }

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
    public static DISONGlobalIndex load(String file) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
        DISONGlobalIndex globalIndex = (DISONGlobalIndex) inputStream.readObject();
        inputStream.close();
        return globalIndex;
    }


}
