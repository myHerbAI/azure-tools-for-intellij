/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerservice.property;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class KubernetesServicePropertiesEditorProvider implements FileEditorProvider, DumbAware {
    public static final String TYPE = "Microsoft.ContainerService.managedClusters";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file.getFileType().getName().equals(getEditorTypeId());
    }

    @Override
    @AzureOperation(name = "kubernetes.create_cluster_properties_editor.cluster", params = {"file.getName()"}, type = AzureOperation.Type.ACTION, target = AzureOperation.Target.PLATFORM)
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        final KubernetesCluster server = (KubernetesCluster) file.getUserData(AzureResourceEditorViewManager.AZURE_RESOURCE_KEY);
        assert server != null;
        return new KubernetesServicePropertiesEditor(file, server, project);
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return TYPE;
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
