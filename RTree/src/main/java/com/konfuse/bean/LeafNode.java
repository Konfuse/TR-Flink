package com.konfuse.bean;

import java.io.Serializable;

/**
 * @Author: Konfuse
 * @Date: 2019/11/26 11:01
 */
public class LeafNode extends Entry implements Serializable {
    private String describe = "";
    public LeafNode(MBR mbr) {
        super(mbr);
    }

    public LeafNode(String describe, MBR mbr) {
        super(mbr);
        this.describe = describe;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    @Override
    public String toString() {
        return "LeafNode{" +
                "describe='" + describe + '\'' +
                ", mbr=" + mbr +
                '}';
    }
}
