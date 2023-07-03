/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class AzureFacet extends Facet<AzureFacetConfiguration> {

    public AzureFacet(@Nonnull FacetType facetType, @Nonnull Module module, @Nonnull String name, @Nonnull AzureFacetConfiguration configuration, Facet underlyingFacet) {
        super(facetType, module, name, configuration, underlyingFacet);
        if (configuration.getDotAzurePath() == null) {
            Optional.of(module).map(ProjectUtil::guessModuleDir).map(d -> Path.of(d.getPath(), ".azure")).ifPresent(configuration::setDotAzureDir);
        }
    }

    public Path getDotAzurePath() {
        return this.getConfiguration().getDotAzurePath();
    }

    public static void addTo(@Nonnull final Module module) {
        final AzureFacet facet = getInstance(module);
        final PropertiesComponent properties = PropertiesComponent.getInstance(module.getProject());
        final String key = getFacetFlag(module);
        if (Objects.isNull(facet)) {
            FacetManager.getInstance(module).addFacet(AzureFacetType.INSTANCE, "Azure", null);
            properties.setValue(key, true);
            ProjectView.getInstance(module.getProject()).getCurrentProjectViewPane().updateFromRoot(true);
        }
        ProjectView.getInstance(module.getProject()).getCurrentProjectViewPane().updateFromRoot(true);
    }

    public static boolean wasEverAddedTo(@Nonnull final Module module) {
        final AzureFacet facet = getInstance(module);
        final PropertiesComponent properties = PropertiesComponent.getInstance(module.getProject());
        final String key = getFacetFlag(module);
        return properties.isValueSet(key);
    }

    @Nullable
    public static AzureFacet getInstance(@Nonnull final Module module) {
        if (module.isDisposed()) {
            return null;
        }
        return FacetManager.getInstance(module).getFacetByType(AzureFacetType.ID);
    }

    @Nullable
    public static AzureFacet getInstance(@Nonnull VirtualFile file, @Nonnull Project project) {
        final Module module = ModuleUtil.findModuleForFile(file, project);
        return Objects.isNull(module) || module.isDisposed() ? null : getInstance(module);
    }

    @Nullable
    public static AzureFacet getInstance(RunConfiguration configuration) {
        return Optional.ofNullable(configuration)
            .map(AzureModule::getTargetModule)
            .map(AzureFacet::getInstance).orElse(null);
    }

    @Nonnull
    private static String getFacetFlag(@Nonnull Module module) {
        return module.getName() + ".wasFacetEverAdded";
    }
}
