package com.konfuse.strtree;

import java.io.Serializable;

/**
 * @author todd
 * @date 2020/5/22 10:38
 * @description: TODO
 */
public class PartitionedMBR extends MBR implements Serializable {
    private int size;
    private int partitionNumber;

    public PartitionedMBR(int size, int partitionNumber) {
        this.size = size;
        this.partitionNumber = partitionNumber;
    }

    public PartitionedMBR(MBR mbr, int size, int partitionNumber) {
        super(mbr);
        this.size = size;
        this.partitionNumber = partitionNumber;
    }

    public PartitionedMBR(double x1, double y1, double x2, double y2, int size, int partitionNumber) {
        super(x1, y1, x2, y2);
        this.size = size;
        this.partitionNumber = partitionNumber;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPartitionNumber() {
        return partitionNumber;
    }

    public void setPartitionNumber(int partitionNumber) {
        this.partitionNumber = partitionNumber;
    }

    @Override
    public String toString() {
        return "PartitionMBR{" +
                ", partitionNumber=" + partitionNumber +
                ", size=" + size +
                super.toString() +
                '}';
    }
}
