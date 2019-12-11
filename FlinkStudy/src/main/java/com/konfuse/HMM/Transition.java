package com.konfuse.HMM;

import java.util.Objects;

/**
 * @Auther todd
 * @Date 2019/12/11
 */
public class Transition {
    public RoadPosition fromCandidate;
    public RoadPosition toCandidate;

    public Transition(RoadPosition fromCandidate, RoadPosition toCandidate) {
        this.fromCandidate = fromCandidate;
        this.toCandidate = toCandidate;
    }

    @Override
    public String toString() {
        return "Transition{" +
                "fromCandidate=" + fromCandidate +
                ", toCandidate=" + toCandidate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        Transition that = (Transition) o;
        return Objects.equals(fromCandidate, that.fromCandidate) && Objects.equals(toCandidate, that.toCandidate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromCandidate, toCandidate);
    }
}
