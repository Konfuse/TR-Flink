package com.konfuse.lmm;

import com.konfuse.road.Road;
import com.konfuse.road.RoadMap;
import com.konfuse.util.Tuple;

import java.io.*;
import java.util.*;

/**
 * @Author: Konfuse
 * @Date: 2020/4/23 22:04
 */
public class DirectedHashIndex {
    private HashMap<Long, HashMap<Long, Double>> labelIn;
    private HashMap<Long, HashMap<Long, Double>> labelOut;

    public DirectedHashIndex() {
    }

    public HashMap<Long, HashMap<Long, Double>> getLabelIn() {
        return labelIn;
    }

    public void setLabelIn(HashMap<Long, HashMap<Long, Double>> labelIn) {
        this.labelIn = labelIn;
    }

    public HashMap<Long, HashMap<Long, Double>> getLabelOut() {
        return labelOut;
    }

    public void setLabelOut(HashMap<Long, HashMap<Long, Double>> labelOut) {
        this.labelOut = labelOut;
    }

    public double query(long source, long target) {
        double distance = Double.MAX_VALUE;
        if (labelOut.containsKey(source) && labelIn.containsKey(target)) {
            HashMap<Long, Double> lout = labelOut.get(source);
            HashMap<Long, Double> lin = labelIn.get(target);
            for (Map.Entry<Long, Double> entry : lout.entrySet()) {
                if (lin.containsKey(entry.getKey())) {
                    long v = entry.getKey();
                    double dis_sv = entry.getValue();
                    double dis_vt = lin.get(v);
                    distance = Math.min(distance, dis_sv + dis_vt);
                }
            }
        }
        return distance;
    }

    public static DirectedHashIndex constructLabel(RoadMap map) {
        HashMap<Long, Integer> nodes = map.getNodesDegree();
        int size = nodes.size();
        System.out.println("nodes size is: " + size);

        DirectedHashIndex label = new DirectedHashIndex();
        label.setLabelIn(new HashMap<>(size));
        label.setLabelOut(new HashMap<>(size));

        // path table记录从起始点nodeId开始，到其余节点的距离信息
        Map<Long, Double> pathTable = new HashMap<>(size);

        // sort vertices 对节点按照节点的degree从大到小排序
        PriorityQueue<Tuple<Long, Integer>> queue = new PriorityQueue<>(size, (o1, o2) -> o2.f1.compareTo(o1.f1));

        for (Map.Entry<Long, Integer> entry : nodes.entrySet()) {
            long nodeId = entry.getKey();
            int degree = entry.getValue();
            pathTable.put(nodeId, Double.MAX_VALUE);
            queue.add(new Tuple<>(nodeId, degree));
        }

        // 按照排序的节点顺序依次做pured dijkstra
        while (!queue.isEmpty()) {
            long nodeId = queue.poll().f0;
            System.out.println("travel the " + nodeId + "th node...; " + queue.size() + " nodes remained.");
            prunedDijkstra(map, nodeId, label, pathTable, size);
            prunedDijkstraReverse(map, nodeId, label, pathTable, size);
        }

        return label;
    }

    private static class NodeEntry {
        long nodeId;
        double cost;

