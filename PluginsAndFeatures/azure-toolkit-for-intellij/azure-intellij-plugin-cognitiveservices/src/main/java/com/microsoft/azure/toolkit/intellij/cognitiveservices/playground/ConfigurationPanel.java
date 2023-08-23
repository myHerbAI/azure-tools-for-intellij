package com.microsoft.azure.toolkit.intellij.cognitiveservices.playground;

import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.AzureSlider;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.Configuration;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class ConfigurationPanel implements AzureForm<Configuration> {
    private JLabel lblMaxResponse;
    private JLabel lblTemperature;
    private JLabel lblStopSequence;
    private AzureTextInput txtStopSequence;
    private JPanel pnlRoot;
    private AzureSlider sliderTopN;
    private AzureSlider sliderTemperature;
    private AzureSlider sliderMaxResponse;
    private AzureSlider sliderFrequency;
    private AzureSlider sliderPresence;
    private JLabel lblTopP;
    private JLabel lblFrequency;
    private JLabel lblPresence;

    public ConfigurationPanel() {
        $$$setupUI$$$();
        this.lblFrequency.setBorder(JBUI.Borders.empty(6, 0));
        this.lblPresence.setBorder(JBUI.Borders.empty(6, 0));
        this.lblMaxResponse.setBorder(JBUI.Borders.empty(6, 0));
        this.lblTemperature.setBorder(JBUI.Borders.empty(6, 0));
        this.lblTopP.setBorder(JBUI.Borders.empty(6, 0));
        this.lblStopSequence.setBorder(JBUI.Borders.empty(6, 0));
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.sliderMaxResponse = new AzureSlider(1000, 0, 4000, 40, 1);
        this.sliderTemperature = new AzureSlider(0.7, 0, 1, 0.01, 100);
        this.sliderTopN = new AzureSlider(0.95, 0, 1, 0.01, 100);
        this.sliderFrequency = new AzureSlider(0, 0, 2, 0.01, 100);
        this.sliderPresence = new AzureSlider(0, 0, 2, 0.01, 100);
    }

    @Override
    public Configuration getValue() {
        return Configuration.builder()
                .maxResponse((int) sliderMaxResponse.getValue())
                .temperature(sliderTemperature.getValue())
                .topP(sliderTopN.getValue())
                .stopSequences(txtStopSequence.getValue())
                .frequencyPenalty(sliderFrequency.getValue())
                .presencePenalty(sliderPresence.getValue())
                .build();
    }

    @Override
    public void setValue(@Nonnull Configuration data) {
        this.sliderMaxResponse.setValue(data.getMaxResponse());
        this.sliderTemperature.setValue(data.getTemperature());
        this.sliderTopN.setValue(data.getTopP());
        this.txtStopSequence.setValue(data.getStopSequences());
        this.sliderFrequency.setValue(data.getFrequencyPenalty());
        this.sliderPresence.setValue(data.getPresencePenalty());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.emptyList();
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
