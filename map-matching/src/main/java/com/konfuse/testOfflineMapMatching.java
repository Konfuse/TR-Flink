package com.konfuse;

import com.esri.core.geometry.Point;
import com.konfuse.OfflineMapMatching.OfflineMatcher;
import com.konfuse.OfflineMapMatching.TimeStep;
import com.konfuse.markov.SequenceState;
import com.konfuse.markov.ViterbiAlgorithm;
import com.konfuse.road.*;
import com.konfuse.spatial.Geography;
import com.konfuse.tools.GenerateTestGPSPoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2019/12/31
 */
public class testOfflineMapMatching {
    public static void main(String[] args) throws Exception{
        RoadMap map = RoadMap.Load(new RoadReader());
        map.construct();
        HashMap<Long, Road> roads = map.getRoads();
        Geography spatial = new Geography();
        OfflineMatcher offlineMatcher = new OfflineMatcher();
//        for(Long id : roads.keySet()){
//            Road road = roads.get(id);
//            LinkedList<Point> points = (LinkedList)road.base().getPoints();
//            System.out.println(id + ": " + road.source() + " " + road.target());
//            System.out.println(id + ": " + points.getFirst() + " " + points.getLast());
//            Iterator<Road> nextRoad = road.successors();
//            System.out.println("************successor************");
//            while (nextRoad.hasNext()) {
//                Road successor = nextRoad.next();
//                System.out.println("->" + successor.id());
//            }
//            System.out.println("*********************************");
//        }

        GenerateTestGPSPoint test = new GenerateTestGPSPoint();
        LinkedList<GPSPoint> testRoads = (LinkedList)test.generateTestGPSPoint(map);
        LinkedList<GPSPoint> testGPSPoint = (LinkedList)test.generateTestCase(testRoads);

        ViterbiAlgorithm<RoadPoint, GPSPoint, Path<Road>> viterbi = new ViterbiAlgorithm<>();
        TimeStep<RoadPoint, GPSPoint, Path<Road>> prevTimeStep = null;
        System.out.println("**********************");

        for (GPSPoint gps : testGPSPoint) {
            final Collection<RoadPoint> candidates = offlineMatcher.computeCandidates(gps, 20, map);
            final TimeStep<RoadPoint, GPSPoint, Path<Road>> timeStep = new TimeStep<>(gps, candidates);
            offlineMatcher.computeEmissionProbabilities(timeStep);
            if (prevTimeStep == null) {
                viterbi.startWithInitialObservation(timeStep.observation, timeStep.candidates, timeStep.emissionLogProbabilities);
            } else {
                offlineMatcher.computeTransitionProbabilities(prevTimeStep, timeStep);
                viterbi.nextStep(timeStep.observation, timeStep.candidates,
                        timeStep.emissionLogProbabilities, timeStep.transitionLogProbabilities,
                        timeStep.roadPaths);
            }
            prevTimeStep = timeStep;
        }

        System.out.println("**********************");
        List<SequenceState<RoadPoint, GPSPoint, Path<Road>>> roadPositions = viterbi.computeMostLikelySequence();


        System.out.println("************road***********");
        for(GPSPoint point1 : testRoads){
            double x1 = point1.getPosition().getX();
            double y1 = point1.getPosition().getY();
            System.out.println(x1 + ";" + y1);
        }
        System.out.println("***************************");
        System.out.println("************test***********");
        for(GPSPoint point2 : testGPSPoint){
            double x2 = point2.getPosition().getX();
            double y2 = point2.getPosition().getY();
            System.out.println(x2 + ";" + y2);
        }
        System.out.println("***************************");
        System.out.println("************match***********");
        for(SequenceState<RoadPoint, GPSPoint, Path<Road>> roadPosition : roadPositions){
            Point e = spatial.interpolate(roadPosition.state.edge().geometry(), spatial.length(roadPosition.state.edge().geometry()), roadPosition.state.fraction());
            System.out.println(e.getX() + ";" + e.getY());
        }
        System.out.println("***************************");
    }

}
