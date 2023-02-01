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

public class WebAppPropertyViewProvider extends WebAppBasePropertyViewProvider {
    public static final String TYPE = "WEB_APP_PROPERTY";

    @NotNull
    @Override
    @ExceptionNotification
    @AzureOperation(name = "platform/webapp.create_app_properties_editor.app", params = {"virtualFile.getName()"})
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        String sid = virtualFile.getUserData(OpenAppServicePropertyViewAction.SUBSCRIPTION_ID);
        String id = virtualFile.getUserData(OpenAppServicePropertyViewAction.RESOURCE_ID);
        return WebAppPropertyView.create(project, sid, id, virtualFile);
    }

    @Override
    protected String getType() {
        return TYPE;
    }
}
