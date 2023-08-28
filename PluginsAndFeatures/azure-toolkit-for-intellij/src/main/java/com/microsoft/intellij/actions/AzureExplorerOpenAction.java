/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.ui.ServerExplorerToolWindowFactory;

import javax.annotation.Nonnull;

public class AzureExplorerOpenAction extends AnAction {
    @Override
    @AzureOperation(name = "user/common.open_explorer")
    public void actionPerformed(@Nonnull AnActionEvent event) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        ToolWindowManager.getInstance(project).getToolWindow(ServerExplorerToolWindowFactory.EXPLORER_WINDOW).activate(null);
    }
}
