/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.properties;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import org.jetbrains.annotations.NotNull;

public class ContainerAppPropertiesEditorProvider implements FileEditorProvider, DumbAware {

    public static final String CONTAINER_APP_PROPERTY_TYPE = "Microsoft.App.containerApps";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return virtualFile.getFileType().getName().equals(getEditorTypeId());
    }

    @NotNull
    @Override
    @ExceptionNotification
    @AzureOperation(name = "user/containerapps.create_properties_editor.app", params = {"virtualFile.getName()"})
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        final ContainerApp app = (ContainerApp) virtualFile.getUserData(AzureResourceEditorViewManager.AZURE_RESOURCE_KEY);
        assert app != null;
        return new ContainerAppPropertiesEditor(project, app, virtualFile);
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return CONTAINER_APP_PROPERTY_TYPE;
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
