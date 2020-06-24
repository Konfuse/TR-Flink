package com.konfuse.road;

import org.apache.flink.api.java.tuple.Tuple2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author todd
 * @date 2020/6/14 9:19
 * @description: TODO
 */
public class TopologyRoadMap implements Serializable {
    private HashMap<Long, Road> roadsMap;
    private ArrayList<Tuple2<Long, Long>> nodesMap;
    private ArrayList<Tuple2<Long, Long>> successors;
    private ArrayList<Tuple2<Long, Long>> neighbors;

    public TopologyRoadMap() {
        roadsMap = new HashMap<>();
        nodesMap = new ArrayList<>();
    }

    public TopologyRoadMap(HashMap<Long, Road> roadsMap, HashMap<Long, Road> nodesMap) {
        this.roadsMap = roadsMap;
        this.nodesMap = new ArrayList<>(nodesMap.entrySet().size());
        for (Map.Entry<Long, Road> longRoadEntry : nodesMap.entrySet()) {
            if (longRoadEntry.getValue() != null) {
                this.nodesMap.add(new Tuple2<>(longRoadEntry.getKey(), longRoadEntry.getValue().id()));
            }
            else {
                this.nodesMap.add(new Tuple2<>(longRoadEntry.getKey(), null));
            }
        }
        this.successors = new ArrayList<>(roadsMap.entrySet().size());
        this.neighbors = new ArrayList<>(roadsMap.entrySet().size());
        for (Map.Entry<Long, Road> longRoadEntry : roadsMap.entrySet()) {
            if (longRoadEntry.getValue().successor() != null) {
                successors.add(new Tuple2<>(longRoadEntry.getKey(), longRoadEntry.getValue().successor().id()));
            } else {
                successors.add(new Tuple2<>(longRoadEntry.getKey(), null));
            }
            if (longRoadEntry.getValue().neighbor() != null) {
                neighbors.add(new Tuple2<>(longRoadEntry.getKey(), longRoadEntry.getValue().neighbor().id()));
            } else {
                neighbors.add(new Tuple2<>(longRoadEntry.getKey(), null));
            }
        }
    }

    public HashMap<Long, Road> getRoadsMap() {
        return roadsMap;
    }

    public void setRoadsMap(HashMap<Long, Road> roadsMap) {
        this.roadsMap = roadsMap;
    }

    public ArrayList<Tuple2<Long, Long>> getNodesMap() {
        return nodesMap;
    }

    public void setNodesMap(ArrayList<Tuple2<Long, Long>> nodesMap) {
        this.nodesMap = nodesMap;
    }

    public ArrayList<Tuple2<Long, Long>> getSuccessors() {
        return successors;
    }

    public void setSuccessors(ArrayList<Tuple2<Long, Long>> successors) {
        this.successors = successors;
    }

    public ArrayList<Tuple2<Long, Long>> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(ArrayList<Tuple2<Long, Long>> neighbors) {
        this.neighbors = neighbors;
    }

    public HashMap<Long, Road> constructNodesMap() {
        HashMap<Long, Road> nodes = new HashMap<>(nodesMap.size());
        int size = roadsMap.entrySet().size();
        for (int i = 0; i < size; i++) {
            roadsMap.get(successors.get(i).f0).successor(roadsMap.get(successors.get(i).f1));
            roadsMap.get(neighbors.get(i).f0).neighbor(roadsMap.get(neighbors.get(i).f1));
        }
        for (Tuple2<Long, Long> pointToRoadTuple : nodesMap) {
            nodes.put(pointToRoadTuple.f0, roadsMap.get(pointToRoadTuple.f1));
        }
        return nodes;
    }
    
}
