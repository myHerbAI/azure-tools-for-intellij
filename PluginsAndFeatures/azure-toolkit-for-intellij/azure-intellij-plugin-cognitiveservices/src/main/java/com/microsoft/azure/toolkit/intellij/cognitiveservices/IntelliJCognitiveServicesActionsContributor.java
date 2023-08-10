/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.cognitiveservices.CognitiveServicesActionsContributor;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.creation.CognitiveAccountCreationDialog;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.creation.CognitiveDeploymentCreationDialog;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeploymentDraft;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntelliJCognitiveServicesActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<AzureCognitiveServices, AnActionEvent> serviceCondition = (r, e) -> r instanceof AzureCognitiveServices;
        final BiConsumer<AzureCognitiveServices, AnActionEvent> createAccountHandler = (c, e) -> openAccountCreationDialog(e.getProject());
        am.registerHandler(CognitiveServicesActionsContributor.CREATE_ACCOUNT, serviceCondition, createAccountHandler);

        final BiPredicate<CognitiveAccount, AnActionEvent> accountCondition = (r, e) -> r instanceof CognitiveAccount;
        final BiConsumer<CognitiveAccount, AnActionEvent> openAccountHandler = (c, e) -> System.out.println("CognitiveServicesActionsContributor");
        am.registerHandler(CognitiveServicesActionsContributor.OPEN_ACCOUNT_IN_PLAYGROUND, accountCondition, openAccountHandler);

        final BiConsumer<CognitiveAccount, AnActionEvent> createDeploymentHandler = (c, e) -> openDeploymentCreationDialog(c, e.getProject());
        am.registerHandler(CognitiveServicesActionsContributor.CREATE_DEPLOYMENT, accountCondition, createDeploymentHandler);

        final BiPredicate<CognitiveDeployment, AnActionEvent> deploymentCondition = (r, e) -> r instanceof CognitiveDeployment;
        final BiConsumer<CognitiveDeployment, AnActionEvent> openDeploymentHandler = (c, e) -> System.out.println("CognitiveServicesActionsContributor");
        am.registerHandler(CognitiveServicesActionsContributor.OPEN_DEPLOYMENT_IN_PLAYGROUND, deploymentCondition, openDeploymentHandler);
    }

    public static void openAccountCreationDialog(@Nullable Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final CognitiveAccountCreationDialog dialog = new CognitiveAccountCreationDialog(project);
            dialog.setOkActionListener(value -> {
                final AzureString title = OperationBundle.description("user/cognitiveservices.create_account");
                AzureTaskManager.getInstance().runInBackground(title, value::commit);
                dialog.close();
            });
            dialog.show();
        });
    }

    public static void openDeploymentCreationDialog(@Nonnull CognitiveAccount account, @Nullable Project project) {
        final String name = Utils.generateRandomResourceName("deployment", 40);
        final CognitiveDeploymentDraft draft = account.deployments().create(name, account.getResourceGroupName());
        AzureTaskManager.getInstance().runLater(() -> {
            final CognitiveDeploymentCreationDialog dialog = new CognitiveDeploymentCreationDialog(account, project);
            dialog.setOkActionListener(value -> {
                final AzureString title = OperationBundle.description("user/cognitiveservices.create_deployment.account", account.getName());
                AzureTaskManager.getInstance().runInBackground(title, value::commit);
                dialog.close();
            });
            dialog.setValue(draft);
            dialog.show();
        });
    }
}
