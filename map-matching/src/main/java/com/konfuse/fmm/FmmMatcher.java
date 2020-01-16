package com.konfuse.fmm;

import com.konfuse.geometry.Point;
import com.konfuse.hmm.HmmProbabilities;
import com.konfuse.hmm.TimeStep;
import com.konfuse.markov.SequenceState;
import com.konfuse.markov.ViterbiAlgorithm;
import com.konfuse.road.*;
import com.konfuse.spatial.Geography;
import com.konfuse.topology.Dijkstra;

import java.util.*;

/**
 * @Auther todd
 * @Date 2020/1/8
 */
public class FmmMatcher {
    public UBODT ubodt;
    public final Geography spatial = new Geography();
    public final HmmProbabilities hmmProbabilities = new HmmProbabilities();
    public final Dijkstra<Road, RoadPoint> dijkstra = new Dijkstra<>();
    public final DistanceCost cost = new DistanceCost();
    public final static double DISTANCE_NOT_FOUND = 5000.0;
    public final double penaltyFactor;

    public FmmMatcher(long multiplier, int buckets, double penaltyFactor){
        ubodt = new UBODT(multiplier, buckets);
        this.penaltyFactor = penaltyFactor;
    }

    public void constructUBODT(RoadMap map, double max){
        List<Record> records = calcAllShortestPathWithinThreshold(map, max);

        System.out.println("records size: " + records.size());
        for (Record record : records) {
            ubodt.insert(record);
        }
    }

    public List<RoadPoint> match(List<GPSPoint> gpsPoints, RoadMap map, double radius) {
        ViterbiAlgorithm<RoadPoint, GPSPoint, Path<Road>> viterbi = new ViterbiAlgorithm<>();
        TimeStep<RoadPoint, GPSPoint, Path<Road>> prevTimeStep = null;

        for (GPSPoint gpsPoint : gpsPoints) {
            final Collection<RoadPoint> candidates = map.spatial().radiusMatch(gpsPoint, radius);
            final TimeStep<RoadPoint, GPSPoint, Path<Road>> timeStep = new TimeStep<>(gpsPoint, candidates);
            computeEmissionProbabilities(timeStep);
            if (prevTimeStep == null) {
                viterbi.startWithInitialObservation(timeStep.observation, timeStep.candidates, timeStep.emissionLogProbabilities);
            } else {
                computeTransitionProbabilities(prevTimeStep, timeStep);
                viterbi.nextStep(timeStep.observation, timeStep.candidates,
                        timeStep.emissionLogProbabilities, timeStep.transitionLogProbabilities,
                        timeStep.roadPaths);
            }
            prevTimeStep = timeStep;
        }

        List<SequenceState<RoadPoint, GPSPoint, Path<Road>>> sequenceStates = viterbi.computeMostLikelySequence();
        List<RoadPoint> matchedPoints = new LinkedList<>();
        for (SequenceState<RoadPoint, GPSPoint, Path<Road>> sequenceState : sequenceStates) {
            matchedPoints.add(sequenceState.getState());
        }
        return matchedPoints;
    }

    public void computeEmissionProbabilities(TimeStep<RoadPoint, GPSPoint, Path<Road>> timeStep) {
        for (RoadPoint candidate : timeStep.candidates) {
            double distance = spatial.distance(new Point(timeStep.observation.getPosition().getX(), timeStep.observation.getPosition().getY()), candidate.point());
            timeStep.addEmissionLogProbability(candidate, hmmProbabilities.emissionProbability(distance));
        }
    }

    public void computeTransitionProbabilities(TimeStep<RoadPoint, GPSPoint, Path<Road>> prevTimeStep, TimeStep<RoadPoint, GPSPoint, Path<Road>> timeStep) {
        Point preObservationPosition = new Point(prevTimeStep.observation.getPosition().getX(), prevTimeStep.observation.getPosition().getY());
        Point thisObservationPosition = new Point(timeStep.observation.getPosition().getX(), timeStep.observation.getPosition().getY());
        final double linearDistance = spatial.distance(preObservationPosition, thisObservationPosition);
        final double timeDiff = (timeStep.observation.getTime() - prevTimeStep.observation.getTime());

        for (RoadPoint from : prevTimeStep.candidates) {
            for (RoadPoint to : timeStep.candidates) {
                double spdistance = getShortestDistanceM3Penalized(from, to);
                LinkedList<Road> roadList = new LinkedList<>();
                timeStep.addRoadPath(from, to, new Path<>(from, to, roadList));

                final double transitionLogProbability = calcFMMTransitionProbability(spdistance, linearDistance);
                timeStep.addTransitionLogProbability(from, to, transitionLogProbability);
            }
        }
    }

