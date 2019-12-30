package com.konfuse.tools;

import com.konfuse.RTree;
import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.Line;
import com.konfuse.geometry.Point;
import com.konfuse.internal.MBR;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Use for visualization
 * Class contains four parameters to control canvas size.
 *
 * @Author: Konfuse
 * @Date: 2019/11/27 14:46
 */
public class Visualization extends JComponent {
    private static final int RECT_X = 400;
    private static final int RECT_Y = 400;
    private static final int RECT_WIDTH = 400;
    private static final int RECT_HEIGHT = 400;
    
    private RTree tree;

    private Visualization(RTree tree) {
        this.tree = tree;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int height = tree.getRoot().getHeight();
//        int control = 10000;
        int fix = 10;
        for (int i = 1; i <= height; i++) {
            ArrayList<MBR> mbrList = tree.getMBRsWithHeight(i);
            g.setColor(iterateColor(i));
            for (MBR mbr : mbrList) {
//                System.out.println("x1 of bound is: " +
//                        (int) (unionPoints.getX1() * fix - tree.getRoot().getMBR().getX1() * fix) +
//                        "; y1 of bound is: " +
//                        (int) (unionPoints.getY1() * fix - tree.getRoot().getMBR().getY1() * fix) +
//                        "; width of x is: " +
//                        (int) (unionPoints.getX2() * fix - unionPoints.getX1() * fix) +
//                        "; length of x is: " +
//                        (int) (unionPoints.getY2() * fix - unionPoints.getY1() * fix)
//                );
                g.drawRect((int) (mbr.getX1() * fix - tree.getRoot().getMBR().getX1() * fix), (int) (mbr.getY1() * fix - tree.getRoot().getMBR().getY1() * fix),
                        (int) (mbr.getX2() * fix - mbr.getX1() * fix), (int) (mbr.getY2() * fix - mbr.getY1() * fix));
            }
        }
        g.setColor(iterateColor(0));
        ArrayList<DataObject> dataObjects = tree.getDataObjects();
        if (dataObjects.get(0) instanceof Point) {
            for (DataObject dataObject : dataObjects) {
                Point point = (Point) dataObject;
//                g.fillOval((int) (point.getX() * fix - tree.getRoot().getMBR().getX1() * fix), (int) (point.getY() * fix - tree.getRoot().getMBR().getY1() * fix), 2, 2);
            }
        } else if (dataObjects.get(0) instanceof Line) {

        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(RECT_WIDTH + 2 * RECT_X, RECT_HEIGHT + 2 * RECT_Y);
    }

    /**
     * Select color according to height of nodes.
     * @param i height of nodes
     * @return Color
     */
    private Color iterateColor(int i){
        switch (i % 6){
            case 0:
                return Color.red;
            case 1:
                return Color.blue;
            case 2:
                return Color.green;
            case 3:
                return Color.gray;
            case 4:
                return Color.black;
            case 5:
                return Color.yellow;
        }
        return Color.red;
    }

    /**
     * Create the GUI
     * @param tree r-tree object
     */
    public static void createAndShowGui(RTree tree) {
        Visualization visualization = new Visualization(tree);

        JFrame frame = new JFrame("Visualization of MBRs");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(visualization);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }
}
