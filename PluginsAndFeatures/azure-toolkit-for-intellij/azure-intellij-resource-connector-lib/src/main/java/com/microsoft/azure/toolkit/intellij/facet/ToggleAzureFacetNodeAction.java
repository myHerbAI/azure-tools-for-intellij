/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class ToggleAzureFacetNodeAction extends AnAction {
    @Override
    @AzureOperation(name = "user/connector.show_azure_node")
    public void actionPerformed(@Nonnull AnActionEvent event) {
        OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(event.getPlace(), EMPTY_PLACE));
        final Module module = LangDataKeys.MODULE.getData(event.getDataContext());
        if (Objects.nonNull(module) && !module.getProject().isDisposed()) {
            final Project project = Objects.requireNonNull(event.getProject());
            final PropertiesComponent properties = PropertiesComponent.getInstance(project);
            properties.setValue(module.getName() + ".azure", "show");
            ProjectView.getInstance(module.getProject()).getCurrentProjectViewPane().updateFromRoot(true);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Module module = LangDataKeys.MODULE.getData(e.getDataContext());
        final Object[] selected = PlatformDataKeys.SELECTED_ITEMS.getData(e.getDataContext());
        final boolean onDotAzure = Objects.nonNull(selected) && selected.length == 1 && (selected[0] instanceof PsiDirectoryNode node) && ".azure".equalsIgnoreCase(node.getValue().getName());
        e.getPresentation().setEnabledAndVisible(Objects.nonNull(module) || onDotAzure);
    }
}
