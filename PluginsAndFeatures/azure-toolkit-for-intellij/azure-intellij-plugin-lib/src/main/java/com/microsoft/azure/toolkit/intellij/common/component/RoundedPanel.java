/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class RoundedPanel extends JPanel {
    private int cornerRadius = 5;

    public RoundedPanel(LayoutManager layout) {
        super(layout);
    }

    public RoundedPanel(LayoutManager layout, int radius) {
        super(layout);
        cornerRadius = radius;
    }

    public RoundedPanel(int radius) {
        super();
        cornerRadius = radius;
    }

    public void paint(Graphics g) {
        final int fieldX = 0;
        final int fieldY = 0;
        final int fieldWeight = getSize().width;
        final int fieldHeight = getSize().height;
        final RoundRectangle2D rect = new RoundRectangle2D.Double(fieldX, fieldY, fieldWeight, fieldHeight, cornerRadius, cornerRadius);
        g.setClip(rect);
        super.paint(g);
    }

//    @Override
//    protected void paintComponent(Graphics g) {
//        super.paintComponent(g);
//        final Dimension arcs = new Dimension(cornerRadius, cornerRadius);
//        final int width = getWidth();
//        final int height = getHeight();
//        final Graphics2D graphics = (Graphics2D) g;
//        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//        //Draws the rounded panel with borders.
//        graphics.setColor(getBackground());
//        graphics.fillRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height); //paint background
////        graphics.setColor(getForeground());
//        graphics.drawRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height); //paint border
//    }
}