/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.DeploymentTargetManager;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class DeploymentTargetsNode extends AbstractAzureFacetNode<DeploymentTargetManager> {

    public DeploymentTargetsNode(@Nonnull Project project, @Nonnull DeploymentTargetManager manager) {
        super(project, manager);
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractAzureFacetNode<?>> buildChildren() {
        return Optional.ofNullable(this.getValue()).stream()
            .flatMap(p -> p.getTargets().stream())
            .map(id -> Azure.az().getById(id))
            .filter(Objects::nonNull)
            .map(this::createResourceNode)
            .toList();
    }

    @Override
    protected void buildView(@Nonnull final PresentationData presentation) {
        presentation.setIcon(AllIcons.Nodes.Deploy);
        presentation.setPresentableText("Deployment Targets");
        presentation.setTooltip("The Azure services that this project was deployed to.");
    }

    private AbstractAzureFacetNode<?> createResourceNode(@Nonnull final AbstractAzResource<?, ?, ?> app) {
        final Node<?> node = AzureExplorer.manager.createNode(app, null, IExplorerNodeProvider.ViewType.APP_CENTRIC);
        return new ResourceNode(this.getProject(), node);
    }

    @Override
    public int getWeight() {
        return DEFAULT_WEIGHT - 1;
    }

    @Override
    public String toString() {
        return "Deployment Targets";
    }

    @Override
    public @Nonnull LeafState getLeafState() {
        return LeafState.NEVER;
    }
}