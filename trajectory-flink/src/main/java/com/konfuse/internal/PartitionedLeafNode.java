package com.konfuse.internal;

import java.io.Serializable;

/**
 * @Author: Konfuse
 * @Date: 2019/12/6 15:23
 */
public class PartitionedLeafNode extends TreeNode implements Serializable {
    private int partitionNumber;
    private long entryCount;

    PartitionedLeafNode(MBR mbr, int height, int partitionNumber, long entryCount) {
        super(mbr, height);
        this.partitionNumber = partitionNumber;
        this.entryCount = entryCount;
    }

    public long getEntryCount() {
        return entryCount;
    }

    public int getPartitionNumber() {
        return partitionNumber;
    }

    public void setPartitionNumber(int partitionNumber) {
        this.partitionNumber = partitionNumber;
    }

    @Override
    public String toString() {
        return "PartitionedLeafNode{" +
                "partitionNumber=" + partitionNumber +
                ", entryCount=" + entryCount +
                ", height=" + height +
                ", mbr=" + mbr +
                '}';
    }
}