        public NodeEntry(long nodeId, double cost) {
            this.nodeId = nodeId;
            this.cost = cost;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NodeEntry)) return false;
            NodeEntry nodeEntry = (NodeEntry) o;
            return nodeId == nodeEntry.nodeId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, cost);
        }
    }

    public static void prunedDijkstra(RoadMap map, long v, DirectedHashIndex label, Map<Long, Double> pathTable, int size) {
        Map<Long, Road> nodesOut = map.getNodesOut();
        Set<Long> visited = new HashSet<>(size);

        // 获取需要更新的label表
        HashMap<Long, HashMap<Long, Double>> labelIn = label.getLabelIn();

        // 初始化路径表中，当前节点v的代价为0，其它的值为无穷大
        pathTable.replace(v, 0.0);

        // 优先级队列queue，记录已遍历点集内的所有点到起始点nodeId的最短路径信息
        PriorityQueue<NodeEntry> queue = new PriorityQueue<>(size, Comparator.comparingDouble(o -> o.cost));
        queue.add(new NodeEntry(v, 0.0));

        while (!queue.isEmpty()) {
            long u = queue.poll().nodeId;
            visited.add(u);
            if (label.query(v, u) <= pathTable.get(u)) {
                continue;
            }

            if (!labelIn.containsKey(u)) {
                labelIn.put(u, new HashMap<>());
            }
            labelIn.get(u).put(v, pathTable.get(u));

            if (nodesOut.get(u) == null) {
                continue;
            }
            Iterator<Road> roads = nodesOut.get(u).neighbors();
            while (roads.hasNext()) {
                Road next = roads.next();
                double cost = pathTable.get(u) + next.length();
                long w = next.target();
                double w_cost = pathTable.get(w);
                if (cost < w_cost) {
                    pathTable.replace(w, cost);
                    NodeEntry entry = new NodeEntry(w, cost);
                    if (w_cost != Double.MAX_VALUE) {
                        queue.remove(entry);
                    }
                    queue.add(entry);
                }
            }
        }

        for (Long nodeId : visited) {
            pathTable.replace(nodeId, Double.MAX_VALUE);
        }
    }

    public static void prunedDijkstraReverse(RoadMap map, long v, DirectedHashIndex label, Map<Long, Double> pathTable, int size) {
        HashMap<Long, Road> nodesIn = map.getNodesIn();
        Set<Long> visited = new HashSet<>(size);

        // 获取需要更新的label表
        HashMap<Long, HashMap<Long, Double>> labelOut = label.getLabelOut();

        // 初始化路径表中，当前节点v的代价为0，其它的值为无穷大
        pathTable.replace(v, 0.0);

        // 优先级队列queue，记录已遍历点集内的所有点到起始点nodeId的最短路径信息
        PriorityQueue<NodeEntry> queue = new PriorityQueue<>(size, Comparator.comparingDouble(o -> o.cost));
        queue.add(new NodeEntry(v, 0.0));

        while (!queue.isEmpty()) {
            long u = queue.poll().nodeId;
            visited.add(u);
            if (label.query(v, u) <= pathTable.get(u)) {
                continue;
            }

            if (!labelOut.containsKey(u)) {
                labelOut.put(u, new HashMap<>());
            }
            labelOut.get(u).put(v, pathTable.get(u));

            if (nodesIn.get(u) == null) {
                continue;
            }
            Iterator<Road> roads = nodesIn.get(u).preneighbors();
            while (roads.hasNext()) {
                Road next = roads.next();
                double cost = pathTable.get(u) + next.length();
                long w = next.source();
                double w_cost = pathTable.get(w);
                if (cost < w_cost) {
                    pathTable.replace(w, cost);
                    NodeEntry entry = new NodeEntry(w, cost);
                    if (w_cost != Double.MAX_VALUE) {
                        queue.remove(entry);
                    }
                    queue.add(entry);
                }
            }
        }

        for (Long nodeId : visited) {
            pathTable.replace(nodeId, Double.MAX_VALUE);
        }
    }

    public void store(String labelInPath, String labelOutPath) {
        BufferedWriter labelInWriter = null, labelOutWriter = null;
        try {
            labelInWriter = new BufferedWriter(new FileWriter(labelInPath));
            for (Map.Entry<Long, HashMap<Long, Double>> entry : labelIn.entrySet()) {
                labelInWriter.write(entry.getKey() + ":");
                for (Map.Entry<Long, Double> pair : entry.getValue().entrySet()) {
                    labelInWriter.write(pair.getKey() + "," + pair.getValue() + ";");
                }
                labelInWriter.newLine();
            }

            labelOutWriter = new BufferedWriter(new FileWriter(labelOutPath));
            for (Map.Entry<Long, HashMap<Long, Double>> entry : labelOut.entrySet()) {
                labelOutWriter.write(entry.getKey() + ":");
                for (Map.Entry<Long, Double> pair : entry.getValue().entrySet()) {
                    labelOutWriter.write(pair.getKey() + "," + pair.getValue() + ";");
                }
                labelOutWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (labelInWriter != null) {
                    labelInWriter.close();
                }

                if (labelOutWriter != null) {
                    labelOutWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static DirectedHashIndex read(String labelInPath, String labelOutPath) {
        BufferedReader labelInReader = null, labelOutReader = null;
        DirectedHashIndex label = new DirectedHashIndex();
        HashMap<Long, HashMap<Long, Double>> labelIn = new HashMap<>();
        HashMap<Long, HashMap<Long, Double>> labelOut = new HashMap<>();

        try {
            labelInReader = new BufferedReader(new FileReader(labelInPath));
            String line;
            while ((line = labelInReader.readLine()) != null) {
                String[] elements = line.split(":");
                long node = Long.parseLong(elements[0]);
                HashMap<Long, Double> pairs = new HashMap<>();

                elements = elements[1].split(";");
                for (String element : elements) {
                    String[] pair = element.split(",");
                    pairs.put(Long.parseLong(pair[0]), Double.parseDouble(pair[1]));
                }
                labelIn.put(node, pairs);
            }

            labelOutReader = new BufferedReader(new FileReader(labelOutPath));
            while ((line = labelInReader.readLine()) != null) {
                String[] elements = line.split(":");
                long node = Long.parseLong(elements[0]);
                HashMap<Long, Double> pairs = new HashMap<>();

                elements = elements[1].split(";");
                for (String element : elements) {
                    String[] pair = element.split(",");
                    pairs.put(Long.parseLong(pair[0]), Double.parseDouble(pair[1]));
                }
                labelOut.put(node, pairs);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (labelInReader != null) {
                    labelInReader.close();
                }

                if (labelOutReader != null) {
                    labelOutReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        label.setLabelIn(labelIn);
        label.setLabelOut(labelOut);
        return label;
    }
}
