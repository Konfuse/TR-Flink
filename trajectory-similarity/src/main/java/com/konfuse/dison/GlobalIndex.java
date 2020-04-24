package com.konfuse.dison;

import com.konfuse.geometry.Point;
import com.konfuse.road.Road;
import com.konfuse.util.Tuple;

import java.io.*;
import java.util.*;

/**
 * @Auther todd
 * @Date 2020/4/20
 */
public class GlobalIndex implements Serializable {
    private final int globalPartitionNum;
    private int partitionNum;
    private ArrayList<GlobalPartition> globalPartitions;
    private HashMap<Long, Integer> firstLevelMap;
    private List<HashMap<Long, Integer>> SecondLevelMapLists;
    private List<Set<Long>> lastLevelSet;
    private HashMap<Integer, Integer> indexMap;
    private ArrayList<Double> longestLengthOfPartition;

    public GlobalIndex(int globalPartitionNum) {
        this.globalPartitionNum = globalPartitionNum;
        this.partitionNum = 0;
        this.globalPartitions = new ArrayList<>();
        this.firstLevelMap = new HashMap<>();
        this.SecondLevelMapLists = new ArrayList<>();
        this.lastLevelSet = new ArrayList<>();
        this.indexMap = new HashMap<>();
        this.longestLengthOfPartition = new ArrayList<>();
    }

    public HashMap<Long, Integer> getFirstLevelMap() {
        return firstLevelMap;
    }

    public List<HashMap<Long, Integer>> getSecondLevelMapLists() {
        return SecondLevelMapLists;
    }

    public HashMap<Integer, Integer> getIndexMap() {
        return indexMap;
    }

    public int getPartitionNum() {
        return partitionNum;
    }

    public ArrayList<GlobalPartition> getGlobalPartitions() {
        return globalPartitions;
    }

    public void buildGlobalIndex(List<DISONTrajectory> trajectories) {
        ArrayList<LinkedList<Integer>> allPartitionResults = new ArrayList<>();
        LinkedList<LinkedList<Tuple<Integer, Point>>> buckets = STRFirstPartition(trajectories);
        int indexFirstCount = 0;

        for (LinkedList<Tuple<Integer, Point>> bucket : buckets) {
            for (Tuple<Integer, Point> firstPointTuple : bucket) {
                firstLevelMap.put(firstPointTuple.f1.getId(), indexFirstCount);
            }
            indexFirstCount++;
        }

        indexFirstCount = 0;
        int indexLastCount = 0;

        for (LinkedList<Tuple<Integer, Point>> bucket : buckets) {

            System.out.println("Bucket size: " + bucket.size());
            LinkedList<LinkedList<Tuple<Integer, Point>>> subBuckets = STRLastPartition(trajectories, bucket);
            System.out.println("subBucket num: " + subBuckets.size());

            HashMap<Long, Integer> SecondLevelMap = new HashMap<>(subBuckets.size());
            for (LinkedList<Tuple<Integer, Point>> subBucket : subBuckets) {
                indexMap.put(indexLastCount, indexFirstCount);
                LinkedList<Integer> trajectoryIds = new LinkedList<>();
                Set<Long> subBucketVertexIds = new HashSet<>();
                for (Tuple<Integer, Point> lastPointTuple : subBucket) {
                    trajectoryIds.add(lastPointTuple.f0);
                    SecondLevelMap.put(lastPointTuple.f1.getId(), indexLastCount);
                    subBucketVertexIds.add(lastPointTuple.f1.getId());
                }
                allPartitionResults.add(trajectoryIds);
                lastLevelSet.add(subBucketVertexIds);
                indexLastCount++;

                System.out.println("subBucket size: " + subBucket.size());
            }
            indexFirstCount++;
            SecondLevelMapLists.add(SecondLevelMap);
        }

        for (int i = 0; i < allPartitionResults.size(); i++) {
            GlobalPartition curPartition = new GlobalPartition(i, allPartitionResults.get(i));
            double longestLength = -1.0;
            for (Integer trajectoryIndex : allPartitionResults.get(i)) {
                longestLength = Math.max(longestLength, trajectories.get(trajectoryIndex).getLength());
            }
            longestLengthOfPartition.add(longestLength);
            globalPartitions.add(curPartition);
        }

        this.partitionNum = indexLastCount;
    }

