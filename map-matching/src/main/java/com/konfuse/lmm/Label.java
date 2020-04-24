package com.konfuse.lmm;

import java.util.Arrays;

/**
 * @Author: Konfuse
 * @Date: 2020/4/23 1:47
 */
public class Label {
    Pair[] pairs;

    public Label(int size) {
        pairs = new Pair[size];
    }

    public Label(Pair[] pairs) {
        this.pairs = pairs;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Pair pair : pairs) {
            stringBuilder.append(pair.nodeId);
            stringBuilder.append(",");
            stringBuilder.append(pair.cost);
            stringBuilder.append(";");
        }
        return stringBuilder.toString();
    }
}
