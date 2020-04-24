package com.konfuse.lmm;

import com.konfuse.road.Road;
import com.konfuse.road.RoadMap;
import com.konfuse.util.Tuple;

import java.io.*;
import java.util.*;

/**
 * @Author: Konfuse
 * @Date: 2020/4/24 14:15
 */
public class UndirectedIndex {
    private Label[] index;

    public UndirectedIndex(Label[] index) {
        this.index = index;
    }

    public double query(long source, long target) {
        double distance = Double.MAX_VALUE;

        int size = index.length - 1;
        if (source > size || target > size || source <= 0 || target <= 0) {
            return source == target ? 0 : distance;
        }

        final Label label_s = index[(int) source], label_t = index[(int) target];

        if (label_s != null && label_t != null) {
            for (int is = 0, it = 0; ; ) {
                long v1 = label_s.pairs[is].nodeId, v2 = label_t.pairs[it].nodeId;
                if (v1 == v2) {
                    if (v1 == size + 1) {
                        break;
                    }
                    double cost = label_s.pairs[is].cost + label_t.pairs[it].cost;
                    if (cost < distance) distance = cost;
                    ++is;
                    ++it;
                } else {
                    is += v1 < v2 ? 1 : 0;
                    it += v1 > v2 ? 1 : 0;
                }
            }
        }

        return distance;
    }

    public static UndirectedIndex constructLabel(RoadMap map) {
        HashMap<Long, Integer> nodes = map.getNodesDegree();
//        int size = 2 << (Integer.toBinaryString(nodes.size() - 1).length() - 1);
        int size = nodes.size();
        System.out.println("nodes size is: " + nodes.size());

        TempLabel[] tempIndex = new TempLabel[size + 1];

        // path table记录从起始点nodeId开始，到其余节点的距离信息
        double[] pathTable = new double[size + 1];

        // sort vertices 对节点按照节点的degree从大到小排序
        List<Tuple<Long, Integer>> list = new LinkedList<>();

        // 创建存储访问过节点的集合，通过保存已经访问的元素，重新初始化path table
        long[] visited = new long[size + 1];

        // 用于表示节点是否在队列中，如果在队列中则表示为true，否则为false
        boolean[] inQueue = new boolean[size + 1];

        // 数组T记录了从当前遍历节点v出发，或者到达v的，所有节点的距离
        double[] T = new double[size + 2];

        for (Map.Entry<Long, Integer> entry : nodes.entrySet()) {
            long nodeId = entry.getKey();
            int degree = entry.getValue();
            pathTable[(int) nodeId] = Double.MAX_VALUE;
            T[(int) nodeId] = Double.MAX_VALUE;
            list.add(new Tuple<>(nodeId, degree));
        }
        pathTable[0] = Double.MAX_VALUE;
        T[0] = Double.MAX_VALUE;

        // 按照排序的节点顺序依次做pured dijkstra
        list.sort((o1, o2) -> o2.f1.compareTo(o1.f1));
        int count = 1;
        for (Tuple<Long, Integer> tuple : list) {
            long nodeId = tuple.f0;
            System.out.println("travel the " + nodeId + "th node...; " + (list.size() - count) + " nodes remained.");
            prunedDijkstra(map.getNodesOut(), nodeId, tempIndex, pathTable, inQueue, T, visited);
            ++count;
        }

        // 将生成的索引导入
        Label[] index = new Label[size + 1];
        Pair.PairComparator comparator = new Pair.PairComparator();
        for (int i = 1; i <= size; i++) {
            List<Pair> temp = tempIndex[i].pairs;
            index[i] = new Label(temp.size());
            temp.sort(comparator);
            index[i].pairs = temp.toArray(new Pair[0]);
            temp.clear();
        }

        return new UndirectedIndex(index);
    }

    private static Long queuePoll(double[] pathTable, boolean[] inQueue) {
        int size = pathTable.length;
        Long v = null;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            if (inQueue[i] && pathTable[i] < min) {
                min = pathTable[i];
                v = (long) i;
            }
        }
        if (v != null) {
            inQueue[Math.toIntExact(v)] = false;
        }

