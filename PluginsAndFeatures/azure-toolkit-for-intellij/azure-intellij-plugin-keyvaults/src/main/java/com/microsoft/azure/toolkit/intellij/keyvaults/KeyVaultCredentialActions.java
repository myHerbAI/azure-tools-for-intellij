/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvaults;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.TerminalUtils;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.lib.auth.cli.AzureCliUtils;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.keyvaults.Credential;
import com.microsoft.azure.toolkit.lib.keyvaults.CredentialVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class KeyVaultCredentialActions {

    public static final String AZURE_CLI_INSTALL_URL = "https://learn.microsoft.com/en-us/cli/azure/install-azure-cli#install";

    public static void showCredential(@Nonnull final CredentialVersion resource, @Nullable final Project project) {
        ensureAzureCli(project);
        final String command = resource.getShowCredentialCommand();
        TerminalUtils.executeInTerminal(project, command);
    }

    public static void downloadCredential(@Nonnull final CredentialVersion resource, @Nullable final Project project) {
        ensureAzureCli(project);
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runLater(() -> {
            final FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            fileChooserDescriptor.setTitle("Choose Where to Save the Credential");
            final VirtualFile vf = FileChooser.chooseFile(fileChooserDescriptor, null, null);
            if (vf != null) {
                final Path path = Paths.get(vf.getPath(), UUID.randomUUID().toString());
                final String downloadCredentialCommand = resource.getDownloadCredentialCommand(path.toString());
                TerminalUtils.executeInTerminal(project, downloadCredentialCommand);
                final File file = path.toFile();

                // todo: show notification after download
            }
        });
    }

    private static void ensureAzureCli(@Nullable final Project project) {
        boolean appropriateCliInstalled = AzureCliUtils.isAppropriateCliInstalled();
        if (!appropriateCliInstalled) {
            throw new AzureToolkitRuntimeException("Please install Azure CLI first.", getAzuriteFailureActions(project));
        }
    }

    private static Action<?>[] getAzuriteFailureActions(@Nonnull final Project project) {
        final Action<Object> settingAction = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS);
        final Action<String> learnAction = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL)
                .bind(AZURE_CLI_INSTALL_URL).withLabel("Learn More");
        return new Action[]{settingAction, learnAction};
    }
}
