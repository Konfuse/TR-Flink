package com.konfuse;

import com.konfuse.geometry.Point;
import com.konfuse.markov.*;
import com.konfuse.roadnetwork.GpsPoint;
import com.konfuse.roadnetwork.LocationOnRoad;
import com.konfuse.roadnetwork.Path;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @Auther todd
 * @Date 2019/12/12
 */
public class testMarkov {
    private final HmmProbabilities hmmProbabilities = new HmmProbabilities();

    private final static Map<GpsPoint, Collection<LocationOnRoad>> candidateMap =
            new HashMap<>();

    private final static Map<Transition<LocationOnRoad>, Double> routeLengths = new HashMap<>();

    private final static GpsPoint gps1 = new GpsPoint(seconds(0), 10, 10);
    private final static GpsPoint gps2 = new GpsPoint(seconds(1), 30, 20);
    private final static GpsPoint gps3 = new GpsPoint(seconds(2), 30, 40);
    private final static GpsPoint gps4 = new GpsPoint(seconds(3), 10, 70);

    private final static LocationOnRoad rp11 = new LocationOnRoad(1, 1.0 / 5.0, 20.0, 10.0);
    private final static LocationOnRoad rp12 = new LocationOnRoad(2, 1.0 / 5.0, 60.0, 10.0);
    private final static LocationOnRoad rp21 = new LocationOnRoad(1, 2.0 / 5.0, 20.0, 20.0);
    private final static LocationOnRoad rp22 = new LocationOnRoad(2, 2.0 / 5.0, 60.0, 20.0);
    private final static LocationOnRoad rp31 = new LocationOnRoad(1, 5.0 / 6.0, 20.0, 40.0);
    private final static LocationOnRoad rp32 = new LocationOnRoad(3, 1.0 / 4.0, 30.0, 50.0);
    private final static LocationOnRoad rp33 = new LocationOnRoad(2, 5.0 / 6.0, 60.0, 40.0);
    private final static LocationOnRoad rp41 = new LocationOnRoad(4, 2.0 / 3.0, 20.0, 70.0);
    private final static LocationOnRoad rp42 = new LocationOnRoad(5, 2.0 / 3.0, 60.0, 70.0);

    @BeforeClass
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

    private static void addRouteLength(LocationOnRoad from, LocationOnRoad to, double routeLength) {
        routeLengths.put(new Transition<LocationOnRoad>(from, to), routeLength);
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
    private Collection<LocationOnRoad> computeCandidates(GpsPoint gpsMeasurement) {
        return candidateMap.get(gpsMeasurement);
    }

    private void computeEmissionProbabilities(
            TimeStep<LocationOnRoad, GpsPoint, Path> timeStep) {
        for (LocationOnRoad candidate : timeStep.candidates) {
            final double distance =
                    computeDistance(candidate.position, timeStep.observation.position);
            timeStep.addEmissionLogProbability(candidate,
                    hmmProbabilities.emissionLogProbability(distance));
        }
    }

    private void computeTransitionProbabilities(
            TimeStep<LocationOnRoad, GpsPoint, Path> prevTimeStep,
            TimeStep<LocationOnRoad, GpsPoint, Path> timeStep) {
        final double linearDistance = computeDistance(prevTimeStep.observation.position,
                timeStep.observation.position);
        final double timeDiff = (timeStep.observation.time.getTime() -
                prevTimeStep.observation.time.getTime()) / 1000.0;

        for (LocationOnRoad from : prevTimeStep.candidates) {
            for (LocationOnRoad to : timeStep.candidates) {

                // For real map matching applications, route lengths and road paths would be
                // computed using a router. The most efficient way is to use a single-source
                // multi-target router.
                final double routeLength = routeLengths.get(new Transition<>(from, to));
                timeStep.addRoadPath(from, to, new Path(from, to));

                final double transitionLogProbability = hmmProbabilities.transitionLogProbability(
                        routeLength, linearDistance, timeDiff);
                timeStep.addTransitionLogProbability(from, to, transitionLogProbability);
            }
        }
    }

    @Test
    public void testMapMatching() {
        final List<GpsPoint> gpsMeasurements = Arrays.asList(gps1, gps2, gps3, gps4);

        ViterbiAlgorithm<LocationOnRoad, GpsPoint, Path> viterbi =
                new ViterbiAlgorithm<>();
        TimeStep<LocationOnRoad, GpsPoint, Path> prevTimeStep = null;
        for (GpsPoint gpsMeasurement : gpsMeasurements) {
            final Collection<LocationOnRoad> candidates = computeCandidates(gpsMeasurement);
            final TimeStep<LocationOnRoad, GpsPoint, Path> timeStep =
                    new TimeStep<>(gpsMeasurement, candidates);
            computeEmissionProbabilities(timeStep);
            if (prevTimeStep == null) {
                viterbi.startWithInitialObservation(timeStep.observation, timeStep.candidates,
                        timeStep.emissionLogProbabilities);
            } else {
                computeTransitionProbabilities(prevTimeStep, timeStep);
                viterbi.nextStep(timeStep.observation, timeStep.candidates,
                        timeStep.emissionLogProbabilities, timeStep.transitionLogProbabilities,
                        timeStep.roadPaths);
            }
            prevTimeStep = timeStep;
        }

        List<SequenceState<LocationOnRoad, GpsPoint, Path>> roadPositions =
                viterbi.computeMostLikelySequence();

        assertFalse(viterbi.isBroken());
        List<SequenceState<LocationOnRoad, GpsPoint, Path>> expected = new ArrayList<>();
        expected.add(new SequenceState<>(rp11, gps1, (Path) null, null));
        expected.add(new SequenceState<>(rp21, gps2, new Path(rp11, rp21), null));
        expected.add(new SequenceState<>(rp31, gps3, new Path(rp21, rp31), null));
        expected.add(new SequenceState<>(rp41, gps4, new Path(rp31, rp41), null));
        int i = 0;
        System.out.println("****************");
        for (SequenceState expect : expected){
            SequenceState a = roadPositions.get(i);
            System.out.println(a.toString());
            System.out.println(expect.toString());
            assertEquals(a.toString(), expect.toString());
            i++;
        }
        System.out.println("****************");
//        assertEquals(expected, roadPositions);
    }

    public static void main(String[] args) throws Exception {
        setUpClass();
        new testMarkov().testMapMatching();
    }
}
