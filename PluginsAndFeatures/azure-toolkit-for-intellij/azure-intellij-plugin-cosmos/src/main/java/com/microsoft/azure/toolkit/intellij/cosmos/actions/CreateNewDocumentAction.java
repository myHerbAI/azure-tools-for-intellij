/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.cosmos.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocumentContainer;
import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class CreateNewDocumentAction {

    private static final String DEFAULT_CONTENT =
        "{\n" +
        "\t\"id\": \"replace_with_new_document_id\"\n" +
        "}";

    @AzureOperation(name = "user/cosmos.create_document.container", params = {"container.getName()"})
    @SneakyThrows
    public static void create(@Nonnull final ICosmosDocumentContainer<?> container, @Nonnull final Project project) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final VirtualFile virtualFile = getOrCreateVirtualFile(container, fileEditorManager);
        final Function<String, Boolean> onSave = content -> {
            AzureTaskManager.getInstance().runInBackground(new AzureTask<>(OperationBundle.description("internal/cosmos.create_document.container", container.getName()), () -> {
                try {
                    final ObjectNode node = new ObjectMapper().readValue(content, ObjectNode.class);
                    container.importDocument(node);
                    AzureMessager.getMessager().info(String.format("Save document to %s successfully.", container.getName()));
                } catch (final RuntimeException | JsonProcessingException e) {
                    AzureMessager.getMessager().error(e);
                }
            }));
            return true;
        };
        final Runnable onClose = () -> WriteAction.run(() -> FileUtil.delete(new File(virtualFile.getPath())));
        VirtualFileActions.openFileInEditor(virtualFile, onSave, onClose, fileEditorManager);
    }

    private static synchronized VirtualFile getOrCreateVirtualFile(@Nonnull final ICosmosDocumentContainer<?> container, final FileEditorManager manager) {
        final VirtualFile virtualFile = VirtualFileActions.getVirtualFile(container.getId(), manager);
        return Objects.isNull(virtualFile) ? createVirtualFile(container, manager) : virtualFile;
    }

    @SneakyThrows
    private static VirtualFile createVirtualFile(@Nonnull final ICosmosDocumentContainer<?> container, final FileEditorManager manager) {
        final File tempFile = FileUtil.createTempFile(UUID.randomUUID().toString(), ".json", true);
        FileUtil.writeToFile(tempFile, DEFAULT_CONTENT);
        return VirtualFileActions.createVirtualFile(container.getId(), tempFile.getName(), tempFile, manager);
    }
}
