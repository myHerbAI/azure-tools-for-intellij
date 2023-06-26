/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor.EDIT_CONNECTION;
import static com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor.REMOVE_CONNECTION;

public class ConnectionNode extends AbstractTreeNode<Connection<?, ?>> implements IAzureFacetNode {
    private final AzureModule module;
    private final Action<?> editAction;

    public ConnectionNode(@Nonnull Project project, @Nonnull AzureModule module, @Nonnull final Connection<?, ?> connection) {
        super(project, connection);
        this.module = module;
        this.editAction = new Action<>(Action.Id.of("user/connector.edit_connection_in_editor"))
                .withLabel("Open In Editor")
                .withIcon(AzureIcons.Action.EDIT.getIconPath())
                .withHandler(ignore -> AzureTaskManager.getInstance().runLater(() -> this.navigate(true)))
                .withAuthRequired(false);
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final Connection<?, ?> connection = this.getValue();
        if (!connection.validate(getProject())) {
            return Collections.singletonList(new ActionNode<>(this.myProject, ResourceConnectionActionsContributor.FIX_CONNECTION, connection));
        }
        final ArrayList<AbstractTreeNode<?>> children = new ArrayList<>();
        if (connection.getResource() instanceof AzureServiceResource) {
            final AbstractTreeNode<?> resourceNode = getResourceNode(connection);
            children.add(resourceNode);
        }
        final Profile profile = Objects.requireNonNull(module.getDefaultProfile());
        final EnvironmentVariablesNode environmentVariablesNode = new EnvironmentVariablesNode(this.getProject(), profile, connection);
        children.add(environmentVariablesNode);
        return children;
    }

    private AbstractTreeNode<?> getResourceNode(Connection<?, ?> connection) {
        try {
            final Object resource = connection.getResource().getData();
            final Node<?> node = AzureExplorer.manager.createNode(resource, null, IExplorerNodeProvider.ViewType.APP_CENTRIC);
            return new ResourceNode(this.getProject(), node);
        } catch (Throwable e) {
            e.printStackTrace();
            e = ExceptionUtils.getRootCause(e);
            if (e instanceof AzureToolkitAuthenticationException) {
                final Action<Object> signin = AzureActionManager.getInstance().getAction(Action.AUTHENTICATE).bind(connection).withLabel("Sign in to manage connected resource");
                return new ActionNode<>(this.myProject, signin);
            } else {
                return new ExceptionNode(this.myProject, e);
            }
        }
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        final Connection<?, ?> connection = this.getValue();
        final Resource<?> resource = connection.getResource();
        final boolean isValid = connection.validate(getProject());
        final String icon = StringUtils.firstNonBlank(resource.getDefinition().getIcon(), AzureIcons.Common.AZURE.getIconPath());
        presentation.setIcon(IntelliJAzureIcons.getIcon(icon));
        presentation.addText(resource.getDefinition().getTitle(), AzureFacetRootNode.getTextAttributes(isValid));
        if (isValid) {
            presentation.addText(resource.getName(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        } else {
            presentation.setTooltip("Resource is missing, please edit the connection.");
        }
        if (resource.getDefinition().isEnvPrefixSupported()) {
            presentation.addText(" (" + connection.getEnvPrefix() + "_*)", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
        // presentation.setIcon(AllIcons.CodeWithMe.CwmInvite);
        // presentation.setIcon(AllIcons.Debugger.ThreadStates.Socket);
    }

    @Override
    public void onDoubleClicked(Object event) {
        final boolean isValid = getValue().validate(getProject());
        if (!isValid) {
            Optional.ofNullable(AzureActionManager.getInstance().getAction(EDIT_CONNECTION))
                    .ifPresent(action -> action.handle(getValue(), event));
        }
    }

    @Override
    @Nullable
    public Object getData(@Nonnull String dataId) {
        return StringUtils.equalsIgnoreCase(dataId, Action.SOURCE) ? this.getValue() : null;
    }

    @Nullable
    @Override
    public IActionGroup getActionGroup() {
        return new ActionGroup(
                editAction,
                "---",
                EDIT_CONNECTION,
                REMOVE_CONNECTION
        );
    }

    @Override
    public boolean isAlwaysExpand() {
        return true;
    }

    @Override
    public String toString() {
        return "->" + this.getValue().getResource().getName();
    }

    @Override
    public void navigate(boolean requestFocus) {
        final VirtualFile connectionsFile = this.getConnectionsFile();
        final PsiFile psiFile = Optional.ofNullable(connectionsFile)
                .map(f -> PsiManager.getInstance(getProject()).findFile(f)).orElse(null);
        if (Objects.isNull(psiFile)) {
            return;
        }
        NavigationUtil.openFileWithPsiElement(psiFile, requestFocus, requestFocus);
        EditorUtils.focusContentInCurrentEditor(getProject(), connectionsFile, getValue().getId());
    }

    @Override
    public boolean canNavigateToSource() {
        return Objects.nonNull(getConnectionsFile());
    }

    @Nullable
    private VirtualFile getConnectionsFile() {
        return Optional.ofNullable(getValue())
                .map(Connection::getProfile)
                .map(Profile::getConnectionManager)
                .map(ConnectionManager::getConnectionsFile)
                .orElse(null);
    }
}