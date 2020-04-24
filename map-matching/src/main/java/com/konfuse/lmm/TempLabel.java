package com.konfuse.lmm;

import java.util.LinkedList;
import java.util.List;

/**
 * @Author: Konfuse
 * @Date: 2020/4/22 17:02
 */
public class TempLabel {
    List<Pair> pairs;

    public TempLabel() {
        pairs = new LinkedList<>();
    }

    public TempLabel(long nodeId, double cost) {
        pairs = new LinkedList<>();
        add(nodeId, cost);
    }

    public void add(Pair pair) {
        pairs.add(pair);
    }

    public void add(long nodeId, double cost) {
        add(new Pair(nodeId, cost));
    }
}