    public List<Integer> searchGlobalIndex(DISONTrajectory query, HashMap<Long, Road> nodes, double threshold) {
        HashSet<Integer> results = new HashSet<>();
        List<Point> globalIndexedPivots = query.getGlobalIndexedPivot();
        double limitedDistance = query.getLength() * (1 - threshold) / threshold;
        HashMap<Long, Double> firstPointRangeDistanceMap = dijkstraSearch(globalIndexedPivots.get(0).getId(), nodes, limitedDistance);
        HashMap<Long, Double> lastPointRangeDistanceMap = dijkstraSearch(globalIndexedPivots.get(1).getId(), nodes, limitedDistance);

        Set<Long> firstSet = new HashSet<>();
        firstSet.addAll(firstPointRangeDistanceMap.keySet());
        firstSet.retainAll(firstLevelMap.keySet());

        Set<Integer> candidatePartitions = new HashSet<>();

        for (Long vertexFirstId : firstSet) {
            Set<Long> secondSet = new HashSet<>();
            secondSet.addAll(lastPointRangeDistanceMap.keySet());
            HashMap<Long, Integer> secondPartitionHashMap = SecondLevelMapLists.get(firstLevelMap.get(vertexFirstId));
            secondSet.retainAll(secondPartitionHashMap.keySet());
            for (Long vertexLastId : secondSet) {
                candidatePartitions.add(secondPartitionHashMap.get(vertexLastId));
            }
        }

        List<Integer> C = new LinkedList<>();

        for (Integer gij : candidatePartitions) {
            Integer gi = indexMap.get(gij);
            Set<Long> vf = new HashSet<>();
            vf.addAll(firstPointRangeDistanceMap.keySet());
            vf.retainAll(SecondLevelMapLists.get(gi).keySet());
            double df = Double.MAX_VALUE;
            for (Long vertexId : vf) {
                df = Math.min(df, firstPointRangeDistanceMap.get(vertexId));
            }

            Set<Long> vl = new HashSet<>();
            vl.addAll(lastPointRangeDistanceMap.keySet());
            vl.retainAll(lastLevelSet.get(gij));
            double dl = Double.MAX_VALUE;
            for (Long vertexId : vl) {
                dl = Math.min(dl, lastPointRangeDistanceMap.get(vertexId));
            }

            if (df + dl <= limitedDistance && df + dl <= ((1 - threshold) / (1 + threshold)) * (longestLengthOfPartition.get(gij) + query.getLength())) {
                C.add(gij);
            }

        }

        return C;
    }


    private LinkedList<LinkedList<Tuple<Integer, Point>>> STRFirstPartition(List<DISONTrajectory> trajectories) {
        int trajectoriesSize = trajectories.size();
        ArrayList<Tuple<Integer, Point>> firstPoints = new ArrayList<>(trajectoriesSize);

        for (DISONTrajectory trajectory : trajectories) {
            firstPoints.add(new Tuple<Integer, Point>(trajectory.getId(), trajectory.getGlobalIndexedPivot().get(0)));
        }

        int[] dim = new int[2];
        dim[0] = (int) Math.ceil(Math.sqrt(globalPartitionNum));
        dim[1] = globalPartitionNum / dim[0];
        int nodeCapacityLon = (int) Math.ceil(trajectoriesSize * 1.0 / dim[0]);
        firstPoints.sort((o1, o2) -> new Double(o1.f1.getX()).compareTo(o2.f1.getX()));
        LinkedList<LinkedList<Tuple<Integer, Point>>> buckets = new LinkedList<>();
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
                LinkedList<Tuple<Integer, Point>> subBuckets = new LinkedList<>();
                for (int i = p2; i < packTo; i++) {
                    subBuckets.add(subFirstPoint.get(i));
                }
                buckets.add(subBuckets);
                p2 = packTo;
            } while (p2 < sliceSize);
            p1 = sliceTo;
        } while (p1 < trajectoriesSize);
        return buckets;
    }

    private LinkedList<LinkedList<Tuple<Integer, Point>>> STRLastPartition(List<DISONTrajectory> trajectories, LinkedList<Tuple<Integer, Point>> SeqData) {
        int dataSize = SeqData.size();
        ArrayList<Tuple<Integer, Point>> lastPoints = new ArrayList<>(dataSize);
        for (Tuple<Integer, Point> seqDatum : SeqData) {
            lastPoints.add(new Tuple<Integer, Point>(seqDatum.f0, trajectories.get(seqDatum.f0).getGlobalIndexedPivot().get(1)));
        }
        int[] dim = new int[2];
        dim[0] = (int) Math.ceil(Math.sqrt(globalPartitionNum));
        dim[1] = globalPartitionNum / dim[0];
        int nodeCapacityLon = (int) Math.ceil(dataSize * 1.0 / dim[0]);
        lastPoints.sort((o1, o2) -> new Double(o1.f1.getX()).compareTo(o2.f1.getX()));
        LinkedList<LinkedList<Tuple<Integer, Point>>> buckets = new LinkedList<>();
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
                LinkedList<Tuple<Integer, Point>> subBuckets = new LinkedList<>();
                for (int i = p2; i < packTo; i++) {
                    subBuckets.add(subFirstPoint.get(i));
                }
                buckets.add(subBuckets);
                p2 = packTo;
            } while (p2 < sliceSize);
            p1 = sliceTo;
        } while (p1 < dataSize);
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

            if (entry.nodeId != source) {
                Double distance = distRecords.get(entry.nodeId);
                records.put(entry.nodeId, distance);
            }

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
    public static GlobalIndex load(String file) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
        GlobalIndex globalIndex = (GlobalIndex) inputStream.readObject();
        inputStream.close();
        return globalIndex;
    }


}
