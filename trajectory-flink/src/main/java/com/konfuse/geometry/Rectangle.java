package com.konfuse.geometry;

import com.konfuse.internal.MBR;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @Author: Konfuse
 * @Date: 2019/12/29 20:07
 */
public class Rectangle extends DataObject implements Serializable {
    private MBR mbr;

    public Rectangle(long id, MBR mbr) {
        super(id);
        this.mbr = mbr;
    }

    public Rectangle(long id, double x1, double y1, double x2, double y2) {
        super(id);
        this.mbr = new MBR(x1, y1, x2, y2);
    }

    @Override
    public double calDistance(Point point) {
        return mbr.calculateDistanceToBorder(point);
    }

    public MBR getMBR() {
        return mbr;
    }

    public void setMBR(MBR mbr) {
        this.mbr = mbr;
    }

    /**
     * Inner class RectangleComparator.
     * A comparator of Rectangle, compare mbrs by specified dimensions.
     */
    public static class RectangleComparator implements Comparator<Rectangle> {
        private int dimension;
        private boolean low;

        /**
         * @param dimension if 1, then compare mbrs by x, else if 2, compare mbrs by y.
         * @param low if true, then compare mbrs by the lower bound, else
         *            compare mbrs by the lower bound.
         */
        public RectangleComparator(int dimension, boolean low) {
            this.dimension = dimension;
            this.low = low;
        }


        @Override
        public int compare(Rectangle p1, Rectangle p2) {
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
}