    public double getShortestDistanceM1(RoadPoint source, RoadPoint target) {
        double offsetOfSource = cost.cost(source.edge(), source.fraction());
        double offsetOfTarget = cost.cost(target.edge(), target.fraction());
        double spDist = 0.0;

        if(source.edge().id() == target.edge().id() && source.fraction() <=  target.fraction()){
            spDist += offsetOfTarget - offsetOfSource;
        }else if(source.edge().id() == target.edge().id()){
            spDist += 2 * source.edge().length() + offsetOfSource - offsetOfTarget;
        }else{
            Record r = ubodt.lookUp(source.edge().target(), target.edge().source());
            if(r == null){
                List<Road> path = dijkstra.route(source, target, cost);
                if(path == null){
                    return DISTANCE_NOT_FOUND;
                }
                for(int i = 1; i < path.size() - 1; i++){
                    spDist += path.get(i).length();
                }
                spDist += source.edge().length() + offsetOfSource - offsetOfTarget;
            }else{
                spDist = source.edge().length() + offsetOfSource - offsetOfTarget + r.cost;
            }
        }
        return spDist;

    }

    public double getShortestDistanceM3(RoadPoint source, RoadPoint target) {
        double offsetOfSource = cost.cost(source.edge(), source.fraction());
        double offsetOfTarget = cost.cost(target.edge(), target.fraction());
        double spDist = 0.0;
        if(source.edge().id() == target.edge().id() && source.fraction() <=  target.fraction()){
            spDist += offsetOfTarget - offsetOfSource;
        }else if(source.edge().id() == target.edge().id()){
            spDist += 2 * source.edge().length() + offsetOfSource - offsetOfTarget;
        }else{
            Record r = ubodt.lookUp(source.edge().target(), target.edge().source());
            spDist = r == null ? DISTANCE_NOT_FOUND : source.edge().length() + offsetOfSource - offsetOfTarget + r.cost;
        }
        return spDist;
    }

    public double getShortestDistanceM3Penalized(RoadPoint source, RoadPoint target) {
        double offsetOfSource = cost.cost(source.edge(), source.fraction());
        double offsetOfTarget = cost.cost(target.edge(), target.fraction());
        double spDist = 0.0;
        if(source.edge().id() == target.edge().id() && source.fraction() <=  target.fraction()){
            spDist += offsetOfTarget - offsetOfSource;
        }else if(source.edge().id() == target.edge().id()){
            spDist += 2 * source.edge().length() + offsetOfSource - offsetOfTarget;
        }else{
            Record r = ubodt.lookUp(source.edge().target(), target.edge().source());
            if(r == null) {
                return DISTANCE_NOT_FOUND;
            }
            spDist = source.edge().length() + offsetOfSource - offsetOfTarget + r.cost;
            if(r.prev_n == target.edge().target()){
                spDist += penaltyFactor * target.edge().length();
            }
            if(r.first_n == source.edge().source()){
                spDist += penaltyFactor * source.edge().length();
            }
        }
        return spDist;
    }

    public double calcFMMTransitionProbability(double shorestPathDist, double linearDist){
        double transitionProbability = 1.0;
        if (linearDist < 0.000001) {
            transitionProbability = shorestPathDist > 0.000001 ? 0:1.0;
        } else {
            transitionProbability = linearDist > shorestPathDist ? shorestPathDist/linearDist : linearDist/shorestPathDist;
        }
        return transitionProbability;
    }

    public List<Road> constructCompletePath(List<RoadPoint> o_path, RoadMap map){
        List<Long> c_path = ubodt.constructCompletePath(o_path);
        if(c_path != null){
            System.out.println("c_path: " + c_path.size());
        }else{
            System.out.println("c_path: null");
        }
        List<Road> paths = new LinkedList<>();
        if(c_path == null){
            int N = o_path.size();

            for(int i = 0; i < N - 1; i++){
                RoadPoint source = o_path.get(i);
                RoadPoint target = o_path.get(i + 1);
                List<Road> path = dijkstra.route(source, target, cost);
                if(path != null && path.size() > 1) {
                    if (i == 0) {
                        int index = path.size() - 2;
                        paths.addAll(path.subList(0, index));
                    } else {
                        paths.addAll(path);
                    }
                }
            }
        }else{
            for (Long edge : c_path) {
                paths.add(map.getEdges().get(edge));
            }
        }
        return paths;
    }

    public List<Road> constructCompletePathOptimized(List<RoadPoint> o_path, RoadMap map){
        List<Road> paths = new LinkedList<>();
        int N = o_path.size();
        paths.add(o_path.get(0).edge());
        for(int i = 0; i < N - 1; i++) {
            RoadPoint source = o_path.get(i);
            RoadPoint target = o_path.get(i + 1);
            if((source.edge().id() != target.edge().id()) || (source.fraction() > target.fraction())){
                List<Long> segs = ubodt.lookShortestPath(source.edge().target(), target.edge().source());
                if(segs == null){
                    List<Road> path = dijkstra.route(source, target, cost);
                    if(path != null && path.size() > 1) {
                        System.out.println("dijkstra!");
                        if (i == 0) {
                            int index = path.size() - 2;
                            paths.addAll(path.subList(0, index));
                        } else {
                            paths.addAll(path);
                        }
                    }
                }else if(segs.isEmpty() && source.edge().target() != target.edge().source()) {
                    System.out.println("UDOBT!");
                    paths.add(map.getEdges().get(target.edge().id()));
                    continue;
                }else{
                    System.out.println("UDOBT!");
                    for (Long seg : segs) {
                        paths.add(map.getEdges().get(seg));
                    }
                    paths.add(map.getEdges().get(target.edge().id()));
                }
            }
        }
        return paths;
    }


