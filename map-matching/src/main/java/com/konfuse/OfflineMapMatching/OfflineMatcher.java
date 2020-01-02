package com.konfuse.OfflineMapMatching;

import com.esri.core.geometry.Point;
import com.konfuse.road.*;
import com.konfuse.spatial.Geography;
import com.konfuse.topology.Cost;
import com.konfuse.topology.Dijkstra;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * @Auther todd
 * @Date 2019/12/31
 */
public class OfflineMatcher {
    public final Geography spatial = new Geography();
    public final HmmProbabilities hmmProbabilities = new HmmProbabilities();
    public final Dijkstra<Road, RoadPoint> dijkstra = new Dijkstra<>();
    public final DistanceCost cost = new DistanceCost();

    public Set<RoadPoint> computeCandidates(GPSPoint gps, double radius, RoadMap map) {
        return map.spatial().radiusMatch(gps, radius);
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