        return v;
    }

    public static void prunedDijkstra(Map<Long, Road> nodesOut, long v, TempLabel[] tempIndex, double[] pathTable, boolean[] inQueue, double[] T, long[] visited) {
        int visited_size = 0, size = nodesOut.size();
        Long u;

        // 初始化路径表中，当前节点v的代价为0，其它的值为无穷大
        pathTable[(int) v] = 0.0;

        // inQueue表示节点是否在队列中的标记，如果在队列中，则将该点设置为true，否则为false
        inQueue[(int) v] = true;

        // 数组T记录了从节点v出发的能到达所有节点的距离
        TempLabel label_v = tempIndex[(int) v];
        if (label_v != null) {
            for (Pair pair : label_v.pairs) {
                T[(int) pair.nodeId] = pair.cost;
            }
        }

        while ((u = queuePoll(pathTable, inQueue)) != null) {
            visited[visited_size++] = u;

            double distance = Double.MAX_VALUE;
            if (label_v != null) {
                TempLabel label_u = tempIndex[Math.toIntExact(u)];
                if (label_u != null) {
                    for (Pair pair : label_u.pairs) {
                        distance = (T[(int) pair.nodeId] == Double.MAX_VALUE) ? distance :
                                Math.min((pair.cost + T[(int) pair.nodeId]), distance);
                    }
                }
            }
            if (distance <= pathTable[Math.toIntExact(u)]) {
                continue;
            }

            if (tempIndex[Math.toIntExact(u)] == null) {
                // 为设置索引的每一个Label结尾设置哨兵，nodeId = sizeOf(nodes) + 1, 值为Double.MAX_VALUE
                tempIndex[Math.toIntExact(u)] = new TempLabel(size  + 1, Double.MAX_VALUE);
            }
            tempIndex[Math.toIntExact(u)].add(v, pathTable[Math.toIntExact(u)]);

            if (nodesOut.get(u) == null) {
                continue;
            }
            Iterator<Road> roads = nodesOut.get(u).neighbors();
            while (roads.hasNext()) {
                Road next = roads.next();
                double cost = pathTable[Math.toIntExact(u)] + next.length();
                long w = next.target();
                double w_cost = pathTable[(int) w];
                if (cost < w_cost) {
                    pathTable[(int) w] = cost;
                    inQueue[(int) w] = true;
                }
            }
        }

        for (int i = 0; i < visited_size; i++) {
            pathTable[(int) visited[i]] = Double.MAX_VALUE;
            inQueue[(int) visited[i]] = false;
        }

        if (label_v != null) {
            for (Pair pair : label_v.pairs) {
                T[(int) pair.nodeId] = Double.MAX_VALUE;
            }
        }
    }

    public void store(String indexPath) {
        BufferedWriter indexWriter = null;
        try {
            indexWriter = new BufferedWriter(new FileWriter(indexPath));
            int size = index.length;
            indexWriter.write("size:" + (size - 1));
            indexWriter.newLine();
            for (int i = 1; i < size; i++) {
                indexWriter.write(i + ":" + index[i].toString());
                indexWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (indexWriter != null) {
                    indexWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static UndirectedIndex read(String indexPath) {
        BufferedReader indexReader = null;
        Label[] index = null;
        String line;
        int size;

        try {
            indexReader = new BufferedReader(new FileReader(indexPath));
            line = indexReader.readLine();
            size = Integer.parseInt(line.split(":")[1]);
            index = new Label[size + 1];
            while ((line = indexReader.readLine()) != null) {
                String[] elements = line.split(":");
                long v = Long.parseLong(elements[0]);

                elements = elements[1].split(";");
                Pair[] pairs = new Pair[elements.length];
                int i = 0;
                for (String element : elements) {
                    String[] pair = element.split(",");
                    long nodeId = Long.parseLong(pair[0]);
                    double cost = Double.parseDouble(pair[1]);
                    pairs[i++] = new Pair(nodeId, cost);
                }
                index[(int) v] = new Label(pairs);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (indexReader != null) {
                    indexReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new UndirectedIndex(index);
    }
}
