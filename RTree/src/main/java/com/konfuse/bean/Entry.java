package com.konfuse.bean;

import java.io.Serializable;

/**
 * @Author: Konfuse
 * @Date: 2019/11/26 10:49
 */
public abstract class Entry implements Serializable {
    MBR mbr;
    Entry(MBR mbr) {
        this.mbr = mbr;
    }

    public MBR getMBR() {
        return mbr;
    }

    public void setMBR(MBR mbr) {
        this.mbr = mbr;
    }
}
