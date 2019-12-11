package com.konfuse.HMM;

import java.util.*;

/**
 * @Auther todd
 * @Date 2019/12/11
 */
public class ViterbiAlgorithm {
    /**
     * Stores addition information for each candidate.
     */
    private static class ExtendedState {

        RoadPosition state;
        GPSMeasurement observation;
        RoadPath transitionDescriptor;
        /**
         * Back pointer to previous state candidate in the most likely sequence.
         * Back pointers are chained using plain Java references.
         * This allows garbage collection of unreachable back pointers.
         */
        ExtendedState backPointer;

        ExtendedState(RoadPosition state, ExtendedState backPointer, GPSMeasurement observation, RoadPath transitionDescriptor) {
            this.state = state;
            this.backPointer = backPointer;
            this.observation = observation;
            this.transitionDescriptor = transitionDescriptor;
        }
    }

    private static class ForwardStepResult {
        final Map<RoadPosition, Double> newMessage;
        /**
         * Includes back pointers to previous state candidates for retrieving the most likely
         * sequence after the forward pass.
         */
        final Map<RoadPosition, ExtendedState> newExtendedStates;

        ForwardStepResult(int numberStates) {
            newMessage = new LinkedHashMap<>(Utils.initialHashMapCapacity(numberStates));
            newExtendedStates = new LinkedHashMap<>(Utils.initialHashMapCapacity(numberStates));
        }
    }

    /**
     * Allows to retrieve the most likely sequence using back pointers.
     */
    private Map<RoadPosition, ExtendedState> lastExtendedStates;

    private ArrayList<RoadPosition> prevCandidates;

    /**
     * For each state s_t of the current time step t, message.get(s_t) contains the log
     * probability of the most likely sequence ending in state s_t with given observations
     * o_1, ..., o_t.
     *
     * Formally, this is max log p(s_1, ..., s_t, o_1, ..., o_t) w.r.t. s_1, ..., s_{t-1}.
     * Note that to compute the most likely state sequence, it is sufficient and more
     * efficient to compute in each time step the joint probability of states and observations
     * instead of computing the conditional probability of states given the observations.
     */
    private Map<RoadPosition, Double> message;

    private boolean isBroken = false;

    private ForwardBackwardAlgorithm forwardBackward;

    private List<Map<RoadPosition, Double>> messageHistory; // For debugging only.

    /**
     * Need to construct a new instance for each sequence of observations.
     */

    public ViterbiAlgorithm() { }

    /**
     * Whether to store intermediate forward messages
     * (probabilities of intermediate most likely paths) for debugging.
     * Default: false
     * Must be called before processing is started.
     */
    public ViterbiAlgorithm setKeepMessageHistory(boolean keepMessageHistory) {
        if (processingStarted()) {
            throw new IllegalStateException("Processing has already started.");
        }

        if (keepMessageHistory) {
            messageHistory = new ArrayList<>();
        } else {
            messageHistory = null;
        }
        return this;
    }

    /**
     * Whether to compute smoothing probabilities using the {@link ForwardBackwardAlgorithm}
     * for the states of the most likely sequence. Note that this significantly increases
     * computation time and memory footprint.
     * Default: false
     * Must be called before processing is started.
     */
    public ViterbiAlgorithm setComputeSmoothingProbabilities(
            boolean computeSmoothingProbabilities) {
        if (processingStarted()) {
            throw new IllegalStateException("Processing has already started.");
        }

        if (computeSmoothingProbabilities) {
            forwardBackward = new ForwardBackwardAlgorithm();
        } else {
            forwardBackward = null;
        }
        return this;
    }

    public boolean processingStarted() {
        return message != null;
    }


    public void startWithInitialStateProbabilities(ArrayList<RoadPosition> initialStates,
                                                   Map<RoadPosition, Double> initialLogProbabilities) {
        initializeStateProbabilities(null, initialStates, initialLogProbabilities);

        if (forwardBackward != null) {
            forwardBackward.startWithInitialStateProbabilities(initialStates,
                    Utils.logToNonLogProbabilities(initialLogProbabilities));
        }
    }


    public void startWithInitialObservation(GPSMeasurement observation, ArrayList<RoadPosition> candidates,
                                            Map<RoadPosition, Double> measurementLogProbabilities) {
        initializeStateProbabilities(observation, candidates, measurementLogProbabilities);

        if (forwardBackward != null) {
            forwardBackward.startWithInitialObservation(observation, candidates,
                    Utils.logToNonLogProbabilities(measurementLogProbabilities));
        }
    }


