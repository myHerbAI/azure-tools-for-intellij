/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.ProjectUtils;
import com.microsoft.azure.toolkit.intellij.common.utils.JdkUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class CreateSpringCloudAppAction {
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "Deployment succeeded but the app is still starting, " +
        "you can check the app status from Azure Portal.";
    private static final String NOTIFICATION_TITLE = "Deploy Spring app";

    public static void createApp(@Nonnull SpringCloudCluster cluster, @Nullable Project project) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final SpringCloudAppCreationDialog dialog = new SpringCloudAppCreationDialog(cluster, project);
            dialog.setDefaultRuntimeVersion(JdkUtils.getJdkLanguageLevel(Optional.ofNullable(project).orElseGet(ProjectUtils::getProject)));
            dialog.setOkActionListener((draft) -> {
                dialog.close();
                createApp(draft);
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "user/springcloud.create_app.app", params = "app.getName()")
    private static void createApp(SpringCloudAppDraft app) {
        AzureTaskManager.getInstance().runInBackground(OperationBundle.description("user/springcloud.create_app.app", app.getName()), () -> {
            final SpringCloudDeploymentDraft deployment = (SpringCloudDeploymentDraft) app.getActiveDeployment();
            Objects.requireNonNull(deployment).commit();
            app.reset();
            CacheManager.getUsageHistory(SpringCloudCluster.class).push(app.getParent());
            CacheManager.getUsageHistory(SpringCloudApp.class).push(app);
            final boolean hasArtifact = Optional.of(deployment)
                .map(SpringCloudDeploymentDraft::getArtifact)
                .map(IArtifact::getFile).isPresent();
            if (hasArtifact && !deployment.waitUntilReady(GET_STATUS_TIMEOUT)) {
                AzureMessager.getMessager().warning(GET_DEPLOYMENT_STATUS_TIMEOUT, NOTIFICATION_TITLE);
            }
        });
    }
}
