package com.microsoft.azure.toolkit.ide.guidance.view;

import com.intellij.ui.JBColor;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class ViewUtils {
    public static final JBColor NOTIFICATION_BACKGROUND_COLOR =
            JBColor.namedColor("StatusBar.hoverBackground", new JBColor(15595004, 4606541));

    public static void setBackgroundColor(@Nonnull final JPanel c, @Nonnull final Color color) {
        c.setBackground(color);
        Arrays.stream(c.getComponents()).filter(component -> component instanceof JPanel).forEach(child -> setBackgroundColor((JPanel) child, color));
        Arrays.stream(c.getComponents()).filter(component -> component instanceof JTextPane || component instanceof JButton).forEach(child -> child.setBackground(color));
    }
}
