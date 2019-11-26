package com.konfuse.bean;

import java.io.Serializable;

/**
 * @Author: Konfuse
 * @Date: 2019/11/26 10:49
 */
public abstract class Entry implements Serializable {
    public MBR mbr;
    Entry(MBR mbr) {
        this.mbr = mbr;
    }

    public MBR getMbr() {
        return mbr;
    }

    public void setMbr(MBR mbr) {
        this.mbr = mbr;
    }
}
