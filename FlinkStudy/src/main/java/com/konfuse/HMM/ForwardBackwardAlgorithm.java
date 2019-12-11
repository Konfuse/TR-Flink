package com.konfuse.HMM;

import com.konfuse.bean.Point;

import java.util.*;

/**
 * @Auther todd
 * @Date 2019/12/11
 */
public class ForwardBackwardAlgorithm {
    /**
     * Internal state of each time step.
     */
    private class Step {
        final ArrayList<RoadPosition> candidates;
        final Map<RoadPosition, Double> measurementProbabilities;
        final Map<Transition, Double> transitionProbabilities;
        final Map<RoadPosition, Double> forwardProbabilities;
        final double scalingDivisor; // Normalizes sum of forward probabilities to 1.

        Step(ArrayList<RoadPosition> candidates, Map<RoadPosition, Double> measurementProbabilities,
             Map<Transition, Double> transitionProbabilities,
             Map<RoadPosition, Double> forwardProbabilities, double scalingDivisor) {
            this.candidates = candidates;
            this.measurementProbabilities = measurementProbabilities;
            this.transitionProbabilities = transitionProbabilities;
            this.forwardProbabilities = forwardProbabilities;
            this.scalingDivisor = scalingDivisor;
        }
    }

    private static final double DELTA = 1e-8;
    private List<Step> steps;
    private ArrayList<RoadPosition> prevCandidates; // For on-the-fly computation of forward probabilities

    public void startWithInitialStateProbabilities(ArrayList<RoadPosition> initialStates, Map<RoadPosition, Double> initialProbabilities) {
        if (!sumsToOne(initialProbabilities.values())) {
            throw new IllegalArgumentException("Initial state probabilities must sum to 1.");
        }

        initializeStateProbabilities(null, initialStates, initialProbabilities);
    }

    public void startWithInitialObservation(GPSMeasurement observation, ArrayList<RoadPosition> candidates,
                                            Map<RoadPosition, Double> measurementProbabilities) {
        initializeStateProbabilities(observation, candidates, measurementProbabilities);
    }

    public void nextStep(GPSMeasurement observation, ArrayList<RoadPosition> candidates,
                         Map<RoadPosition, Double> measurementProbabilities,
                         Map<Transition, Double> transitionProbabilities) {
        if (steps == null) {
            throw new IllegalStateException("startWithInitialStateProbabilities(...) or " +
                    "startWithInitialObservation(...) must be called first.");
        }

        measurementProbabilities = new LinkedHashMap<>(measurementProbabilities);
        transitionProbabilities = new LinkedHashMap<>(transitionProbabilities);

        // On-the-fly computation of forward probabilities at each step allows to efficiently
        // (re)compute smoothing probabilities at any time step.
        final Map<RoadPosition, Double> prevForwardProbabilities =
                steps.get(steps.size() - 1).forwardProbabilities;
        final Map<RoadPosition, Double> curForwardProbabilities = new LinkedHashMap<>();
        double sum = 0.0;
        for (RoadPosition curState : candidates) {
            final double forwardProbability = computeForwardProbability(curState,
                    prevForwardProbabilities, measurementProbabilities, transitionProbabilities);
            curForwardProbabilities.put(curState, forwardProbability);
            sum += forwardProbability;
        }

        normalizeForwardProbabilities(curForwardProbabilities, sum);
        steps.add(new Step(candidates, measurementProbabilities, transitionProbabilities,
                curForwardProbabilities, sum));

        prevCandidates = candidates;
    }

    /**
     * Returns the probability for all candidates of all time steps given all observations.
     * The time steps include the initial states/observations time step.
     */
    public List<Map<RoadPosition, Double>> computeSmoothingProbabilities() {
        return computeSmoothingProbabilities(null);
    }

    /**
     * Returns the probability of the specified candidate at the specified zero-based time step
     * given the observations up to t.
     */
    public double forwardProbability(int t, RoadPosition candidate) {
        if (steps == null) {
            throw new IllegalStateException("No time steps yet.");
        }

        return steps.get(t).forwardProbabilities.get(candidate);
    }

    /**
     * Returns the probability of the specified candidate given all previous observations.
     */
    public double currentForwardProbability(RoadPosition candidate) {
        if (steps == null) {
            throw new IllegalStateException("No time steps yet.");
        }

        return forwardProbability(steps.size() - 1, candidate);
    }

    /**
     * Returns the log probability of the entire observation sequence.
     * The log is returned to prevent arithmetic underflows for very small probabilities.
     */
    public double observationLogProbability() {
        if (steps == null) {
            throw new IllegalStateException("No time steps yet.");
        }

        double result = 0.0;
        for (Step step : steps) {
            result += Math.log(step.scalingDivisor);
        }
        return result;
    }

