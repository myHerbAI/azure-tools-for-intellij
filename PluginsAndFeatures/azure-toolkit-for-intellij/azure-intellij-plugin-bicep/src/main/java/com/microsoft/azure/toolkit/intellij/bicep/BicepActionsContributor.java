/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.bicep;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.dotnet.DotnetRuntimeHandler;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.io.File;
import java.util.function.BiConsumer;

public class BicepActionsContributor implements IActionsContributor {

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiConsumer<Object, AnActionEvent> downloadFuncCoreToolsHandler = (v, e) -> {
            final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            descriptor.setTitle("Select Path to Install .NET Runtime");
            final VirtualFile defaultFile = LocalFileSystem.getInstance().findFileByIoFile(new File(System.getProperty("user.home")));
            AzureTaskManager.getInstance().runLater(() -> FileChooser.chooseFile(descriptor, null, defaultFile, files -> {
                final String installPath = files.getPath();
                AzureTaskManager.getInstance().runInBackground("Download and Install .NET Runtime",
                        () -> DotnetRuntimeHandler.installDotnet(installPath));
            }));
        };
        am.registerHandler(ResourceCommonActionsContributor.INSTALL_DOTNET_RUNTIME, downloadFuncCoreToolsHandler);
    }
}
