/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.function;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.intellij.appservice.DockerUtils;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.containerapps.component.ImageForm;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTable;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTableUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionAppUpdateImageDialog extends AzureDialog<FunctionAppConfig> implements AzureFormPanel<FunctionAppConfig> {
    private JPanel pnlRoot;
    private JLabel lblFunction;
    private JLabel lblAppSettings;
    private JPanel pnlAppSettings;
    private JPanel pnlImageContainer;
    private ImageForm imageForm;
    private AzureComboBox<FunctionApp> functionAppComboBox;
    private TitledSeparator titleApp;
    private TitledSeparator titleImage;
    private TitledSeparator titleAppSettings;
    private FunctionAppSettingsTable appSettingsTable;

    public FunctionAppUpdateImageDialog(final Project project) {
        super(project);
        this.init();
    }

    @Override
    public AzureForm<FunctionAppConfig> getForm() {
        return this;
    }

    @Nonnull
    @Override
    protected String getDialogTitle() {
        return "Update Image";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    protected void init() {
        super.init();
        this.functionAppComboBox.addValueChangedListener(app -> this.appSettingsTable.loadAppSettings(app::getAppSettings));
    }

    @Override
    public void setValue(@Nullable FunctionAppConfig data) {
        Optional.ofNullable(data)
                .map(d -> Azure.az(AzureFunctions.class).functionApps(d.subscriptionId()).get(d.appName(), d.resourceGroup()))
                .ifPresent(functionAppComboBox::setValue);
        Optional.ofNullable(data).map(FunctionAppConfig::runtime)
                .map(DockerUtils::convertRuntimeConfigToImageConfig).ifPresent(imageForm::setValue);
        Optional.ofNullable(data).map(FunctionAppConfig::appSettings).ifPresent(appSettingsTable::setAppSettings);
    }

    public void setFunctionApp(@Nullable FunctionApp functionApp) {
        Optional.ofNullable(functionApp).ifPresent(functionAppComboBox::setValue);
    }

    public void setImage(@Nullable ContainerAppDraft.ImageConfig imageConfig) {
        Optional.ofNullable(imageConfig).ifPresent(imageForm::setValue);
    }

    @Nullable
    @Override
    public FunctionAppConfig getValue() {
        final FunctionApp app = functionAppComboBox.getValue();
        if (Objects.isNull(app)) {
            return null;
        }
        final FunctionAppConfig config = AppServiceConfigUtils.fromFunctionApp(app);
        config.appSettings(appSettingsTable.getAppSettings());
        Optional.ofNullable(imageForm.getValue()).map(DockerUtils::convertImageConfigToRuntimeConfig).ifPresent(config::runtime);
        return config;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(functionAppComboBox, imageForm);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.pnlImageContainer = new JPanel(new GridLayoutManager(1, 1));
        this.imageForm = new ImageForm();
        this.pnlImageContainer.add(this.imageForm.getContentPanel(), new GridConstraints(0, 0, 1, 1, 0,
                GridConstraints.FILL_BOTH, 7, 7, null, null, null, 0));

        this.appSettingsTable = new FunctionAppSettingsTable();
        this.appSettingsTable.setProject(getProject());
        this.pnlAppSettings = FunctionAppSettingsTableUtils.createAppSettingPanel(appSettingsTable);

        this.functionAppComboBox = new AzureComboBox<>(true) {
            @Override
            protected List<? extends FunctionApp> loadItems() {
                return Azure.az(AzureFunctions.class).functionApps().stream()
                        .filter(app -> Objects.nonNull(app.getRemote()))
                        .filter(app -> StringUtils.startsWithIgnoreCase(app.getRemote().linuxFxVersion(), "docker"))
                        .collect(Collectors.toList());
            }

            @Override
            protected String getItemText(final Object item) {
                return item instanceof FunctionApp app ? app.getName() : super.getItemText(item);
            }
        };
        this.functionAppComboBox.reloadItems();
    }
}
