/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.integration.services;

import com.intellij.execution.services.ServiceViewDescriptor;
import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.component.TreeUtils;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.util.Optional;

@RequiredArgsConstructor
public class AzureResourceViewDescriptor implements ServiceViewDescriptor {
    private final Project project;
    private final Node<?> node;

    @Override
    public @NotNull ItemPresentation getPresentation() {
        final Node.View view = node.getView();
        final PresentationData presentation = new PresentationData();
        presentation.setIcon(IntelliJAzureIcons.getIcon(view.getIcon()));
        final SimpleTextAttributes attributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
        presentation.addText(view.getLabel(), attributes);
        presentation.setTooltip(view.getTips());
        Optional.ofNullable(view.getDescription()).ifPresent(d -> presentation.addText(" " + d, SimpleTextAttributes.GRAYED_ATTRIBUTES));
        return presentation;
    }

    @Override
    public @Nullable ActionGroup getPopupActions() {
        return TreeUtils.toIntellijActionGroup(node.getActions());
    }

    @Override
    public @Nullable DataProvider getDataProvider() {
        return dataId -> {
            if (StringUtils.equalsIgnoreCase(dataId, Action.SOURCE)) {
                return Optional.ofNullable(node).map(Node::getValue).orElse(null);
            }
            return null;
        };
    }

    @Override
    public boolean handleDoubleClick(@NotNull final MouseEvent e) {
        final DataContext context = DataManager.getInstance().getDataContext(e.getComponent());
        final AnActionEvent event = AnActionEvent.createFromInputEvent(e, "ServicesNode.click", null, context);
        this.node.doubleClick(event);
        return true;
    }
}
