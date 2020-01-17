package com.konfuse.fmm;

import com.konfuse.geometry.Point;
import com.konfuse.markov.TimeStep;
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
    public final FmmProbabilities fmmProbabilities = new FmmProbabilities();
    public final Dijkstra<Road, RoadPoint> dijkstra = new Dijkstra<>();
    public final DistanceCost cost = new DistanceCost();
    public final static double DISTANCE_NOT_FOUND = 5000.0;
    public final double penaltyFactor;

    public FmmMatcher(double penaltyFactor){
        this.penaltyFactor = penaltyFactor;
    }

    public void constructUBODT(RoadMap map, double max){
        HashMap<Long, Road> nodes = map.getNodes();

        List<Record> records = calcAllShortestPathWithinThreshold(nodes, max);
        int multiplier = nodes.size();

        ubodt = UBODT.construct(records, multiplier);
        records.clear();
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
            timeStep.addEmissionLogProbability(candidate, fmmProbabilities.emissionLogProbability(distance));
        }
    }

    public void computeTransitionProbabilities(TimeStep<RoadPoint, GPSPoint, Path<Road>> prevTimeStep, TimeStep<RoadPoint, GPSPoint, Path<Road>> timeStep) {
        Point preObservationPosition = new Point(prevTimeStep.observation.getPosition().getX(), prevTimeStep.observation.getPosition().getY());
        Point thisObservationPosition = new Point(timeStep.observation.getPosition().getX(), timeStep.observation.getPosition().getY());

        final double linearDistance = spatial.distance(preObservationPosition, thisObservationPosition);

        for (RoadPoint from : prevTimeStep.candidates) {
            for (RoadPoint to : timeStep.candidates) {
                double spDistance = getShortestDistanceM3Penalized(from, to);
                LinkedList<Road> roadList = new LinkedList<>();
                timeStep.addRoadPath(from, to, new Path<>(from, to, roadList));

                final double transitionLogProbability = fmmProbabilities.transitionLogProbability(spDistance, linearDistance);
                timeStep.addTransitionLogProbability(from, to, transitionLogProbability);
            }
        }
    }

    public double getShortestDistanceM1(RoadPoint a, RoadPoint b) {
        double offsetOfSource = cost.cost(a.edge(), a.fraction());
        double offsetOfTarget = cost.cost(b.edge(), b.fraction());

        double spDist = 0.0;
        if(a.edge().id() == b.edge().id() && a.fraction() <=  b.fraction()) {
            spDist += offsetOfTarget - offsetOfSource;
        } else if(a.edge().target() == b.edge().source()) {
            spDist += a.edge().length() + offsetOfSource - offsetOfTarget;
        } else {
            Record r = ubodt.lookUp(a.edge().target(), b.edge().source());
            if(r == null) {
                List<Road> path = dijkstra.route(a, b, cost);
                if(path == null){
                    return DISTANCE_NOT_FOUND;
                }
                for(int i = 1; i < path.size() - 1; i++) {
                    spDist += path.get(i).length();
                }
                spDist += a.edge().length() + offsetOfSource - offsetOfTarget;
            } else {
                spDist = a.edge().length() + offsetOfSource - offsetOfTarget + r.cost;
            }
        }
        return spDist;
    }

    public double getShortestDistanceM3(RoadPoint a, RoadPoint b) {
        double offsetOfSource = cost.cost(a.edge(), a.fraction());
        double offsetOfTarget = cost.cost(b.edge(), b.fraction());

        double spDist = 0.0;
        if(a.edge().id() == b.edge().id() && a.fraction() <=  b.fraction()) {
            spDist += offsetOfTarget - offsetOfSource;
        } else if(a.edge().target() == b.edge().source()) {
            spDist += a.edge().length() + offsetOfSource - offsetOfTarget;
        } else {
            Record r = ubodt.lookUp(a.edge().target(), b.edge().source());
            spDist = r == null ? DISTANCE_NOT_FOUND : a.edge().length() + offsetOfSource - offsetOfTarget + r.cost;
        }
        return spDist;
    }

    public double getShortestDistanceM3Penalized(RoadPoint a, RoadPoint b) {
        double offsetOfSource = cost.cost(a.edge(), a.fraction());
        double offsetOfTarget = cost.cost(b.edge(), b.fraction());

        double spDist = 0.0;
        if(a.edge().id() == b.edge().id() && a.fraction() <=  b.fraction()) {
            spDist += offsetOfTarget - offsetOfSource;
        } else if(a.edge().target() == b.edge().source()) {
            spDist += a.edge().length() + offsetOfSource - offsetOfTarget;
        } else {
            Record r = ubodt.lookUp(a.edge().target(), b.edge().source());
            if(r == null) {
                return DISTANCE_NOT_FOUND;
            }
            spDist = a.edge().length() + offsetOfSource - offsetOfTarget + r.cost;
            if(r.prev_n == b.edge().target()) {
                spDist += penaltyFactor * b.edge().length();
            }
            if(r.first_n == a.edge().source()){
                spDist += penaltyFactor * a.edge().length();
            }
        }
        return spDist;
    }

    public List<Road> constructCompletePath(List<RoadPoint> o_path, RoadMap map){
        List<Road> paths = new ArrayList<>();
        if(o_path.isEmpty()) {
            return null;
        }

        int N = o_path.size();
        paths.add(o_path.get(0).edge());

        for(int i = 0; i < N - 1; i++) {
            RoadPoint a = o_path.get(i);
            RoadPoint b = o_path.get(i + 1);
            if((a.edge().id() != b.edge().id()) || (a.fraction() > b.fraction())) {
                List<Long> shortestPathBetweenAB = ubodt.lookShortestPath(a.edge().target(), b.edge().source());

                if(shortestPathBetweenAB.isEmpty() && a.edge().target() != b.edge().source()){
                    return null;
                }

                for (Long seg : shortestPathBetweenAB) {
                    paths.add(map.getEdges().get(seg));
                }
                paths.add(map.getEdges().get(b.edge().id()));
            }
        }
        return paths;
    }

    public List<Road> constructCompletePathOptimized(List<RoadPoint> o_path, RoadMap map) {
        List<Road> paths = new ArrayList<>();

        int N = o_path.size();
        paths.add(o_path.get(0).edge());

        for(int i = 0; i < N - 1; i++) {
            RoadPoint a = o_path.get(i);
            RoadPoint b = o_path.get(i + 1);

            if((a.edge().id() != b.edge().id()) || (a.fraction() > b.fraction())) {
                List<Long> shortestPathBetweenAB = ubodt.lookShortestPath(a.edge().target(), b.edge().source());

                if (!shortestPathBetweenAB.isEmpty()) {
                    System.out.println("UDOBT!");
                    for (Long seg : shortestPathBetweenAB) {
                        paths.add(map.getEdges().get(seg));
                    }
                    paths.add(map.getEdges().get(b.edge().id()));
                } else if (a.edge().target() == b.edge().source()) {
                    System.out.println("UDOBT!");
                    paths.add(map.getEdges().get(b.edge().id()));
                } else {
                    List<Road> path = dijkstra.route(a, b, cost);
                    if (path == null) {
                        return null;
                    }
                    paths.addAll(path.subList(1, path.size()));
                }
            }
        }
        return paths;
    }


    public List<GPSPoint> getCompletePathGPS(List<Road> c_path) {
        List<GPSPoint> completePathCoordinate = new ArrayList<>();

        int count = 0;
        boolean flag = false;

        for (Road road : c_path) {
            List<Point> pointList = road.getPoints();
            if (!flag) {
                for (Point point : pointList) {
                    completePathCoordinate.add(new GPSPoint(count++, point.getX(), point.getY()));
                }
                flag = true;
            } else {
                for (int i = 1; i < pointList.size(); i++) {
                    completePathCoordinate.add(new GPSPoint(count++, pointList.get(i).getX(), pointList.get(i).getY()));
                }
            }
        }

        return completePathCoordinate;
    }

    public List<Record> calcAllShortestPathWithinThreshold(HashMap<Long, Road> nodes, double max) {
        class DijkstraQueueEntry implements Comparable<DijkstraQueueEntry> {
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

        class PathTableEntry {
            final double length;
            final long predecessor;
            final long edgeId;

            PathTableEntry(double length, long predecessor, long edgeId) {
                this.length = length;
                this.predecessor = predecessor;
                this.edgeId = edgeId;
            }
        }

        List<Record> records = new LinkedList<>();
        HashMap<Long, DijkstraQueueEntry> queueEntry = new HashMap<>();


        for (Long longEntry : nodes.keySet()) {
            queueEntry.put(longEntry, new DijkstraQueueEntry(longEntry));
        }

        for (long source : nodes.keySet()) {
            HashMap<Long, PathTableEntry> pathRecords = new HashMap<>();

            for (DijkstraQueueEntry entry : queueEntry.values()) {
                entry.cost = Double.MAX_VALUE;
                entry.inQueue = true;
            }

            DijkstraQueueEntry sourceEntry = queueEntry.get(source);
            sourceEntry.cost = 0.0;
            pathRecords.put(source, new PathTableEntry(0.0, source, -1));

            PriorityQueue<DijkstraQueueEntry> queue = new PriorityQueue<>(queueEntry.values());

            while(!queue.isEmpty()) {
                DijkstraQueueEntry entry = queue.poll();
                entry.inQueue = false;

                if(entry.cost > max) {
                    continue;
                }

                if(entry.nodeId != source) {
                    PathTableEntry pathRecord = pathRecords.get(entry.nodeId);

                    long prev_n = pathRecord.predecessor;
                    long current = entry.nodeId;
                    long first_n = current;
                    long pred = current;

                    while(current != source) {
                        first_n = current;
                        pathRecord = pathRecords.get(pred);
                        pred = pathRecord.predecessor;
                        current = pred;
                    }

                    long next_e = pathRecord.edgeId;
                    records.add(new Record(source, entry.nodeId, first_n, prev_n, next_e, entry.cost));
                }

                Iterator<Road> roads = null;
                if(nodes.get(entry.nodeId) == null) {
                    continue;
                } else {
                    roads = nodes.get(entry.nodeId).neighbors();
                }

                while (roads.hasNext()) {
                    Road next = roads.next();
                    DijkstraQueueEntry v = queueEntry.get(next.target());

                    if(!v.inQueue) {
                        continue;
                    }

                    double cost = entry.cost + next.length();

                    if(v.cost > cost) {
                        queue.remove(v);
                        v.cost = cost;
                        pathRecords.put(next.target(), new PathTableEntry(v.cost, next.source(), next.id()));
                        queue.add(v);
                    }
                }
            }
        }
        return records;
    }
}
