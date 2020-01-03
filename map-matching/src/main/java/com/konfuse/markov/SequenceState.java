package com.konfuse.markov;

/**
 * State of the most likely sequence with additional information.
 *
 * @param <S> the state type
 * @param <O> the observation type
 * @param <D> the transition descriptor type
 */
public class SequenceState<S, O, D> {

    public final S state;

    /**
     * Null if HMM was started with initial state probabilities and state is the initial state.
     */
    public final O observation;

    /**
     * Null if transition descriptor was not provided.
     */
    public final D transitionDescriptor;

    /**
     * Probability of this state given all observations.
     */
    public final Double smoothingProbability;

    public SequenceState(S state, O observation, D transitionDescriptor, Double smoothingProbability) {
        this.state = state;
        this.observation = observation;
        this.transitionDescriptor = transitionDescriptor;
        this.smoothingProbability = smoothingProbability;
    }

    public S getState() {
        return state;
    }

    public O getObservation() {
        return observation;
    }

    public D getTransitionDescriptor() {
        return transitionDescriptor;
    }

    public Double getSmoothingProbability() {
        return smoothingProbability;
    }

    @Override
    public String toString() {
        return "SequenceState{" +
                "state=" + state +
                ", observation=" + observation +
                ", transitionDescriptor=" + transitionDescriptor +
                ", smoothingProbability=" + smoothingProbability +
                '}';
    }
}
