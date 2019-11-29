package com.konfuse.internal;

import java.io.Serializable;

/**
 * @Author: Konfuse
 * @Date: 2019/11/26 10:49
 */
public abstract class TreeNode implements Serializable {
    int height;
    MBR mbr;
    TreeNode(MBR mbr, int height) {
        this.mbr = mbr;
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public MBR getMBR() {
        return mbr;
    }

    public void setMBR(MBR mbr) {
        this.mbr = mbr;
    }
}
