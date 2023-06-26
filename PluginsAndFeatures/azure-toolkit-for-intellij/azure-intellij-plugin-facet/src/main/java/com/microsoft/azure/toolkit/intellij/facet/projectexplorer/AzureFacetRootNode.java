/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.ide.projectView.NodeSortOrder;
import com.intellij.ide.projectView.NodeSortSettings;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.NamedColorUtil;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics;
import com.microsoft.azure.toolkit.intellij.connector.DeploymentTargetTopics;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics.CONNECTION_CHANGED;
import static com.microsoft.azure.toolkit.intellij.connector.DeploymentTargetTopics.TARGET_APP_CHANGED;

public class AzureFacetRootNode extends ProjectViewNode<AzureModule> implements IAzureFacetNode {

    public AzureFacetRootNode(final AzureModule module, ViewSettings settings) {
        super(module.getProject(), module, settings);
        AzureEventBus.once("account.logged_in.account", (a, b) -> this.rerender(true));
        final MessageBusConnection connection = module.getProject().getMessageBus().connect();
        connection.subscribe(CONNECTION_CHANGED, (ConnectionTopics.ConnectionChanged) (p, conn, action) -> {
            if (conn.getConsumer().getId().equalsIgnoreCase(module.getName())) {
                rerender(true);
            }
        });
        connection.subscribe(TARGET_APP_CHANGED, (DeploymentTargetTopics.TargetAppChanged) (m, app, action) -> {
            if (m.getName().equalsIgnoreCase(module.getName())) {
                rerender(true);
            }
        });
    }

    @Override
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final AzureModule module = this.getValue();
//        final ArrayList<AbstractTreeNode<?>> result = new ArrayList<>();
//        final List<Connection<?, ?>> connections = Optional.ofNullable(module.getDefaultProfile()).map(Profile::getConnections).orElse(Collections.emptyList());
//        final List<String> appIds = Optional.ofNullable(module.getDefaultProfile()).map(Profile::getTargetAppIds).orElse(Collections.emptyList());
//        if (CollectionUtils.isNotEmpty(connections)) { // add back .azure file node
//            final VirtualFile virtualFile = Optional.ofNullable(getValue()).map(AzureModule::getDotAzureDir).flatMap(op -> op).orElse(null);
//            Optional.ofNullable(virtualFile)
//                .map(dir -> PsiManagerEx.getInstanceEx(getProject()).findDirectory(dir))
//                .map(dir -> new PsiDirectoryNode(getProject(), dir, viewSettings)).ifPresent(result::add);
//        }
//        if (!appIds.isEmpty()) {
//            result.add(new DeploymentTargetsNode(module));
//        }
//        result.add(new ConnectionsNode(module));
        return new ConnectionsNode(module).getChildren();
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        final AzureModule value = getValue();
        final List<Connection<?, ?>> connections = Optional.ofNullable(value.getDefaultProfile())
                .map(Profile::getConnections).orElse(Collections.emptyList());
        final boolean connected = CollectionUtils.isNotEmpty(connections);
        final boolean isConnectionValid = connections.stream().allMatch(c -> c.validate(getProject()));
        presentation.addText("Azure", getTextAttributes(isConnectionValid));
        presentation.setTooltip(isConnectionValid ? "Manage connected Azure resources here." : "Invalid connections found.");
        presentation.setIcon(connected ? IntelliJAzureIcons.getIcon("/icons/Common/AzureResourceConnector.svg") : IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
    }

    public static SimpleTextAttributes getTextAttributes(boolean isValid) {
        final SimpleTextAttributes regularAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
        return isValid ? regularAttributes : new SimpleTextAttributes(regularAttributes.getBgColor(),
                regularAttributes.getFgColor(), JBUI.CurrentTheme.Focus.warningColor(true), SimpleTextAttributes.STYLE_WAVED);
    }

    @Override
    @Nullable
    public Object getData(@Nonnull String dataId) {
        if (StringUtils.equalsIgnoreCase(dataId, Action.SOURCE)) {
            return this.getValue();
        } else if (StringUtils.equalsIgnoreCase(dataId, CommonDataKeys.VIRTUAL_FILE.getName())) {
            return Optional.ofNullable(getValue()).map(AzureModule::getDotAzureDir).flatMap(op -> op).orElse(null);
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public IActionGroup getActionGroup() {
        return AzureActionManager.getInstance().getGroup(ResourceConnectionActionsContributor.EXPLORER_MODULE_ROOT_ACTIONS);
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public NodeSortOrder getSortOrder(NodeSortSettings settings) {
        return NodeSortOrder.FOLDER;
    }

    @Override
    public boolean contains(VirtualFile file) {
        return false;
    }

    @Override
    public String toString() {
        return "Azure";
    }
}
