/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.util.Disposer;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class DeploymentTargetsNode extends AbstractTreeNode<AzureModule> implements IAzureFacetNode {
    @Getter
    @Setter
    private boolean disposed;

    public DeploymentTargetsNode(@Nonnull AzureFacetRootNode parent) {
        super(parent.getProject(), parent.getValue());
        if (!parent.isDisposed()) {
            Disposer.register(parent, this);
        }
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        Disposer.disposeChildren(this, ignore -> true);
        final AzureModule module = Objects.requireNonNull(this.getValue());
        try {
            return Optional.of(module).stream()
                .map(AzureModule::getDefaultProfile).filter(Objects::nonNull)
                .flatMap(p -> p.getTargetAppIds().stream())
                .map(id -> Azure.az().getById(id))
                .filter(Objects::nonNull)
                .map(this::toNode)
                .toList();
        } catch (Throwable e) {
            e.printStackTrace();
            e = ExceptionUtils.getRootCause(e);
            final ArrayList<AbstractTreeNode<?>> children = new ArrayList<>();
            if (e instanceof AzureToolkitAuthenticationException) {
                final Action<Object> signin = AzureActionManager.getInstance().getAction(Action.AUTHENTICATE).bind(this.getValue()).withLabel("Sign in to manage the deployment targets.");
                children.add(new ActionNode<>(this, signin));
            } else {
                children.add(new ExceptionNode(this, e));
            }
            return children;
        }
    }

    private AbstractTreeNode<?> toNode(@Nonnull final AbstractAzResource<?, ?, ?> app) {
        final Node<?> node = AzureExplorer.manager.createNode(app, null, IExplorerNodeProvider.ViewType.APP_CENTRIC);
        return new ResourceNode(this, node);
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        presentation.setIcon(AllIcons.Nodes.Deploy);
        presentation.setPresentableText("Deployment Targets");
        presentation.setTooltip("The Azure services that this project was deployed to.");
    }

    @Override
    public int getWeight() {
        return DEFAULT_WEIGHT - 1;
    }

    @Override
    @Nullable
    public Object getData(@Nonnull String dataId) {
        return StringUtils.equalsIgnoreCase(dataId, Action.SOURCE) ? this.getValue() : null;
    }

    @Override
    public String toString() {
        return "Deployment Targets";
    }
}