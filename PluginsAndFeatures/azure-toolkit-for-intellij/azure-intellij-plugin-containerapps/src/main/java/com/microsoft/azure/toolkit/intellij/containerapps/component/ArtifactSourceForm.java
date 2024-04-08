/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import lombok.Getter;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArtifactSourceForm implements AzureFormJPanel<ContainerAppDraft.ImageConfig> {
    private static final String LINK_SUPPORTED_JAVA_BUILD_ENV = "https://learn.microsoft.com/en-us/azure/container-apps/java-build-environment-variables?source=recommendations#supported-java-build-environment-variables";
    private final Project project;
    @Getter
    private JPanel contentPanel;
    private JLabel lblArtifact;
    private AzureArtifactComboBox selectorArtifact;
    private EnvironmentVariablesTextFieldWithBrowseButton inputEnv;
    private HyperlinkLabel buildEnvLink;

    public ArtifactSourceForm(final Project project) {
        super();
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        this.selectorArtifact.setLabel("Artifact");
        this.selectorArtifact.setRequired(true);
        this.selectorArtifact.setFileFilter(virtualFile -> {
            final String ext = FileNameUtils.getExtension(virtualFile.getPath());
            return StringUtils.equalsAnyIgnoreCase(ext, "jar", "war");
        });
        this.lblArtifact.setLabelFor(selectorArtifact);
        this.lblArtifact.setIcon(AllIcons.General.ContextHelp);
        buildEnvLink.setHtmlText(String.format("<html><a href=\"%s\">Supported Java build environment variables</a></html>", LINK_SUPPORTED_JAVA_BUILD_ENV));
        buildEnvLink.setIcon(AllIcons.General.Information);
        buildEnvLink.addHyperlinkListener(new BrowserHyperlinkListener());
        buildEnvLink.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    @Override
    public ContainerAppDraft.ImageConfig getValue() {
        //TODO: generate full image name
        final String fullImageName = "";
        final ContainerAppDraft.ImageConfig config = new ContainerAppDraft.ImageConfig(fullImageName);
        Optional.ofNullable(selectorArtifact.getValue()).map(AzureArtifact::getFileForDeployment)
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
            .map(f -> AzureArtifact.createFromFile(f.getPath(), project))
            .ifPresent(selectorArtifact::setValue);
        Optional.ofNullable(config.getSourceBuildEnv())
            .ifPresent(this.inputEnv::setEnvironmentVariables);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.selectorArtifact);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
    }

    private void createUIComponents() {
        this.selectorArtifact = new AzureArtifactComboBox(project, true);
        this.selectorArtifact.reloadItems();
    }
}
