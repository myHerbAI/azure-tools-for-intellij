/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.settings;

import com.intellij.application.options.OptionsContainingConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DeprecatedAzureSettingsConfigurable implements SearchableConfigurable, Configurable.NoScroll, OptionsContainingConfigurable {
    private Project project;

    public DeprecatedAzureSettingsConfigurable(final Project project) {
        this.project = project;
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "com.microsoft.intellij.deprecated";
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Microsoft Tools";
    }

    @Override
    public @Nullable JComponent createComponent() {
        return new DeprecatedAzureSettingsPanel(project).getPnlRoot();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }
}
