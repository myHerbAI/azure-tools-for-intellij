/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.appservice.actions.OpenAppServicePropertyViewAction;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;

public class DeploymentSlotPropertyViewProvider extends WebAppBasePropertyViewProvider {
    public static final String TYPE = "DEPLOYMENT_SLOT_PROPERTY";

    @NotNull
    @Override
    @ExceptionNotification
    @AzureOperation(name = "webapp.create_slot_properties_editor.slot", params = {"virtualFile.getName()"}, type = AzureOperation.Type.ACTION, target = AzureOperation.Target.PLATFORM)
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        final String sid = virtualFile.getUserData(OpenAppServicePropertyViewAction.SUBSCRIPTION_ID);
        final String webAppId = virtualFile.getUserData(OpenAppServicePropertyViewAction.WEBAPP_ID);
        final String name = virtualFile.getUserData(OpenAppServicePropertyViewAction.SLOT_NAME);
        return DeploymentSlotPropertyView.create(project, sid, webAppId, name, virtualFile);
    }

    @Override
    protected String getType() {
        return TYPE;
    }
}
