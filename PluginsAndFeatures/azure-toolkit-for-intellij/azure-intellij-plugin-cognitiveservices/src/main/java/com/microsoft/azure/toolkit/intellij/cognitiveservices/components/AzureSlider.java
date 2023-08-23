package com.microsoft.azure.toolkit.intellij.cognitiveservices.components;

import lombok.Getter;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Hashtable;

public class AzureSlider {
    private final double minimum;
    private final double maximum;
    private final double stepSize;
    private final double defaultValue;
    private final int magnification;
    @Getter
    private JPanel pnlRoot;
    private JSlider numSlider;
    private JSpinner numValue;
    private int realMin = Integer.MIN_VALUE;

    public AzureSlider(final double value, final double minimum, final double maximum, final double stepSize, final int magnification) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.stepSize = stepSize;
        this.defaultValue = value;
        this.magnification = magnification;
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        this.numSlider.setMinimum((int) (this.minimum * this.magnification));
        this.numSlider.setMaximum((int) (this.maximum * this.magnification));
        this.numSlider.setMinorTickSpacing((int) (maximum - minimum) * magnification / 40);
        this.numSlider.setMajorTickSpacing((int) (maximum - minimum) * magnification / 4);
        final Hashtable<Integer, JComponent> labels = this.createLabels(numSlider);
        this.numSlider.setLabelTable(labels);
        this.numSlider.addChangeListener(e -> this.setValue(this.numSlider.getValue() * 1.0 / this.magnification));

        this.numValue.setModel(new SpinnerNumberModel(defaultValue, minimum, maximum, stepSize));
        this.numValue.addChangeListener(e -> this.setValue((double) this.numValue.getValue()));
    }

    public void addChangeListener(ChangeListener l) {
        this.numSlider.addChangeListener(l);
    }

    public void setValue(double value) {
        final double val = Math.max(this.realMin, value);
        this.numSlider.setValue((int) (val * this.magnification));
        this.numValue.setValue(val);
    }

    public double getValue() {
        return (double) this.numValue.getValue();
    }

    private Hashtable<Integer, JComponent> createLabels(@Nonnull final JSlider numSlider) {
        final Hashtable<Integer, JComponent> result = new Hashtable<>();
        final double step = (this.maximum - this.minimum) / 4;
        for (double value = this.minimum; value <= this.maximum; value += step) {
            final JLabel label = new JLabel(String.valueOf(value));
            label.setPreferredSize(new Dimension(50, 24));
            label.setHorizontalAlignment(SwingConstants.TRAILING);
            result.put((int) (value * this.magnification), label);
        }
        return result;
    }

    public void setEnabled(boolean isEnabled) {
        this.numSlider.setEnabled(isEnabled);
        this.numValue.setEnabled(isEnabled);
    }

    private JComponent $$$getRootComponent$$$() {
        return pnlRoot;
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
