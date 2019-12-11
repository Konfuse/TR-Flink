package com.konfuse.HMM;

import java.util.Objects;

/**
 * @Auther todd
 * @Date 2019/12/11
 */
public class RoadPath {

    public final RoadPosition from;
    public final RoadPosition to;

    public RoadPath(RoadPosition from, RoadPosition to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        RoadPath roadPath = (RoadPath) o;
        return from.equals(roadPath.from) && to.equals(roadPath.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
