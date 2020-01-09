package com.konfuse.topology;


import com.konfuse.fmm.Record;
import com.konfuse.road.Road;
import com.konfuse.road.RoadMap;
import com.konfuse.util.Quadruple;
import com.konfuse.util.Tuple;

import java.util.*;

/**
 * @Author: todd
 * @Date: 2020/1/2
 */
public class Dijkstra<E extends AbstractLink<E>, P extends LocationOnEdge<E>> {
    public List<E> route(P source, P target, Cost<E> cost) {
        return ssst(source, target, cost, null, null);
    }

    public List<E> route(P source, P target, Cost<E> cost, Cost<E> bound, Double max) {
        return ssst(source, target, cost, bound, max);
    }

    public Map<P, List<E>> route(P source, Set<P> targets, Cost<E> cost) {
        return ssmt(source, targets, cost, null, null);
    }

    public Map<P, List<E>> route(P source, Set<P> targets, Cost<E> cost, Cost<E> bound,
                                 Double max) {
        return ssmt(source, targets, cost, bound, max);
    }

    public Map<P, Tuple<P, List<E>>> route(Set<P> sources, Set<P> targets, Cost<E> cost) {
        return msmt(sources, targets, cost, null, null);
    }

    public Map<P, Tuple<P, List<E>>> route(Set<P> sources, Set<P> targets, Cost<E> cost, Cost<E> bound, Double max) {
        return msmt(sources, targets, cost, bound, max);
    }

    private List<E> ssst(P source, P target, Cost<E> cost, Cost<E> bound, Double max) {
        return ssmt(source, new HashSet<>(Arrays.asList(target)), cost, bound, max).get(target);
    }

    private Map<P, List<E>> ssmt(P source, Set<P> targets, Cost<E> cost, Cost<E> bound, Double max) {
        Map<P, Tuple<P, List<E>>> map =
                msmt(new HashSet<>(Arrays.asList(source)), targets, cost, bound, max);
        Map<P, List<E>> result = new HashMap<>();
        for (Map.Entry<P, Tuple<P, List<E>>> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().two());
        }
        return result;
    }

    private Map<P, Tuple<P, List<E>>> msmt(final Set<P> sources, final Set<P> targets, Cost<E> cost, Cost<E> bound, Double max) {

        /*
         * Route mark representation.
         */
        class Mark extends Quadruple<E, E, Double, Double> implements Comparable<Mark> {
            private static final long serialVersionUID = 1L;

            /**
             * Constructor of an entry.
             *
             * @param one {@link AbstractLink} defining the route mark.
             * @param two Predecessor {@link AbstractLink}.
             * @param three Cost value to this route mark.
             * @param four Bounding cost value to this route mark.
             */
            public Mark(E one, E two, Double three, Double four) {
                super(one, two, three, four);
            }

            @Override
            public int compareTo(Mark other) {
                return (this.three() < other.three()) ? -1 : (this.three() > other.three()) ? 1 : 0;
            }
        }

        /*
         * Initialize map of edges to target points.
         */
        Map<E, Set<P>> targetEdges = new HashMap<>();
        for (P target : targets) {
            if (!targetEdges.containsKey(target.edge())) {
                targetEdges.put(target.edge(), new HashSet<>(Arrays.asList(target)));
            } else {
                targetEdges.get(target.edge()).add(target);
            }
        }

        /*
         * Setup data structures
         */
        PriorityQueue<Mark> priorities = new PriorityQueue<>();
        Map<E, Mark> entries = new HashMap<>();
        Map<P, Mark> finishs = new HashMap<>();
        Map<Mark, P> reaches = new HashMap<>();
        Map<Mark, P> starts = new HashMap<>();

        /*
         * Initialize map of edges with start points
         */
        for (P source : sources) { // initialize sources as start edges
            double startcost = cost.cost(source.edge(), 1 - source.fraction());
            double startbound =
                    bound != null ? bound.cost(source.edge(), 1 - source.fraction()) : 0.0;

            if (targetEdges.containsKey(source.edge())) { // start edge reaches target edge
                for (P target : targetEdges.get(source.edge())) {
                    if (target.fraction() < source.fraction()) {
                        continue;
                    }
                    double reachcost = startcost - cost.cost(source.edge(), 1 - target.fraction());
                    double reachbound = bound != null
                            ? startcost - bound.cost(source.edge(), 1 - target.fraction())
                            : 0.0;

                    Mark reach = new Mark(source.edge(), null, reachcost, reachbound);
                    reaches.put(reach, target);
                    starts.put(reach, source);
                    priorities.add(reach);
                }
            }

            Mark start = entries.get(source.edge());
            if (start == null) {
                start = new Mark(source.edge(), null, startcost, startbound);
                entries.put(source.edge(), start);
                starts.put(start, source);
                priorities.add(start);
            } else if (startcost < start.three()) {
                start = new Mark(source.edge(), null, startcost, startbound);
                entries.put(source.edge(), start);
                starts.put(start, source);
                priorities.remove(start);
                priorities.add(start);
            }
        }

        /*
         * Dijkstra algorithm.
         */
        while (priorities.size() > 0) {
            Mark current = priorities.poll();

            if (targetEdges.isEmpty()) {
                break;
            }

            if (max != null && current.four() > max) {
                break;
            }

            /*
             * Finish target if reached.
             */
            if (reaches.containsKey(current)) {
                P target = reaches.get(current);

                if (finishs.containsKey(target)) {
                    continue;
                } else {
                    finishs.put(target, current);

                    Set<P> edges = targetEdges.get(current.one());
                    edges.remove(target);

                    if (edges.isEmpty()) {
                        targetEdges.remove(current.one());
                    }
                    continue;
                }
            }

            Iterator<E> successors = current.one().successors();

            while (successors.hasNext()) {
                E successor = successors.next();

                double succcost = current.three() + cost.cost(successor);
                double succbound = bound != null ? current.four() + bound.cost(successor) : 0.0;

                if (targetEdges.containsKey(successor)) { // reach target edge
                    for (P target : targetEdges.get(successor)) {
                        double reachcost = succcost - cost.cost(successor, 1 - target.fraction());
                        double reachbound = bound != null
                                ? succbound - bound.cost(successor, 1 - target.fraction())
                                : 0.0;

                        Mark reach = new Mark(successor, current.one(), reachcost, reachbound);
                        reaches.put(reach, target);
                        priorities.add(reach);
                    }
                }

                if (!entries.containsKey(successor)) {
                    Mark mark = new Mark(successor, current.one(), succcost, succbound);

                    entries.put(successor, mark);
                    priorities.add(mark);
                }
            }
        }

        Map<P, Tuple<P, List<E>>> paths = new HashMap<>();

        for (P target : targets) {
            if (!finishs.containsKey(target)) {
                paths.put(target, null);
            } else {
                LinkedList<E> path = new LinkedList<>();
                Mark iterator = finishs.get(target);
                Mark start = null;
                while (iterator != null) {
                    path.addFirst(iterator.one());
                    start = iterator;
                    iterator = iterator.two() != null ? entries.get(iterator.two()) : null;
                }
                paths.put(target, new Tuple<P, List<E>>(starts.get(start), path));
            }
        }

        entries.clear();
        finishs.clear();
        reaches.clear();
        priorities.clear();

        return paths;
    }



