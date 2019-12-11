package com.konfuse;

import com.konfuse.HMM.*;
import com.konfuse.bean.Point;

import java.util.*;

import static junit.framework.TestCase.assertEquals;

/**
 * @Auther todd
 * @Date 2019/12/11
 */
public class testMarkov {
    private final HmmProbabilities hmmProbabilities = new HmmProbabilities();

    private final static Map<GPSMeasurement, Collection<RoadPosition>> candidateMap = new HashMap<>();

    private final static Map<Transition, Double> routeLengths = new HashMap<>();

    private final static GPSMeasurement gps1 = new GPSMeasurement(10, 10, seconds(0));
    private final static GPSMeasurement gps2 = new GPSMeasurement(30, 20, seconds(1));
    private final static GPSMeasurement gps3 = new GPSMeasurement(30, 40, seconds(2));
    private final static GPSMeasurement gps4 = new GPSMeasurement(10, 70, seconds(3));

    private final static RoadPosition rp11 = new RoadPosition(1, 1.0 / 5.0, 20.0, 10.0);
    private final static RoadPosition rp12 = new RoadPosition(2, 1.0 / 5.0, 60.0, 10.0);
    private final static RoadPosition rp21 = new RoadPosition(1, 2.0 / 5.0, 20.0, 20.0);
    private final static RoadPosition rp22 = new RoadPosition(2, 2.0 / 5.0, 60.0, 20.0);
    private final static RoadPosition rp31 = new RoadPosition(1, 5.0 / 6.0, 20.0, 40.0);
    private final static RoadPosition rp32 = new RoadPosition(3, 1.0 / 4.0, 30.0, 50.0);
    private final static RoadPosition rp33 = new RoadPosition(2, 5.0 / 6.0, 60.0, 40.0);
    private final static RoadPosition rp41 = new RoadPosition(4, 2.0 / 3.0, 20.0, 70.0);
    private final static RoadPosition rp42 = new RoadPosition(5, 2.0 / 3.0, 60.0, 70.0);

    public static void setUpClass() {
        candidateMap.put(gps1, Arrays.asList(rp11, rp12));
        candidateMap.put(gps2, Arrays.asList(rp21, rp22));
        candidateMap.put(gps3, Arrays.asList(rp31, rp32, rp33));
        candidateMap.put(gps4, Arrays.asList(rp41, rp42));

        addRouteLength(rp11, rp21, 10.0);
        addRouteLength(rp11, rp22, 110.0);
        addRouteLength(rp12, rp21, 110.0);
        addRouteLength(rp12, rp22, 10.0);

        addRouteLength(rp21, rp31, 20.0);
        addRouteLength(rp21, rp32, 40.0);
        addRouteLength(rp21, rp33, 80.0);
        addRouteLength(rp22, rp31, 80.0);
        addRouteLength(rp22, rp32, 60.0);
        addRouteLength(rp22, rp33, 20.0);

        addRouteLength(rp31, rp41, 30.0);
        addRouteLength(rp31, rp42, 70.0);
        addRouteLength(rp32, rp41, 30.0);
        addRouteLength(rp32, rp42, 50.0);
        addRouteLength(rp33, rp41, 70.0);
        addRouteLength(rp33, rp42, 30.0);
    }

    private static Date seconds(int seconds) {
        Calendar c = new GregorianCalendar(2014, 1, 1);
        c.add(Calendar.SECOND, seconds);
        return c.getTime();
    }

    private static void addRouteLength(RoadPosition from, RoadPosition to, double routeLength) {
        routeLengths.put(new Transition(from, to), routeLength);
    }

    /*
     * Returns the Cartesian distance between two points.
     * For real map matching applications, one would compute the great circle distance between
     * two GPS points.
     */
    private double computeDistance(Point p1, Point p2) {
        final double xDiff = p1.getX() - p2.getX();
        final double yDiff = p1.getY() - p2.getY();
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    /*
     * For real map matching applications, candidates would be computed using a radius query.
     */
    private Collection<RoadPosition> computeCandidates(GPSMeasurement gpsMeasurement) {
        System.out.println(candidateMap.get(gpsMeasurement).toString());
        return candidateMap.get(gpsMeasurement);
    }

    private void computeEmissionProbabilities(TimeStep timeStep) {
        for (RoadPosition candidate : timeStep.candidates) {
            final double distance = computeDistance(candidate.position, timeStep.observation.position);
            timeStep.addMeasurementLogProbability(candidate, hmmProbabilities.measurementLogProbability(distance));
        }
    }

    private void computeTransitionProbabilities(TimeStep prevTimeStep, TimeStep timeStep) {
        final double linearDistance = computeDistance(prevTimeStep.observation.position, timeStep.observation.position);
        final double timeDiff = (timeStep.observation.time.getTime() - prevTimeStep.observation.time.getTime()) / 1000.0;

        for (RoadPosition from : prevTimeStep.candidates) {
            for (RoadPosition to : timeStep.candidates) {

                // For real map matching applications, route lengths and road paths would be
                // computed using a router. The most efficient way is to use a single-source
                // multi-target router.
                final double routeLength = routeLengths.get(new Transition(from, to));
                timeStep.addRoadPath(from, to, new RoadPath(from, to));

                final double transitionLogProbability = hmmProbabilities.transitionLogProbability(
                        routeLength, linearDistance, timeDiff);
                timeStep.addTransitionLogProbability(from, to, transitionLogProbability);
            }
        }
    }

    public void testMapMatching() {
        final List<GPSMeasurement> gpsMeasurements = Arrays.asList(gps1, gps2, gps3, gps4);
        ViterbiAlgorithm viterbi = new ViterbiAlgorithm();
        TimeStep prevTimeStep = null;
        for (GPSMeasurement gpsMeasurement : gpsMeasurements) {
            final ArrayList<RoadPosition> candidates = new ArrayList<>(computeCandidates(gpsMeasurement));
            final TimeStep timeStep = new TimeStep(gpsMeasurement, candidates);
            computeEmissionProbabilities(timeStep);
            if (prevTimeStep == null) {
                viterbi.startWithInitialObservation(timeStep.observation, timeStep.candidates,
                        timeStep.measurementLogProbabilities);
            } else {
                computeTransitionProbabilities(prevTimeStep, timeStep);
                viterbi.nextStep(timeStep.observation, timeStep.candidates,
                        timeStep.measurementLogProbabilities, timeStep.transitionLogProbabilities,
                        timeStep.roadPaths);
            }
            prevTimeStep = timeStep;
        }

        List<SequenceState> roadPositions = viterbi.computeMostLikelySequence();
        for (SequenceState roadPosition : roadPositions){
            System.out.println(roadPosition.toString());
        }
        List<SequenceState> expected = new ArrayList<>();
        expected.add(new SequenceState(gps1, rp11, (RoadPath) null,null));
        expected.add(new SequenceState(gps2, rp21, new RoadPath(rp11, rp21), null));
        expected.add(new SequenceState(gps3, rp31, new RoadPath(rp21, rp31), null));
        expected.add(new SequenceState(gps4, rp41, new RoadPath(rp31, rp41), null));
        int i = 0;
        System.out.println("****************");
        for (SequenceState expect : expected){
            SequenceState a = roadPositions.get(i);
            System.out.println(a.toString());
            System.out.println(expect.toString());
            assertEquals(a.toString(), expect.toString());
            i++;
        }

    }

    public static void main(String[] args) throws Exception {
        setUpClass();
        new testMarkov().testMapMatching();
    }
}
