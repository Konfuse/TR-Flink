package com.konfuse;

import java.io.Serializable;

/**
 * @Author: Konfuse
 * @Date: 2019/12/6 15:33
 */
public class IndexBuilder implements Serializable {
    private int M = 40;
    private int m = 16;

    public IndexBuilder(int M, int m) {
        this.M = M;
        this.m = m;
    }
}