    public void nextStep(GPSMeasurement observation, ArrayList<RoadPosition> candidates,
                         Map<RoadPosition, Double> measurementLogProbabilities,
                         Map<Transition, Double> transitionLogProbabilities,
                         Map<Transition, RoadPath> transitionDescriptors) {
        if (!processingStarted()) {
            throw new IllegalStateException(
                    "startWithInitialStateProbabilities() or startWithInitialObservation() "
                            + "must be called first.");
        }
        if (isBroken) {
            throw new IllegalStateException("Method must not be called after an HMM break.");
        }

        // Forward step
        ForwardStepResult forwardStepResult = forwardStep(observation, prevCandidates,
                candidates, message, measurementLogProbabilities, transitionLogProbabilities,
                transitionDescriptors);
        isBroken = hmmBreak(forwardStepResult.newMessage);
        if (isBroken){
            return;
        }
        if (messageHistory != null) {
            messageHistory.add(forwardStepResult.newMessage);
        }
        message = forwardStepResult.newMessage;
        lastExtendedStates = forwardStepResult.newExtendedStates;

        prevCandidates = new ArrayList<>(candidates); // Defensive copy.

        if (forwardBackward != null) {
            forwardBackward.nextStep(observation, candidates,
                    Utils.logToNonLogProbabilities(measurementLogProbabilities),
                    Utils.logToNonLogProbabilities(transitionLogProbabilities));
        }
    }

    public void nextStep(GPSMeasurement observation, ArrayList<RoadPosition> candidates,
                         Map<RoadPosition, Double> measurementLogProbabilities,
                         Map<Transition, Double> transitionLogProbabilities) {
        nextStep(observation, candidates, measurementLogProbabilities, transitionLogProbabilities,
                new LinkedHashMap<Transition, RoadPath>());
    }


    public List<SequenceState> computeMostLikelySequence() {
        if (message == null) {
            // Return empty most likely sequence if there are no time steps or if initial
            // observations caused an HMM break.
            return new ArrayList<>();
        } else {
            return retrieveMostLikelySequence();
        }
    }

    /**
     * Returns whether an HMM occurred in the last time step.
     *
     * An HMM break means that the probability of all states equals zero.
     */
    public boolean isBroken() {
        return isBroken;
    }

    /**
     * @see #setComputeSmoothingProbabilities(boolean)
     */
    public boolean isComputeSmoothingProbabilities() {
        return forwardBackward != null;
    }

    /**
     * @see #setKeepMessageHistory(boolean)
     */
    public boolean isKeepMessageHistory() {
        return messageHistory != null;
    }

    /**
     *  Returns the sequence of intermediate forward messages for each time step.
     *  Returns null if message history is not kept.
     */
    public List<Map<RoadPosition, Double>> messageHistory() {
        return messageHistory;
    }

