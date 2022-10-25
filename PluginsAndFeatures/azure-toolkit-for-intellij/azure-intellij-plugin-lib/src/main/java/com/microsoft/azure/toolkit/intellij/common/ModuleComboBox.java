/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ModuleComboBox extends AzureComboBox<Module> {

    private final Project project;

    public ModuleComboBox(Project project) {
        super(true);
        this.project = project;
    }

    @Nullable
    @Override
    protected Module doGetDefaultValue() {
        return CacheManager.getUsageHistory(Module.class)
            .peek(v -> Objects.isNull(project) || Objects.equals(project, v.getProject()));
    }

    @Override
    protected List<? extends Module> loadItems() throws Exception {
        return Arrays.asList(ModuleManager.getInstance(project).getModules());
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Module) {
            return ((Module) item).getName();
        } else {
            return StringUtils.EMPTY;
        }
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }
}
