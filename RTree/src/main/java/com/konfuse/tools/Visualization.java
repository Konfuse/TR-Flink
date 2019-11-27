package com.konfuse.tools;

import com.konfuse.RTree;
import com.konfuse.bean.MBR;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Stack;

/**
 * @Author: Konfuse
 * @Date: 2019/11/27 14:46
 */
public class Visualization extends JComponent {
    private static final int RECT_X = 400;
    private static final int RECT_Y = 400;
    private static final int RECT_WIDTH = 400;
    private static final int RECT_HEIGHT = 400;
    private static Stack<ArrayList<MBR>> stack = new Stack<ArrayList<MBR>>();
    
    private RTree tree;

    public Visualization(RTree tree) {
        this.tree = tree;
        int level = tree.getRoot().getHeight();
        for(int i = 0 ; i <= level ; i++ ){
            stack.add(tree.getMBRs(i));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int height = tree.getRoot().getHeight();
        for (int i = 0; i <= height; i++) {
            ArrayList<MBR> mbrList = tree.getMBRs(i);
            g.setColor(iterateColor(i));
            for (MBR mbr : mbrList) {
                System.out.println("x1 of bound is: " +
                        (int) (mbr.getX1() * 10000 - tree.getRoot().getMBR().getX1() * 10000) +
                        "; y1 of bound is: " +
                        (int) (mbr.getY1() * 10000 - tree.getRoot().getMBR().getY1() * 10000) +
                        "; width of x is: " +
                        (int) (mbr.getX2() * 10000 - mbr.getX1() * 10000) +
                        "; length of x is: " +
                        (int) (mbr.getY2() * 10000 - mbr.getY1() * 10000)
                );
                g.drawRect((int) (mbr.getX1() * 10000 - tree.getRoot().getMBR().getX1() * 10000), (int) (mbr.getY1() * 10000 - tree.getRoot().getMBR().getY1() * 10000),
                        (int) (mbr.getX2() * 10000 - mbr.getX1() * 10000), (int) (mbr.getY2() * 10000 - mbr.getY1() * 10000));
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(RECT_WIDTH + 2 * RECT_X, RECT_HEIGHT + 2 * RECT_Y);
    }

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

    // create the GUI
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
