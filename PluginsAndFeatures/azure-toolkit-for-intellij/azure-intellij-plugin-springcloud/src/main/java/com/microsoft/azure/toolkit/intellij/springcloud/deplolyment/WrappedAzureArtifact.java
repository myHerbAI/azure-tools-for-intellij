/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;

public class WrappedAzureArtifact implements IArtifact {
    @Getter
    private final AzureArtifact artifact;
    private final Project project;

    public WrappedAzureArtifact(@Nonnull final AzureArtifact artifact, @Nonnull Project project) {
        this.artifact = artifact;
        this.project = project;
    }

    @Nullable
    @Override
    public File getFile() {
        final AzureArtifactManager manager = AzureArtifactManager.getInstance(this.project);
        return Optional.ofNullable(artifact.getFileForDeployment()).map(File::new).orElse(null);
    }
}
