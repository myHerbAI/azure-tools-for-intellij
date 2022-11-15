/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;

public class ContainerRegistryPropertyViewProvider implements FileEditorProvider, DumbAware {

    public static final String TYPE = "CONTAINER_REGISTRY_PROPERTY_VIEW";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return virtualFile.getFileType().getName().equals(TYPE);
    }

    @NotNull
    @Override
    @ExceptionNotification
    @AzureOperation(name = "container.create_registry_properties_editor.registry", params = {"virtualFile.getName()"},type = AzureOperation.Type.ACTION, target = AzureOperation.Target.PLATFORM)
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new ContainerRegistryPropertyView(project, virtualFile);
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return TYPE;
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