    public String messageHistoryString() {
        if (messageHistory == null) {
            throw new IllegalStateException("Message history was not recorded.");
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Message history with log probabilies\n\n");
        int i = 0;
        for (Map<RoadPosition, Double> message : messageHistory) {
            sb.append("Time step " + i + "\n");
            i++;
            for (RoadPosition state : message.keySet()) {
                sb.append(state + ": " + message.get(state) + "\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns whether the specified message is either empty or only contains state candidates
     * with zero probability and thus causes the HMM to break.
     */
    private boolean hmmBreak(Map<RoadPosition, Double> message) {
        for (double logProbability : message.values()) {
            if (logProbability != Double.NEGATIVE_INFINITY) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param observation Use only if HMM only starts with first observation.
     */
    private void initializeStateProbabilities(GPSMeasurement observation, ArrayList<RoadPosition> candidates,
                                              Map<RoadPosition, Double> initialLogProbabilities) {
        if (processingStarted()) {
            throw new IllegalStateException("Initial probabilities have already been set.");
        }

        // Set initial log probability for each start state candidate based on first observation.
        // Do not assign initialLogProbabilities directly to message to not rely on its iteration
        // order.
        final Map<RoadPosition, Double> initialMessage = new LinkedHashMap<>();
        for (RoadPosition candidate : candidates) {
            final Double logProbability = initialLogProbabilities.get(candidate);
            if (logProbability == null) {
                throw new NullPointerException("No initial probability for " + candidate);
            }
            initialMessage.put(candidate, logProbability);
        }

        isBroken = hmmBreak(initialMessage);
        if (isBroken) {
            return;
        }

        message = initialMessage;
        if (messageHistory != null) {
            messageHistory.add(message);
        }

        lastExtendedStates = new LinkedHashMap<>();
        for (RoadPosition candidate : candidates) {
            lastExtendedStates.put(candidate,
                    new ExtendedState(candidate, null, observation, null));
        }

        prevCandidates = new ArrayList<>(candidates); // Defensive copy.
    }

    /**
     * Computes the new forward message and the back pointers to the previous states.
     *
     * @throws NullPointerException if any measurement probability is missing
     */
    private ForwardStepResult forwardStep(GPSMeasurement observation, ArrayList<RoadPosition> prevCandidates,
                                          ArrayList<RoadPosition> curCandidates, Map<RoadPosition, Double> message,
                                                   Map<RoadPosition, Double> measurementLogProbabilities,
                                                   Map<Transition, Double> transitionLogProbabilities,
                                                   Map<Transition,RoadPath> transitionDescriptors) {
        final ForwardStepResult result = new ForwardStepResult(curCandidates.size());
        assert !prevCandidates.isEmpty();

        for (RoadPosition curState : curCandidates) {
            double maxLogProbability = Double.NEGATIVE_INFINITY;
            RoadPosition maxPrevState = null;
            for (RoadPosition prevState : prevCandidates) {
                final double logProbability = message.get(prevState) + transitionLogProbability(
                        prevState, curState, transitionLogProbabilities);
                if (logProbability > maxLogProbability) {
                    maxLogProbability = logProbability;
                    maxPrevState = prevState;
                }
            }
            // Throws NullPointerException if curState is not stored in the map.
            result.newMessage.put(curState, maxLogProbability
                    + measurementLogProbabilities.get(curState));

            // Note that maxPrevState == null if there is no transition with non-zero probability.
            // In this case curState has zero probability and will not be part of the most likely
            // sequence, so we don't need an ExtendedState.
            if (maxPrevState != null) {
                final Transition transition = new Transition(maxPrevState, curState);
                final ExtendedState extendedState = new ExtendedState(curState,
                        lastExtendedStates.get(maxPrevState), observation,
                        transitionDescriptors.get(transition));
                result.newExtendedStates.put(curState, extendedState);
            }
        }
        return result;
    }

    private double transitionLogProbability(RoadPosition prevState, RoadPosition curState, Map<Transition,
            Double> transitionLogProbabilities) {
        final Double transitionLogProbability =
                transitionLogProbabilities.get(new Transition(prevState, curState));
        if (transitionLogProbability == null) {
            return Double.NEGATIVE_INFINITY; // Transition has zero probability.
        } else {
            return transitionLogProbability;
        }
    }

    /**
     * Retrieves the first state of the current forward message with maximum probability.
     */
    private RoadPosition mostLikelyState() {
        // Otherwise an HMM break would have occurred and message would be null.
        assert !message.isEmpty();

        RoadPosition result = null;
        double maxLogProbability = Double.NEGATIVE_INFINITY;
        for (Map.Entry<RoadPosition, Double> entry : message.entrySet()) {
            if (entry.getValue() > maxLogProbability) {
                result = entry.getKey();
                maxLogProbability = entry.getValue();
            }
        }

        assert result != null; // Otherwise an HMM break would have occurred.
        return result;
    }

    /**
     * Retrieves most likely sequence from the internal back pointer sequence.
     */
    private List<SequenceState> retrieveMostLikelySequence() {
        // Otherwise an HMM break would have occurred and message would be null.
        assert !message.isEmpty();

        final RoadPosition lastState = mostLikelyState();

        // Retrieve most likely state sequence in reverse order
        final List<SequenceState> result = new ArrayList<>();
        ExtendedState es = lastExtendedStates.get(lastState);
        final ListIterator<Map<RoadPosition, Double>> smoothingIter;
        if (forwardBackward != null) {
            List<Map<RoadPosition, Double>> smoothingProbabilities =
                    forwardBackward.computeSmoothingProbabilities();
            smoothingIter = smoothingProbabilities.listIterator(smoothingProbabilities.size());
        } else {
            smoothingIter = null;
        }
        while(es != null) {
            final Double smoothingProbability;
            if (forwardBackward != null) {
                // Number of time steps is the same for Viterbi and ForwardBackward algorithm.
                assert smoothingIter.hasPrevious();
                final Map<RoadPosition, Double> smoothingProbabilitiesVector = smoothingIter.previous();
                smoothingProbability = smoothingProbabilitiesVector.get(es.state);
            } else {
                smoothingProbability = null;
            }
            final SequenceState ss = new SequenceState(es.observation, es.state,
                    es.transitionDescriptor, smoothingProbability);
            result.add(ss);
            es = es.backPointer;
        }

        Collections.reverse(result);
        return result;
    }
}
