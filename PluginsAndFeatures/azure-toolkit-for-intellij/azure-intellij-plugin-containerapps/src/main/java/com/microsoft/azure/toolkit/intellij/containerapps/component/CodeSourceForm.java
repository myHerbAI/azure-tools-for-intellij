/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.intellij.common.component.AzureFileInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import lombok.Getter;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CodeSourceForm implements AzureFormJPanel<ContainerAppDraft.ImageConfig> {
    private static final String LINK_SUPPORTED_JAVA_BUILD_ENV = "https://learn.microsoft.com/en-us/azure/container-apps/java-build-environment-variables?source=recommendations#supported-java-build-environment-variables";
    private final Project project;
    @Getter
    private JPanel contentPanel;
    private JLabel lblCode;
    private AzureFileInput fileCode;
    private EnvironmentVariablesTextFieldWithBrowseButton inputEnv;
    private HyperlinkLabel buildEnvLink;

    public CodeSourceForm(final Project project) {
        super();
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        this.fileCode.setLabel("Code");
        this.fileCode.setRequired(true);
        this.fileCode.addValidator(this::validateCodePath);
        this.lblCode.setLabelFor(fileCode);
        this.lblCode.setIcon(AllIcons.General.ContextHelp);
        buildEnvLink.setHtmlText(String.format("<html><a href=\"%s\">Supported Java build environment variables</a></html>", LINK_SUPPORTED_JAVA_BUILD_ENV));
        buildEnvLink.addHyperlinkListener((e) -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(LINK_SUPPORTED_JAVA_BUILD_ENV);
            }
        });
        buildEnvLink.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    @Override
    public ContainerAppDraft.ImageConfig getValue() {
        //TODO: generate full image name
        final String fullImageName = "";
        final ContainerAppDraft.ImageConfig config = new ContainerAppDraft.ImageConfig(fullImageName);
        Optional.ofNullable(fileCode.getValue())
            .map(File::new)
            .filter(File::exists)
            .ifPresent(config::setSource);

        final Map<String, String> envVarsMap = this.inputEnv.getEnvironmentVariables();
        config.setSourceBuildEnv(envVarsMap);

        return config;
    }

    @Override
    public void setValue(final ContainerAppDraft.ImageConfig config) {
        Optional.ofNullable(config.getSource())
            .filter(File::exists)
            .map(File::getPath)
            .ifPresent(fileCode::setValue);
        Optional.ofNullable(config.getSourceBuildEnv())
            .ifPresent(this.inputEnv::setEnvironmentVariables);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.fileCode);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
    }

    private void createUIComponents() {
        this.fileCode = new AzureFileInput();
        this.fileCode.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>("Select Path of Source Code", null, fileCode,
            this.project, FileChooserDescriptorFactory.createSingleFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));
    }

    private AzureValidationInfo validateCodePath() {
        final String path = this.fileCode.getValue();
        if (!FileUtil.exists(path)) {
            return AzureValidationInfo.error("Directory does not exist", this.fileCode);
        }
        if (!FileUtils.isDirectory(new File(path))) {
            return AzureValidationInfo.error("Not a directory", this.fileCode);
        }
        return AzureValidationInfo.ok(this.fileCode);
    }
}
