/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.intellij.storage.component.FileCreationDialog;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobFileDraft;
import com.microsoft.azure.toolkit.lib.storage.blob.IBlobFile;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Consumer;

public class StorageFileActions {

    @SneakyThrows
    public static void openFileInEditor(StorageFile file, Project project) {
        final String failure = String.format("Can not open file (%s). Try downloading it first and open it manually.", file.getName());
        if (file.getSize() > 10 * FileUtils.ONE_MB) {
            AzureTaskManager.getInstance().runLater(() -> Messages.showWarningDialog(failure, "Open File"));
            return;
        }
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final VirtualFile virtualFile = VirtualFileActions.getOrCreateVirtualFile(file.getId(), file.getName(), fileEditorManager);
        final OutputStream output = virtualFile.getOutputStream(virtualFile);
        final AzureString title = OperationBundle.description("storage.load_content.file", virtualFile.getName());
        final AzureTask<Void> task = new AzureTask<>(project, title, false, () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            file.download(output);
            AzureTaskManager.getInstance().runLater(() -> {
                final Consumer<String> contentSaver = content -> {
                };
                if (!VirtualFileActions.openFileInEditor(contentSaver, virtualFile, fileEditorManager)) {
                    Messages.showWarningDialog(failure, "Open File");
                }
            });
            IOUtils.closeQuietly(output, null);
        });
        AzureTaskManager.getInstance().runInModal(task);
    }

    public static void createBlob(IBlobFile file, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final FileCreationDialog dialog = new FileCreationDialog(file, project, "New Blob");
            dialog.setOkActionListener((p) -> {
                dialog.close();
                final Path rawPath = Paths.get(p);
                IBlobFile current = file;
                int i = 0;
                for (; i < rawPath.getNameCount(); i++) {
                    final String name = rawPath.getName(i).toString();
                    final IBlobFile temp = (IBlobFile) current.getFile(name);
                    if (Objects.isNull(temp)) {
                        break;
                    }
                    current = temp;
                }
                final Path relativePath = rawPath.subpath(i, rawPath.getNameCount());
                final AbstractAzResourceModule<? extends StorageFile, ? extends StorageFile, ?> module = current.getSubFileModule();
                final BlobFileDraft draft = (BlobFileDraft) module.create(relativePath.getName(0).toString(), "");
                draft.setRelativePath(relativePath.toString());
                draft.setDirectory(relativePath.getNameCount() > 1);
                final AzureString title = OperationBundle.description("storage.create_blob.blob", draft.getPath());
                AzureTaskManager.getInstance().runInBackground(title, draft::createIfNotExist);
                openFileInEditor(current.getFile(relativePath.toString()), project);
            });
            dialog.show();
        });
    }

    public static void createFile(StorageFile file, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final FileCreationDialog dialog = new FileCreationDialog(file, project, "New Empty File");
            dialog.setOkActionListener((name) -> {
                dialog.close();
                final AbstractAzResourceModule<? extends StorageFile, ? extends StorageFile, ?> module = file.getSubFileModule();
                final AzResource.Draft<? extends StorageFile, ?> draft = module.create(name, "");
                final AzureString title = OperationBundle.description("storage.create_file.file", draft.getName());
                AzureTaskManager.getInstance().runInBackground(title, draft::createIfNotExist);
                openFileInEditor((StorageFile) draft, project);
            });
            dialog.show();
        });
    }

    public static void createDirectory(StorageFile file, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final FileCreationDialog dialog = new FileCreationDialog(file, project, "New Subdirectory");
            dialog.setOkActionListener((name) -> {
                dialog.close();
                final StorageFile.Draft<?, ?> draft = (StorageFile.Draft<?, ?>) file.getSubFileModule().create(name, "");
                draft.setDirectory(true);
                final AzureString title = OperationBundle.description("storage.create_directory.dir", draft.getName());
                AzureTaskManager.getInstance().runInBackground(title, draft::createIfNotExist);
            });
            dialog.show();
        });
    }

    public static void uploadFiles(StorageFile file, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, true, true, false, true);
            descriptor.setTitle("Choose Files to Upload");
            final VirtualFile[] files = FileChooser.chooseFiles(descriptor, project, null);
            for (final VirtualFile virtualFile : files) {
                final AzureString title = OperationBundle.description("storage.upload_files.file|dir", virtualFile.getName(), file.getName());
                final AzureTask<Void> task = new AzureTask<>(project, title, false, () -> {
                    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                    indicator.setIndeterminate(true);
                    final AbstractAzResourceModule<? extends StorageFile, ? extends StorageFile, ?> module = file.getSubFileModule();
                    final StorageFile.Draft<?, ?> draft = (StorageFile.Draft<?, ?>) module.create(virtualFile.getName(), "");
                    draft.setSourceFile(Paths.get(virtualFile.getPath()));
                    draft.createIfNotExist();
                });
                AzureTaskManager.getInstance().runInBackground(task);
            }
        });
    }

    public static void uploadFolder(StorageFile file, Project project) {

    }

    public static void downloadFile(StorageFile file, Project project) {
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runLater(() -> {
            final FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            fileChooserDescriptor.setTitle("Choose Where to Save the File");
            final VirtualFile vf = FileChooser.chooseFile(fileChooserDescriptor, null, null);
            if (vf != null) {
                final AzureString title = OperationBundle.description("storage.download_file.file|dir", file.getName(), vf.getPath());
                manager.runInModal(title, () -> {
                    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                    indicator.setIndeterminate(true);
                    final Path dest = Paths.get(Objects.requireNonNull(vf).getPath(), file.getName());
                    file.download(dest);
                    final File destFile = dest.toFile();
                    if (destFile.exists()) {
                        VirtualFileActions.notifyDownloadSuccess(file.getName(), destFile, project);
                    }
                });
            }
        });
    }

    public static void copyUrl(StorageFile file, Project project) {
        final String url = file.getUrl();
        CopyPasteManager.getInstance().setContents(new StringSelection(url));
        AzureMessager.getMessager().success(AzureString.format("URL of %s copied to clipboard: %s", file.getName(), url), "URL Copied", openUrl(url));
    }

    public static void copySasUrl(StorageFile file, Project project) {
        final String url = file.getSasUrl();
        CopyPasteManager.getInstance().setContents(new StringSelection(url));
        final AzureString message = AzureString.format("SAS Token and URL of %s copied to clipboard: %s. SAS token will expire after %s day.", file.getName(), url, 1);
        AzureMessager.getMessager().success(message, "SAS Token and URL copied", openUrl(url));
    }

    private static Action<Void> openUrl(@Nonnull final String url) {
        final Action.Id<Void> OPEN = Action.Id.of("common.open_in_browser");
        final ActionView.Builder view = new ActionView.Builder("Open with Browser");
        final Consumer<Void> handler = v -> AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(url);
        return new Action<>(OPEN, handler, view);
    }
}
