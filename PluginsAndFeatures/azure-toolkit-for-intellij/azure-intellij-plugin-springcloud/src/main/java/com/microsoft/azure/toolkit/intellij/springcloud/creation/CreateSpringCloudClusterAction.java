/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudClusterDraft;

import javax.annotation.Nullable;
import java.util.Objects;

public class CreateSpringCloudClusterAction {
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "Deployment succeeded but the app is still starting, " +
        "you can check the app status from Azure Portal.";
    private static final String NOTIFICATION_TITLE = "Deploy Spring app";

    public static void createCluster(@Nullable Project project, @Nullable final SpringCloudClusterDraft data) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final SpringCloudClusterCreationDialog dialog = new SpringCloudClusterCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.getForm().setValue(data);
            }
            dialog.setOkActionListener((draft) -> {
                dialog.close();
                createCluster(draft);
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "user/springcloud.create_cluster.cluster", params = "cluster.getName()", source = "cluster")
    private static void createCluster(SpringCloudClusterDraft cluster) {
        AzureTaskManager.getInstance().runInBackground(OperationBundle.description("user/springcloud.create_cluster.cluster", cluster.getName()), () -> {
            CacheManager.getUsageHistory(SpringCloudCluster.class).push(cluster);
            final SpringCloudCluster createdCluster = cluster.createIfNotExist();
        });
    }
}
