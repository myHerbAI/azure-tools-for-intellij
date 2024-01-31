/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceInfoAdvancedPanel;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceLogsPanel;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;

import javax.swing.*;
import java.util.List;
import java.util.Optional;

public class WebAppConfigFormPanelAdvance implements AzureFormPanel<AppServiceConfig> {
    private final Project project;
    private JTabbedPane tabPane;
    private JPanel pnlRoot;
    private AppServiceInfoAdvancedPanel<AppServiceConfig> appServiceConfigPanelAdvanced;
    private JPanel pnlMonitoring;
    private JPanel pnlAppService;
    private TitledSeparator titleAppServiceLog;
    private AppServiceLogsPanel appServiceLogsPanel;

    public WebAppConfigFormPanelAdvance(final Project project) {
        super();
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        // Application Insights is not supported in Web App
        appServiceLogsPanel.setApplicationLogVisible(false);

        appServiceConfigPanelAdvanced.getSelectorRuntime().addActionListener(event -> {
            final Runtime runtime = appServiceConfigPanelAdvanced.getSelectorRuntime().getValue();
            appServiceLogsPanel.setApplicationLogVisible(runtime != null && runtime.getOperatingSystem() == OperatingSystem.WINDOWS);
        });
    }

    @Override
    public void setVisible(final boolean visible) {
        pnlRoot.setVisible(visible);
    }

    @Override
    public AppServiceConfig getValue() {
        final AppServiceConfig data = appServiceConfigPanelAdvanced.getValue();
        Optional.ofNullable(data).ifPresent(config -> config.diagnosticConfig(appServiceLogsPanel.getValue()));
        return data;
    }

    @Override
    public void setValue(final AppServiceConfig data) {
        Optional.ofNullable(data).ifPresent(appServiceConfigPanelAdvanced::setValue);
        Optional.ofNullable(data).map(AppServiceConfig::diagnosticConfig).ifPresent(appServiceLogsPanel::setValue);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return ListUtils.union(appServiceConfigPanelAdvanced.getInputs(), appServiceLogsPanel.getInputs());
    }

    public void setDeploymentVisible(final boolean visible) {
        this.appServiceConfigPanelAdvanced.setDeploymentVisible(visible);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        appServiceConfigPanelAdvanced = new AppServiceInfoAdvancedPanel<>(project, AppServiceConfig::new);
    }

    public void setFixedRuntime(final Runtime runtime) {
        this.appServiceConfigPanelAdvanced.setFixedRuntime(runtime);
    }
}
