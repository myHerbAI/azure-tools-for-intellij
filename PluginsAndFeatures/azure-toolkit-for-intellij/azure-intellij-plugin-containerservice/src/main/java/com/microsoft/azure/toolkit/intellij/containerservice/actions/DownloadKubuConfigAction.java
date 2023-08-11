/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerservice.actions;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.FileChooser;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;

public class DownloadKubuConfigAction {
    @AzureOperation(name = "azure/kubernetes.download_config.kubernetes", params = {"cluster.getName()"})
    public static void downloadKubuConfig(@Nonnull KubernetesCluster cluster, @Nonnull Project project, boolean isAdmin) {
        final String title = "Download kubernetes configuration";
        final String fileName = String.format("%s-%s.yml", cluster.getName(), isAdmin ? "admin" : "user");
        final File destFile = AzureTaskManager.getInstance().runLater(() -> FileChooser.showFileSaver(title, fileName)).join();
        if (destFile == null) {
            return;
        }
        try {
            final byte[] content = isAdmin ? cluster.getAdminKubeConfig() : cluster.getUserKubeConfig();
            FileUtils.writeByteArrayToFile(destFile, content);
            AzureMessager.getMessager().info(AzureString.format("Save kubernetes configuration file for %s to %s successfully.", cluster.getName(), destFile.getAbsolutePath()),
                null, getOpenInExplorerAction(project, destFile), getOpenInEditorAction(project, destFile), getOpenKubernetesAction(project, destFile));
        } catch (final IOException e) {
            AzureMessager.getMessager().error(e);
        }
    }

    @Nullable
    private static Action<?> getOpenKubernetesAction(@Nonnull Project project, @Nonnull File file) {
        if (!PluginManagerCore.isPluginInstalled(PluginId.getId(KubernetesUtils.KUBERNETES_PLUGIN_ID))) {
            return null;
        }
        final Action.Id<Void> id = Action.Id.of("user/kubernetes.set_kube_config");
        return new Action<>(id)
            .withLabel("Set kubeconfig for project")
            .withHandler(ignore -> {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(file.getAbsolutePath()), null);
                AzureTaskManager.getInstance().runLater(() -> ShowSettingsUtil.getInstance().showSettingsDialog(project, "Kubernetes"));
            });
    }

    // todo: remove duplicated with AppServiceFileAction
    private static Action<?> getOpenInExplorerAction(@Nonnull Project project, @Nonnull File file) {
        return AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.REVEAL_FILE).bind(file);
    }

    private static Action<?> getOpenInEditorAction(@Nonnull Project project, @Nonnull File file) {
        return AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_FILE).bind(file);
    }
}
