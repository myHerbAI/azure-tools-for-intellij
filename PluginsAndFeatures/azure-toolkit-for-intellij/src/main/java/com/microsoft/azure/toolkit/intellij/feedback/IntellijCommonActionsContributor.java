/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.feedback;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.settings.AzureSettingsConfigurable;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Optional;
import java.util.function.BiConsumer;

public class IntellijCommonActionsContributor implements IActionsContributor {

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
    }

    private static void openSettingsDialog(Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, AzureSettingsConfigurable.class);
    }
}