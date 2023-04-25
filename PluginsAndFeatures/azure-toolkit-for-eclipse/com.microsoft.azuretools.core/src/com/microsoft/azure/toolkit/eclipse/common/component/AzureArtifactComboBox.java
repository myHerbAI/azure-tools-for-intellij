/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.component;

import com.microsoft.azure.toolkit.eclipse.common.artifact.AzureArtifact;
import com.microsoft.azure.toolkit.eclipse.common.artifact.AzureArtifactManager;
import com.microsoft.azure.toolkit.eclipse.common.artifact.AzureArtifactType;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class AzureArtifactComboBox extends AzureComboBox<AzureArtifact> {
    private AzureArtifact cachedArtifact;

    public AzureArtifactComboBox(Composite parent) {
        super(parent);
    }

    @Nonnull
    @Override
    @AzureOperation( name = "internal/common.list_artifacts")
    protected List<? extends AzureArtifact> loadItems() {
        final List<AzureArtifact> artifacts = AzureArtifactManager.getInstance().getAllSupportedAzureArtifacts();
        Optional.ofNullable(cachedArtifact).filter(artifact -> artifact.getType() == AzureArtifactType.File).ifPresent(artifacts::add);
        return artifacts;
    }

    protected String getItemText(Object item) {
        if (item instanceof AzureArtifact) {
            return String.format("%s : %s", ((AzureArtifact) item).getType(), ((AzureArtifact) item).getName());
        } else {
            return StringUtils.EMPTY;
        }
    }

    @Nullable
    protected Image getItemIcon(Object item) {
        return item instanceof AzureArtifact ? ((AzureArtifact) item).getIcon() : null;
    }

}