//    public void calcAllShortestPathWithPredecessor(RoadMap map){
//        HashMap<Long, HashMap<Long, PathTableEntry>> pathTable = new HashMap<>();
//        HashMap<Road, DijkstraQueueEntry> queueEntry = new HashMap<>();
//        HashMap<Long, Road> edges = map.getEdges();
//        HashMap<Long, Road> nodes = new HashMap<>();
//        for (Road source : edges.values()){
//            nodes.put(source.source(), source);
//        }
//
//        for(Long i : nodes.keySet()){
//            HashMap<Long, PathTableEntry> t = new HashMap<>();
//            for(Long j : nodes.keySet()){
//                t.put(j, null);
//            }
//            pathTable.put(i, t);
//        }
//
//        for (Map.Entry<Long, Road> i : nodes.entrySet()) {
//            queueEntry.put(i.getValue(), new DijkstraQueueEntry(i.getKey(), i.getValue()));
//        }
//
//        for (Long source : nodes.keySet()) {
//            for(DijkstraQueueEntry entry : queueEntry.values()){
//                entry.cost = Double.MAX_VALUE;
//                entry.inQueue = true;
//            }
//            DijkstraQueueEntry sourceEntry = queueEntry.get(source);
//            sourceEntry.cost = 0.0;
//            pathTable.get(source).put(source, new PathTableEntry(0.0, source));
//
//            PriorityQueue<DijkstraQueueEntry> queue = new PriorityQueue<>(queueEntry.values());
//
//            while(!queue.isEmpty()){
//                DijkstraQueueEntry entry = queue.poll();
//                entry.inQueue = false;
//
//                Iterator<Road> roads =  entry.edge.nextEdges();
//                while (roads.hasNext()){
//                    Road successor = roads.next();
//                    DijkstraQueueEntry v = queueEntry.get(successor);
//                    if(!v.inQueue) {
//                        continue;
//                    }
//                    double cost = entry.cost + successor.length();
//                    if(v.cost > cost){
//                        queue.remove(v);
//                        v.cost = cost;
//                        pathTable.get(source).put(v.edge.target(), new PathTableEntry(v.cost, entry.edge.source()));
//                        queue.add(v);
//                    }
//                }
//            }
//        }
//    }

}
