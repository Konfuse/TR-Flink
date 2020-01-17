package com.konfuse.markov;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains everything the hmm-lib needs to process a new time step including emisson and
 * observation probabilities.
 *
 * @param <S> road position type, which corresponds to the HMM state.
 * @param <O> location measurement type, which corresponds to the HMM observation.
 * @param <D> road path object
 */
public class TimeStep<S, O, D> {

    /**
     * Observation made at this time step.
     */
    public final O observation;

    /**
     * State candidates at this time step.
     */
    public final Collection<S> candidates;

    public final Map<S, Double> emissionLogProbabilities = new HashMap<>();
    public final Map<Transition<S>, Double> transitionLogProbabilities = new HashMap<>();

    /**
     * BaseRoad paths between all candidates pairs of the previous and the current time step.
     */
    public final Map<Transition<S>, D> roadPaths = new HashMap<>();


    public TimeStep(O observation, Collection<S> candidates) {
        if (observation == null || candidates == null) {
            throw new NullPointerException("observation and candidates must not be null.");
        }
        this.observation = observation;
        this.candidates = candidates;
    }

    public void addEmissionLogProbability(S candidate, double emissionLogProbability) {
        if (emissionLogProbabilities.containsKey(candidate)) {
            throw new IllegalArgumentException("Candidate has already been added.");
        }
        emissionLogProbabilities.put(candidate, emissionLogProbability);
    }

    /**
     * Does not need to be called for non-existent transitions.
     */
    public void addTransitionLogProbability(S fromPosition, S toPosition, double transitionLogProbability) {
        final Transition<S> transition = new Transition<>(fromPosition, toPosition);
        if (transitionLogProbabilities.containsKey(transition)) {
            throw new IllegalArgumentException("Transition has already been added.");
        }
        transitionLogProbabilities.put(transition, transitionLogProbability);
    }

    /**
     * Does not need to be called for non-existent transitions.
     */
    public void addRoadPath(S fromPosition, S toPosition, D roadPath) {
        final Transition<S> transition = new Transition<>(fromPosition, toPosition);
        if (roadPaths.containsKey(transition)) {
            throw new IllegalArgumentException("Transition has already been added.");
        }
        roadPaths.put(transition, roadPath);
    }

}
