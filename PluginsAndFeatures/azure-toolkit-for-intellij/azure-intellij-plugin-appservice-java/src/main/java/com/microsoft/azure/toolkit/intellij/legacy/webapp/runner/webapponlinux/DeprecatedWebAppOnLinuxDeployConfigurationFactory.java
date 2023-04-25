/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DeprecatedWebAppOnLinuxDeployConfigurationFactory extends ConfigurationFactory {
    private static final String FACTORY_NAME = "Web App for Containers";

    public DeprecatedWebAppOnLinuxDeployConfigurationFactory(ConfigurationType configurationType) {
        super(configurationType);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new WebAppOnLinuxDeployConfiguration(project, this, String.format("%s: %s", FACTORY_NAME, project
                .getName()));
    }

    @Override
    public String getName() {
        return "Web App for Containers (Deprecated)";
    }

    @Override
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
        return new WebAppOnLinuxDeployConfiguration(template.getProject(), this, name);
    }

    @Override
    public Icon getIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.DockerSupport.RUN_ON_WEB_APP);
    }

    @Override
    public @NotNull @NonNls String getId() {
        return FACTORY_NAME;
    }
}
