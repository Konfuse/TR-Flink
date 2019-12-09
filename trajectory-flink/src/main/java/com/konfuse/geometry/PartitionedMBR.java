package com.konfuse.geometry;

import com.konfuse.geometry.MBR;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @Author: Konfuse
 * @Date: 2019/12/6 15:23
 */
public class PartitionedMBR extends DataObject implements Serializable {
    private MBR mbr;
    private long entryCount;
    private int partitionNumber;

    public PartitionedMBR(MBR mbr, int partitionNumber, long entryCount) {
        super(0);
        this.partitionNumber = partitionNumber;
        this.mbr = mbr;
        this.entryCount = entryCount;
    }

    public int getPartitionNumber() {
        return partitionNumber;
    }

    public void setPartitionNumber(int partitionNumber) {
        this.partitionNumber = partitionNumber;
    }

    public MBR getMBR() {
        return mbr;
    }

    public long getEntryCount() {
        return entryCount;
    }

    /**
     * Inner class PartitionedMBRComparator.
     * A comparator of PartitionedMBR, compare mbrs by specified dimensions.
     */
    public static class PartitionedMBRComparator implements Comparator<PartitionedMBR> {
        private int dimension;
        private boolean low;

        /**
         * @param dimension if 1, then compare mbrs by x, else if 2, compare mbrs by y.
         * @param low if true, then compare mbrs by the lower bound, else
         *            compare mbrs by the lower bound.
         */
        public PartitionedMBRComparator(int dimension, boolean low) {
            this.dimension = dimension;
            this.low = low;
        }


        @Override
        public int compare(PartitionedMBR p1, PartitionedMBR p2) {
            if (dimension == 1) {
                if (low) {
                    return Double.compare(p1.getMBR().getX1(), p2.getMBR().getX1());
                } else {
                    return Double.compare(p1.getMBR().getX2(), p2.getMBR().getX2());
                }
            } else {
                if (low) {
                    return Double.compare(p1.getMBR().getY1(), p2.getMBR().getY1());
                } else {
                    return Double.compare(p1.getMBR().getY2(), p2.getMBR().getY2());
                }
            }
        }
    }

    @Override
    public double calDistance(Point point) {
        return mbr.calculateDistanceToBorder(point);
    }

    @Override
    public String toString() {
        return "PartitionedMBR{" +
                "mbr=" + mbr +
                ", entryCount=" + entryCount +
                ", id=" + id +
                '}';
    }
}
