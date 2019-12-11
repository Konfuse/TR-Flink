package com.konfuse.HMM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @Auther todd
 * @Date 2019/12/11
 */
public class TimeStep {
    public final GPSMeasurement observation;
    public final ArrayList<RoadPosition> candidates;
    public final Map<RoadPosition, Double> measurementLogProbabilities = new HashMap<>();
    public final Map<Transition, Double> transitionLogProbabilities = new HashMap<>();
    public final Map<Transition, RoadPath> roadPaths = new HashMap<>();

    public TimeStep(GPSMeasurement observation, ArrayList<RoadPosition> candidates) {
        if (observation == null || candidates == null) {
            throw new NullPointerException("observation and candidates must not be null.");
        }
        this.observation = observation;
        this.candidates = candidates;
    }

    public void addMeasurementLogProbability(RoadPosition candidate, double measurementLogProbability) {
        if (measurementLogProbabilities.containsKey(candidate)) {
            throw new IllegalArgumentException("Candidate has already been added.");
        }
        measurementLogProbabilities.put(candidate, measurementLogProbability);
    }

    public void addTransitionLogProbability(RoadPosition fromPosition, RoadPosition toPosition, double transitionLogProbability) {
        final Transition transition = new Transition(fromPosition, toPosition);
        if (transitionLogProbabilities.containsKey(transition)) {
            throw new IllegalArgumentException("Transition has already been added.");
        }
        transitionLogProbabilities.put(transition, transitionLogProbability);
    }

    /**
     * Does not need to be called for non-existent transitions.
     */
    public void addRoadPath(RoadPosition fromPosition, RoadPosition toPosition, RoadPath roadPath) {
        final Transition transition = new Transition(fromPosition, toPosition);
        if (roadPaths.containsKey(transition)) {
            throw new IllegalArgumentException("Transition has already been added.");
        }
        roadPaths.put(transition, roadPath);
    }
}
