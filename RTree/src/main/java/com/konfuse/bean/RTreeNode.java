package com.konfuse.bean;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @Author: Konfuse
 * @Date: 2019/11/26 11:10
 */
public class RTreeNode extends Entry implements Serializable {
    public ArrayList<Entry> entries;
    public int height = 1;

    public RTreeNode(int M, int height) {
        super(new MBR());
        this.height = height;
        this.entries = new ArrayList<Entry>(M);
    }

    public RTreeNode(RTreeNode node) {
        super(new MBR());
        this.entries = new ArrayList<Entry>(node.entries);
        this.height = node.height;
        this.mbr = new MBR(node.mbr);
    }

    public ArrayList<Entry> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<Entry> entries) {
        this.entries = entries;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public MBR[] getMBRs(){
        MBR[] mbrs = new MBR[entries.size()];
        int i = 0;
        for(Entry entry : entries){
            mbrs[i] = entry.mbr;
            i++;
        }
        return mbrs;
    }
}
