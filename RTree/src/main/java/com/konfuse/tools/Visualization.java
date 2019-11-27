package com.konfuse.tools;

import com.konfuse.RTree;
import com.konfuse.bean.MBR;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

/**
 * @Author: Konfuse
 * @Date: 2019/11/27 14:46
 */
public class Visualization extends JComponent {
    private static final int RECT_X = 400;
    private static final int RECT_Y = RECT_X;
    private static final int RECT_WIDTH = 400;
    private static final int RECT_HEIGHT = RECT_WIDTH;
    
    private RTree tree;

    public Visualization(RTree tree) {
        this.tree = tree;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int i = 0; i <= tree.getRoot().getHeight(); i++) {
            ArrayList<MBR> mbrList = tree.getMBRs(i);
            g.setColor(iterateColor(i));
            for (MBR mbr : mbrList) {
                g.drawRect((int) mbr.getX1() * 20, (int) mbr.getY1() * 20, (int) (mbr.getX2() * 20 - mbr.getX1() * 20), (int) (mbr.getY2() * 20 - mbr.getY1() * 20));
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
