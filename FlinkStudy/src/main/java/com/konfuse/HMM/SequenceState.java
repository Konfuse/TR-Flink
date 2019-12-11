package com.konfuse.HMM;

/**
 * @Auther todd
 * @Date 2019/12/11
 * State of the most likely sequence with additional information.
 */
public class SequenceState {
    public final GPSMeasurement observation;
    public final RoadPosition state;
    public final RoadPath transitionDescriptor;
    public final Double smoothingProbability;

    public SequenceState(GPSMeasurement observation, RoadPosition state, RoadPath transitionDescriptor, Double smoothingProbability) {
        this.observation = observation;
        this.state = state;
        this.transitionDescriptor = transitionDescriptor;
        this.smoothingProbability = smoothingProbability;
    }

    @Override
    public String toString() {
        return "SequenceState{" +
                "observation=" + observation +
                ", state=" + state +
                ", transitionDescriptor=" + transitionDescriptor +
                ", smoothingProbability=" + smoothingProbability +
                '}';
    }
}