    /**
     * @see #computeSmoothingProbabilities()
     *
     * @param outBackwardProbabilities optional output parameter for backward probabilities, must be empty if not null.
     */
    List<Map<RoadPosition, Double>> computeSmoothingProbabilities( List<Map<RoadPosition, Double>> outBackwardProbabilities) {
        assert outBackwardProbabilities == null || outBackwardProbabilities.isEmpty();

        final List<Map<RoadPosition, Double>> result = new ArrayList<>();

        ListIterator<Step> stepIter = steps.listIterator(steps.size());
        if (!stepIter.hasPrevious()) {
            return result;
        }

        // Initial step
        Step step = stepIter.previous();
        Map<RoadPosition, Double> backwardProbabilities = new LinkedHashMap<>();
        for (RoadPosition candidate : step.candidates) {
            backwardProbabilities.put(candidate, 1.0);
        }
        if (outBackwardProbabilities != null) {
            outBackwardProbabilities.add(backwardProbabilities);
        }
        result.add(computeSmoothingProbabilitiesVector(step.candidates, step.forwardProbabilities, backwardProbabilities));

        // Remaining steps
        while (stepIter.hasPrevious()) {
            final Step nextStep = step;
            step = stepIter.previous();
            final Map<RoadPosition, Double> nextBackwardProbabilities = backwardProbabilities;
            backwardProbabilities = new LinkedHashMap<>();
            for (RoadPosition candidate : step.candidates) {
                // Using the scaling divisors of the next steps eliminates the need to
                // normalize the smoothing probabilities,
                // see also https://en.wikipedia.org/wiki/Forward%E2%80%93backward_algorithm.
                final double probability = computeUnscaledBackwardProbability(candidate, nextBackwardProbabilities, nextStep) / nextStep.scalingDivisor;
                backwardProbabilities.put(candidate, probability);
            }
            if (outBackwardProbabilities != null) {
                outBackwardProbabilities.add(backwardProbabilities);
            }
            result.add(computeSmoothingProbabilitiesVector(step.candidates, step.forwardProbabilities, backwardProbabilities));
        }
        Collections.reverse(result);
        return result;
    }

    private Map<RoadPosition, Double> computeSmoothingProbabilitiesVector(ArrayList<RoadPosition> candidates, Map<RoadPosition, Double> forwardProbabilities, Map<RoadPosition, Double> backwardProbabilities) {
        assert forwardProbabilities.size() == backwardProbabilities.size();
        final Map<RoadPosition, Double> result = new LinkedHashMap<>();
        for (RoadPosition state : candidates) {
            final double probability = forwardProbabilities.get(state) * backwardProbabilities.get(state);
            assert Utils.probabilityInRange(probability, DELTA);
            result.put(state, probability);
        }
        assert sumsToOne(result.values());
        return result;
    }

    private double computeUnscaledBackwardProbability(RoadPosition candidate, Map<RoadPosition, Double> nextBackwardProbabilities, Step nextStep) {
        double result = 0.0;
        for (RoadPosition nextCandidate : nextStep.candidates) {
            result += nextStep.measurementProbabilities.get(nextCandidate) * nextBackwardProbabilities.get(nextCandidate) * transitionProbability(candidate, nextCandidate, nextStep.transitionProbabilities);
        }
        return result;
    }

    private boolean sumsToOne(Collection<Double> probabilities) {
        double sum = 0.0;
        for (double probability : probabilities) {
            sum += probability;
        }
        return Math.abs(sum - 1.0) <= DELTA;
    }

    /**
     * @param observation Use only if HMM only starts with first observation.
     */
    private void initializeStateProbabilities(GPSMeasurement observation, ArrayList<RoadPosition> candidates,
                                              Map<RoadPosition, Double> initialProbabilities) {
        if (steps != null) {
            throw new IllegalStateException("Initial probabilities have already been set.");
        }

        steps = new ArrayList<>();

        final Map<RoadPosition, Double> forwardProbabilities = new LinkedHashMap<>();
        double sum = 0.0;
        for (RoadPosition candidate : candidates) {
            final double forwardProbability = initialProbabilities.get(candidate);
            forwardProbabilities.put(candidate, forwardProbability);
            sum += forwardProbability;
        }

        normalizeForwardProbabilities(forwardProbabilities, sum);
        steps.add(new Step(candidates, null, null, forwardProbabilities, sum));

        prevCandidates = candidates;
    }

    /**
     * Returns the non-normalized forward probability of the specified state.
     */
    private double computeForwardProbability(RoadPosition curState, Map<RoadPosition, Double> prevForwardProbabilities, Map<RoadPosition, Double> measurementProbabilities, Map<Transition, Double> transitionProbabilities) {
        double result = 0.0;
        for (RoadPosition prevState : prevCandidates) {
            result += prevForwardProbabilities.get(prevState) * transitionProbability(prevState, curState, transitionProbabilities);
        }
        result *= measurementProbabilities.get(curState);
        return result;
    }

    /**
     * Returns zero probability for non-existing transitions.
     */
    private double transitionProbability(RoadPosition prevState, RoadPosition curState, Map<Transition, Double> transitionProbabilities) {
        final Double transitionProbability = transitionProbabilities.get(new Transition(prevState, curState));
        return transitionProbability == null ? 0.0 : transitionProbability;
    }

    private void normalizeForwardProbabilities(Map<RoadPosition, Double> forwardProbabilities, double sum) {
        for (Map.Entry<RoadPosition, Double> entry : forwardProbabilities.entrySet()) {
            forwardProbabilities.put(entry.getKey(), entry.getValue() / sum);
        }
    }
}
