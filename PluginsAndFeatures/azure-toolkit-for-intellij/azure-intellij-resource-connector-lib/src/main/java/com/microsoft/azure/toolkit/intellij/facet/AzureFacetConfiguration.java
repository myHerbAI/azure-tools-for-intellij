/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;

public class AzureFacetConfiguration implements FacetConfiguration, PersistentStateComponent<AzureFacetConfiguration.AzureFacetState> {
    @Getter
    @Nonnull
    private AzureFacetState state;

    public AzureFacetConfiguration() {
        this.state = new AzureFacetState();
    }

    public AzureFacetConfiguration(@Nonnull final VirtualFile dotAzureFile) {
        this.state = new AzureFacetState(dotAzureFile.toNioPath().toString());
    }

    @Nonnull
    @Override
    public AzureFacetState getState() {
        return state;
    }

    @Override
    public void loadState(@Nonnull AzureFacetState state) {
        this.state = state;
    }

    @Nullable
    public Path getDotAzurePath() {
        return Optional.ofNullable(this.state.dotAzurePath)
            .filter(StringUtils::isNotBlank).map(Path::of)
            .orElse(null);
    }

    public void setDotAzureDir(final Path dotAzureDir) {
        this.state.setDotAzurePath(Optional.ofNullable(dotAzureDir).map(Path::toString).orElse(null));
    }

    @Override
    public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext, FacetValidatorsManager validatorsManager) {
        return new FacetEditorTab[]{
            new AzureFacetEditorTab(this.state, editorContext, validatorsManager)
        };
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AzureFacetState {
        @Nullable
        private String dotAzurePath;
    }
}