    public List<GPSPoint> getCompletePathGPS(List<Road> c_path){
        List<GPSPoint> completePathCoordinate = new LinkedList<>();
        int count = 0;
        for(Road road : c_path){
            List<Point> points = road.getPoints();
            if(count == 0){
                points = points.subList(0, points.size() - 2);
                for (Point point : points) {
                    completePathCoordinate.add(new GPSPoint(count++, point.getX(), point.getY()));
                }
            }
            else{
                for (Point point : points) {
                    completePathCoordinate.add(new GPSPoint(count++, point.getX(), point.getY()));
                }
            }
        }

        return completePathCoordinate;
    }

    public List<Record> calcAllShortestPathWithinThreshold(RoadMap map, double max) {
        List<Record> records = new LinkedList<>();
        HashMap<Long, HashMap<Long, PathTableEntry>> pathTable = new HashMap<>();
        HashMap<Long, DijkstraQueueEntry> queueEntry = new HashMap<>();
        HashMap<Long, Road> edges = map.getEdges();
        HashMap<Long, Road> nodes = new HashMap<>();

        for (Road road : edges.values()) {
            if (!nodes.containsKey(road.source())) {
                Iterator<Road> itr = road.neighbors();
                nodes.put(road.source(), itr.next());
            }

            if (!nodes.containsKey(road.target())) {
                Iterator<Road> itr = road.successors();
                nodes.put(road.target(), itr.next());
            }
        }

        for (Long nodeId : nodes.keySet()) {
            HashMap<Long, PathTableEntry> t = new HashMap<>();
            for (Long j : nodes.keySet()) {
                t.put(j, null);
            }
            pathTable.put(nodeId, t);
        }

        for (Long longEntry : nodes.keySet()) {
            queueEntry.put(longEntry, new DijkstraQueueEntry(longEntry));
        }

        for (long source : nodes.keySet()) {
            for(DijkstraQueueEntry entry : queueEntry.values()){
                entry.cost = Double.MAX_VALUE;
                entry.inQueue = true;
            }
            DijkstraQueueEntry sourceEntry = queueEntry.get(source);
            sourceEntry.cost = 0.0;
            pathTable.get(source).put(source, new PathTableEntry(0.0, source, -1));

            PriorityQueue<DijkstraQueueEntry> queue = new PriorityQueue<>(queueEntry.values());

            while(!queue.isEmpty()){
                DijkstraQueueEntry entry = queue.poll();
                entry.inQueue = false;

                if(entry.cost > max){
                    continue;
                }

                if(entry.nodeId != source){
                    PathTableEntry pathRecord = pathTable.get(source).get(entry.nodeId);
                    long prev_n = pathRecord.predecessor;
                    long current = entry.nodeId;
                    long first_n = current;
                    long pred = current;
                    while(current != source){
                        first_n = current;
                        pathRecord = pathTable.get(source).get(pred);
                        pred = pathRecord.predecessor;
                        current = pred;
                    }
                    long next_e = pathRecord.edgeId;
                    records.add(new Record(source, entry.nodeId, first_n, prev_n, next_e, entry.cost));
                }

                Iterator<Road> roads = null;
                if(nodes.get(entry.nodeId) == null){
                    continue;
                }else{
                    roads = nodes.get(entry.nodeId).neighbors();
                }

                while (roads.hasNext()){
                    Road next = roads.next();
                    DijkstraQueueEntry v = queueEntry.get(next.target());
                    if(!v.inQueue) {
                        continue;
                    }
                    double cost = entry.cost + next.length();
                    if(v.cost > cost){
                        queue.remove(v);
                        v.cost = cost;
                        pathTable.get(source).put(next.target(), new PathTableEntry(v.cost, next.source(), next.id()));
                        queue.add(v);
                    }
                }
            }
        }

        return records;
    }


    private static class DijkstraQueueEntry implements Comparable<DijkstraQueueEntry> {
        long nodeId;
        double cost;
        boolean inQueue ;

        DijkstraQueueEntry(long nodeId) {
            this.nodeId = nodeId;
            this.cost = Double.MAX_VALUE;
            this.inQueue = true;
        }

        @Override
        public int compareTo(DijkstraQueueEntry j) {
            if (this.cost < j.cost) {
                return -1;
            } else if (this.cost > j.cost) {
                return 1;
            } else if (this.nodeId < j.nodeId) {
                return -1;
            } else if (this.nodeId > j.nodeId) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private static class PathTableEntry {
        final double length;
        final long predecessor;
        final long edgeId;

        PathTableEntry(double length, long predecessor, long edgeId) {
            this.length = length;
            this.predecessor = predecessor;
            this.edgeId = edgeId;
        }
    }
}
