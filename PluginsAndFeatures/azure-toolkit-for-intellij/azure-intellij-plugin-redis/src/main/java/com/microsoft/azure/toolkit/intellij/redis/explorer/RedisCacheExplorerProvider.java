/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.explorer;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.redis.RedisCache;
import org.jetbrains.annotations.NotNull;

/**
 * Created by sheche on 2017/7/4.
 */
public class RedisCacheExplorerProvider implements FileEditorProvider, DumbAware {

    public static final String TYPE = "REDIS_EXPLORER";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return virtualFile.getFileType().getName().equals(TYPE);
    }

    @NotNull
    @Override
    @ExceptionNotification
    @AzureOperation(name = "user/redis.create_explorer_editor.redis", params = {"virtualFile.getName()"})
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        final RedisCache redis = (RedisCache) virtualFile.getUserData(AzureResourceEditorViewManager.AZURE_RESOURCE_KEY);
        return new RedisCacheExplorer(redis, virtualFile, project);
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
