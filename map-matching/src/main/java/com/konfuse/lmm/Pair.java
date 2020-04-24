package com.konfuse.lmm;

import java.util.Comparator;
import java.util.Objects;

/**
 * @Author: Konfuse
 * @Date: 2020/4/22 23:20
 */
public class Pair {
    long nodeId;
    double cost;

    public Pair(long nodeId, double cost) {
        this.nodeId = nodeId;
        this.cost = cost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair pair = (Pair) o;
        return nodeId == pair.nodeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, cost);
    }

    public static class PairComparator implements Comparator<Pair> {
        @Override
        public int compare(Pair o1, Pair o2) {
            return Long.compare(o1.nodeId, o2.nodeId);
        }
    }
}
