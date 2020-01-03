package com.konfuse.OfflineMapMatching;

import com.esri.core.geometry.Point;
import com.konfuse.markov.SequenceState;
import com.konfuse.markov.ViterbiAlgorithm;
import com.konfuse.road.*;
import com.konfuse.spatial.Geography;
import com.konfuse.topology.Cost;
import com.konfuse.topology.Dijkstra;

import java.util.*;


/**
 * @Auther todd
 * @Date 2019/12/31
 */
public class OfflineMatcher {
    public final Geography spatial = new Geography();
    public final HmmProbabilities hmmProbabilities = new HmmProbabilities();
    public final Dijkstra<Road, RoadPoint> dijkstra = new Dijkstra<>();
    public final DistanceCost cost = new DistanceCost();

    public List<RoadPoint> match(List<GPSPoint> gpsPoints, RoadMap map, double radius) {
        ViterbiAlgorithm<RoadPoint, GPSPoint, Path<Road>> viterbi = new ViterbiAlgorithm<>();
        TimeStep<RoadPoint, GPSPoint, Path<Road>> prevTimeStep = null;

        for (GPSPoint gps : gpsPoints) {
            final Collection<RoadPoint> candidates = map.spatial().radiusMatch(gps, radius);
            final TimeStep<RoadPoint, GPSPoint, Path<Road>> timeStep = new TimeStep<>(gps, candidates);
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
            timeStep.addEmissionLogProbability(candidate, hmmProbabilities.emissionLogProbability(distance));
        }
    }

    public void computeTransitionProbabilities(TimeStep<RoadPoint, GPSPoint, Path<Road>> prevTimeStep, TimeStep<RoadPoint, GPSPoint, Path<Road>> timeStep) {
        Point preObservationPosition = new Point(prevTimeStep.observation.getPosition().getX(), prevTimeStep.observation.getPosition().getY());
        Point thisObservationPosition = new Point(timeStep.observation.getPosition().getX(), timeStep.observation.getPosition().getY());
        final double linearDistance = spatial.distance(preObservationPosition, thisObservationPosition);
        final double timeDiff = (timeStep.observation.getTime() - prevTimeStep.observation.getTime());

        for (RoadPoint from : prevTimeStep.candidates) {
            for (RoadPoint to : timeStep.candidates) {
                double routeLength = shortestDistance(from, to, cost);
                LinkedList<Road> roadList = new LinkedList<>();
                timeStep.addRoadPath(from, to, new Path<>(from, to, roadList));

                final double transitionLogProbability = hmmProbabilities.transitionLogProbability(routeLength, linearDistance, timeDiff);
                timeStep.addTransitionLogProbability(from, to, transitionLogProbability);
            }
        }
    }

    public double shortestDistance(RoadPoint source, RoadPoint target, DistanceCost cost) {
        double distanceToEndVertexOfSource = cost.cost(source.edge(), 1 -  source.fraction());
        double distanceFromStartVertexOfDestinationToTarget = cost.cost(target.edge(), target.fraction());
        if(source.edge().id() == target.edge().id()){
            if(source.fraction() < target.fraction()){
                return 2 * source.edge().length() - distanceToEndVertexOfSource +  distanceFromStartVertexOfDestinationToTarget;
            }
            else{
                return distanceFromStartVertexOfDestinationToTarget - distanceToEndVertexOfSource;
            }
        }

        List<Road> shortestPath = dijkstra.route(source, target, cost);

        if(shortestPath == null){
            return Double.MAX_VALUE;
        }

        double pathDistance = 0.0;
        for(int i = 1; i < shortestPath.size() - 1; i++){
            pathDistance += shortestPath.get(i).length();
        }
        return distanceToEndVertexOfSource + pathDistance + distanceFromStartVertexOfDestinationToTarget;
    }
}
