/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.IAccountActions;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.intellij.AzureConfigurable;
import com.microsoft.azure.toolkit.intellij.common.auth.AzureSignInAction;
import com.microsoft.azure.toolkit.intellij.common.subscription.SelectSubscriptionsAction;
import com.microsoft.intellij.ui.ServerExplorerToolWindowFactory;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

public class LegacyIntellijAccountActionsContributor implements IActionsContributor {
    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(Action.REQUIRE_AUTH)
            .withLabel("Authorize")
            .withHandler((Runnable r, AnActionEvent e) ->
                AzureSignInAction.requireSignedIn(Optional.ofNullable(e).map(AnActionEvent::getProject).orElse(null), r))
            .withAuthRequired(false)
            .register(am);

        new Action<>(Action.AUTHENTICATE)
            .withLabel("Sign in")
            .withHandler((Object v, AnActionEvent e) -> {
                final AzureAccount az = Azure.az(AzureAccount.class);
                if (az.isLoggedIn()) az.logout();
                AzureSignInAction.authActionPerformed(e.getProject());
            })
            .withAuthRequired(false)
            .register(am);

        new Action<>(IAccountActions.SELECT_SUBS)
            .withLabel("Select Subscriptions")
            .withHandler((Object v, AnActionEvent e) -> SelectSubscriptionsAction.selectSubscriptions(e.getProject()))
            .withAuthRequired(false)
            .register(am);
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiConsumer<Object, AnActionEvent> openSettingsHandler = (ignore, e) -> {
            final Project project = Optional.ofNullable(e).map(AnActionEvent::getProject).orElseGet(() -> {
                final Project[] openProjects = ProjectManagerEx.getInstance().getOpenProjects();
                return ArrayUtils.isEmpty(openProjects) ? null : openProjects[0];
            });
            final AzureString title = OperationBundle.description("user/common.open_azure_settings");
            AzureTaskManager.getInstance().runLater(new AzureTask<>(title, () -> openSettingsDialog(project)));
        };
        am.registerHandler(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS, (i, e) -> true, openSettingsHandler);

        final BiConsumer<Object, AnActionEvent> openAzureExplorer = (ignore, e) -> openAzureExplorer(e);
        am.registerHandler(ResourceCommonActionsContributor.OPEN_AZURE_EXPLORER, (i, e) -> true, openAzureExplorer);
    }

    private static void openAzureExplorer(AnActionEvent e) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(Objects.requireNonNull(e.getProject())).
            getToolWindow(ServerExplorerToolWindowFactory.EXPLORER_WINDOW);
        if (Objects.nonNull(toolWindow) && !toolWindow.isVisible()) {
            AzureTaskManager.getInstance().runLater(toolWindow::show);
        }
    }

    private static void openSettingsDialog(Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, AzureConfigurable.class);
    }
}