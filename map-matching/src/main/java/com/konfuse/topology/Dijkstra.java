package com.konfuse.topology;


import com.konfuse.road.LocationOnRoad;
import com.konfuse.road.Road;
import com.konfuse.util.Triple;
import com.konfuse.util.Tuple;

import java.util.*;

/**
 * @Author: Konfuse
 * @Date: 2020/1/2 12:37
 */
public class Dijkstra {
    private class PathEntry extends Triple<Road, Road, Double> implements Comparable<PathEntry>{
        PathEntry(Road mark, Road predecessor,double distance) {
            super(mark, predecessor, distance);
        }

        @Override
        public int compareTo(PathEntry other) {
            return (this.f2 < other.f2) ? -1 : (this.f2 > other.f2) ? 1 : 0;
        }
    }

    public List<Road> shortestPath(LocationOnRoad<Road> source, LocationOnRoad<Road> target, Cost cost, Double max){
        return shortestPath(source, new HashSet<>(Arrays.asList(target)), cost, max).get(target);
    }

    public Map<LocationOnRoad<Road>, List<Road>> shortestPath(LocationOnRoad<Road> source, Set<LocationOnRoad<Road>> targets, Cost cost, Double max){
        Map<LocationOnRoad<Road>, Tuple<LocationOnRoad<Road>, List<Road>>> map =
                shortestPath(new HashSet<>(Arrays.asList(source)), targets, cost, max);
        Map<LocationOnRoad<Road>, List<Road>> result = new HashMap<>();
        for (Map.Entry<LocationOnRoad<Road>, Tuple<LocationOnRoad<Road>, List<Road>>> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().f1);
        }
        return result;
    }

    public Map<Long, Double> shortestDistance(LocationOnRoad<Road> source, Set<LocationOnRoad<Road>> targets, Cost cost, Double max){
        Map<Long, Double> shortestDistances = new HashMap<>();
        for(LocationOnRoad<Road> target : targets){
            shortestDistances.put(target.road().id(), shortestDistance(source, target, cost, max));
        }
        return shortestDistances;
    }

    public double shortestDistance(LocationOnRoad<Road> source, LocationOnRoad<Road> target, Cost cost, Double max){
        double distanceToEndVertexOfSource = cost.cost(source.road(), 1 -  source.fraction());
        double distanceFromStartVertexOfDestinationToTarget = cost.cost(target.road(), target.fraction());
        if(source.road().id() == target.road().id()) {
            if(source.fraction() < target.fraction()) {
                return 2 * source.road().length() - distanceToEndVertexOfSource +  distanceFromStartVertexOfDestinationToTarget;
            }
            else{
                return distanceFromStartVertexOfDestinationToTarget - distanceToEndVertexOfSource;
            }
        }

        List<Road> shortestPath = shortestPath(source, target, cost, max);

        if(shortestPath == null){
            return Double.MAX_VALUE;
        }
        double pathDistance = 0.0;
        for(int i = 1; i < shortestPath.size() - 1; i++){
            pathDistance += shortestPath.get(i).length();
        }

        return distanceToEndVertexOfSource + pathDistance + distanceFromStartVertexOfDestinationToTarget;
    }

    public double shortestDistance(Road source, Road target, Cost cost, Map map){
        PriorityQueue<PathEntry> priorities = new PriorityQueue<>();
        Map<Road, PathEntry> entries = new HashMap<>();

        return 0;
    }

    public Map<LocationOnRoad<Road>, Tuple<LocationOnRoad<Road>, List<Road>>> shortestPath(Set<LocationOnRoad<Road>> sources, Set<LocationOnRoad<Road>> targets, Cost cost, Double max){
        for (LocationOnRoad<Road> source : sources) {
            System.out.println("source id ： "  + source.road().id());
        }
        Map<Road, Set<LocationOnRoad<Road>>> targetEdges = new HashMap<>();
        for (LocationOnRoad<Road> target : targets) {
            if (!targetEdges.containsKey(target.road())) {
                targetEdges.put(target.road(), new HashSet<>(Arrays.asList(target)));
            } else {
                targetEdges.get(target.road()).add(target);
            }
        }
        for (Map.Entry<Road, Set<LocationOnRoad<Road>>> target : targetEdges.entrySet()) {
            System.out.println("target id ： " + target.getKey().id());
        }

        /*
         * Setup data structures
         */
        PriorityQueue<PathEntry> priorities = new PriorityQueue<>();
        Map<Road, PathEntry> entries = new HashMap<>();
        Map<LocationOnRoad<Road>, PathEntry> finishs = new HashMap<>();
        Map<PathEntry, LocationOnRoad<Road>> reaches = new HashMap<>();
        Map<PathEntry, LocationOnRoad<Road>> starts = new HashMap<>();

        /*
         * Initialize map of edges with start points
         */
        for (LocationOnRoad<Road> source : sources) { // initialize sources as start edges
            double startcost = cost.cost(source.road(), 1 - source.fraction());

            if (targetEdges.containsKey(source.road())) { // start edge reaches target edge
                for (LocationOnRoad<Road> target : targetEdges.get(source.road())) {
                    if (target.fraction() < source.fraction()) {
                        continue;
                    }
                    double reachcost = startcost - cost.cost(source.road(), 1 - target.fraction());

                    PathEntry reach = new PathEntry(source.road(), null, reachcost);
                    reaches.put(reach, target);
                    starts.put(reach, source);
                    priorities.add(reach);
                }
            }

            PathEntry start = entries.get(source.road());
            if (start == null) {
                start = new PathEntry(source.road(), null, startcost);
                entries.put(source.road(), start);
                starts.put(start, source);
                priorities.add(start);
            } else if (startcost < start.f2) {
                start = new PathEntry(source.road(), null, startcost);
                entries.put(source.road(), start);
                starts.put(start, source);
                priorities.remove(start);
                priorities.add(start);
            }
        }

        while (priorities.size() > 0) {
            PathEntry current = priorities.poll();

            if (targetEdges.isEmpty()) {
                System.out.println("finshed all targets");
                break;
            }

            if (max != null && current.f2 > max) {
                System.out.println("reached maximum bound");
                break;
            }

            /*
             * Finish target if reached.
             */
            if (reaches.containsKey(current)) {
                LocationOnRoad<Road> target = reaches.get(current);

                if (finishs.containsKey(target)) {
                    continue;
                } else {

                    finishs.put(target, current);

                    Set<LocationOnRoad<Road>> edges = targetEdges.get(current.f0);
                    edges.remove(target);

                    if (edges.isEmpty()) {
                        targetEdges.remove(current.f0);
                    }
                    continue;
                }
            }

            Iterator<Road> successors = current.f0.successors();

            while (successors.hasNext()) {
                Road successor = successors.next();

                double succcost = current.f2 + cost.cost(successor);

                if (targetEdges.containsKey(successor)) { // reach target edge
                    for (LocationOnRoad<Road> target : targetEdges.get(successor)) {
                        double reachcost = succcost - cost.cost(successor, 1 - target.fraction());

                        PathEntry reach = new PathEntry(successor, current.f0, reachcost);
                        reaches.put(reach, target);
                        priorities.add(reach);
                    }
                }

                if (!entries.containsKey(successor)) {
                    PathEntry mark = new PathEntry(successor, current.f0, succcost);
                    entries.put(successor, mark);
                    priorities.add(mark);
                }
            }
        }

        Map<LocationOnRoad<Road>, Tuple<LocationOnRoad<Road>, List<Road>>> paths = new HashMap<>();

        for (LocationOnRoad<Road> target : targets) {
            if (!finishs.containsKey(target)) {
                paths.put(target, null);
            } else {
                LinkedList<Road> path = new LinkedList<>();
                PathEntry iterator = finishs.get(target);
                PathEntry start = null;
                while (iterator != null) {
                    path.addFirst(iterator.f0);
                    start = iterator;
                    iterator = iterator.f1 != null ? entries.get(iterator.f1) : null;
                }
                paths.put(target, new Tuple<LocationOnRoad<Road>, List<Road>>(starts.get(start), path));
            }
        }

//        System.out.println("path size: " + paths.size());
//        for(LocationOnRoad<Road> location : paths.keySet()){
//            System.out.println("Target ID: "  + location.road().id());
//            LocationOnRoad<Road> pathSource = paths.get(location).f0;
//            System.out.println("Source ID: " + pathSource.road().id());
//            List<Road> roads_ = paths.get(location).f1;
//            System.out.println("Path Size: " + roads_.size());
//            for(Road road_ : roads_){
//                System.out.println(road_.id());
//            }
//        }

        entries.clear();
        finishs.clear();
        reaches.clear();
        priorities.clear();

        return paths;
    }
}
