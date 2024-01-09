/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.ide.appservice.model.MonitorConfig;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.insights.ApplicationInsightsComboBox;
import com.microsoft.azure.toolkit.lib.appservice.model.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AppServiceMonitorPanel extends JPanel implements AzureFormPanel<MonitorConfig> {
    private JPanel pnlRoot;
    private TitledSeparator titleApplicationInsights;
    private TitledSeparator titleAppServiceLog;
    private ApplicationInsightsPanel applicationInsightsPanel;
    private AppServiceLogsPanel appServiceLogsPanel;

    private final Project project;

    public AppServiceMonitorPanel(final Project project) {
        super();
        this.project = project;
    }

    public void setApplicationInsightsVisible(boolean visible) {
        titleApplicationInsights.setVisible(visible);
        applicationInsightsPanel.setVisible(visible);
    }

    public void setApplicationLogVisible(boolean visible) {
        appServiceLogsPanel.setApplicationLogVisible(visible);
    }

    public void setWebServerLogVisible(boolean enable) {
        appServiceLogsPanel.setWebServerLogVisible(enable);
    }

    @Override
    public MonitorConfig getValue() {
        final ApplicationInsightsConfig insightsConfig = applicationInsightsPanel.getValue();
        final DiagnosticConfig diagnosticConfig = appServiceLogsPanel.getValue();
        return MonitorConfig.builder().applicationInsightsConfig(insightsConfig).diagnosticConfig(diagnosticConfig).build();
    }

    @Override
    public void setValue(final MonitorConfig data) {
        Optional.ofNullable(data.getDiagnosticConfig()).ifPresent(appServiceLogsPanel::setValue);
        Optional.ofNullable(data.getApplicationInsightsConfig()).ifPresent(applicationInsightsPanel::setValue);
    }

    public ApplicationInsightsComboBox getApplicationInsightsComboBox() {
        return applicationInsightsPanel.getApplicationInsightsComboBox();
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(applicationInsightsPanel, appServiceLogsPanel);
    }
}
