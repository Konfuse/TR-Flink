package com.konfuse.roadnetwork;

import java.util.Objects;

/**
 * @Auther todd
 * @Date 2019/12/12
 */
public class Path {
    // The following members are used to check whether the correct road paths are retrieved
    // from the most likely sequence.
    public final LocationOnRoad from;
    public final LocationOnRoad to;

    public Path(LocationOnRoad from, LocationOnRoad to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj){
            return true;
        }
        if (obj == null){
            return false;
        }
        if (getClass() != obj.getClass()){
            return false;
        }
        Path other = (Path) obj;
        if (from == null) {
            if (other.from != null){
                return false;
            }
        } else if (!from.equals(other.from)){
            return false;
        }
        if (to == null) {
            if (other.to != null){
                return false;
            }
        } else if (!to.equals(other.to)){
            return false;
        }
        return true;
    }
}
