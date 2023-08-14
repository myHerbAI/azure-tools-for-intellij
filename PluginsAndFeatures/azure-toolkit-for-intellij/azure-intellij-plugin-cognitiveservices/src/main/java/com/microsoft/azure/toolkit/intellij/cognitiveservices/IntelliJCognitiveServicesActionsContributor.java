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
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.*;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntelliJCognitiveServicesActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<AzureCognitiveServices, AnActionEvent> serviceCondition = (r, e) -> r instanceof AzureCognitiveServices;
        final BiConsumer<AzureCognitiveServices, AnActionEvent> createAccountHandler = (c, e) -> openAccountCreationDialog(e.getProject(), null);
        am.registerHandler(CognitiveServicesActionsContributor.CREATE_ACCOUNT, serviceCondition, createAccountHandler);
        am.registerHandler(CognitiveServicesActionsContributor.GROUP_CREATE_ACCOUNT,
                (ResourceGroup r, AnActionEvent e) -> openAccountCreationDialog(e.getProject(), r));

        final BiPredicate<CognitiveAccount, AnActionEvent> accountCondition = (r, e) -> r instanceof CognitiveAccount;
        final BiConsumer<CognitiveAccount, AnActionEvent> openAccountHandler = (c, e) -> System.out.println("CognitiveServicesActionsContributor");
        am.registerHandler(CognitiveServicesActionsContributor.OPEN_ACCOUNT_IN_PLAYGROUND, accountCondition, openAccountHandler);

        final BiConsumer<CognitiveAccount, AnActionEvent> createDeploymentHandler = (c, e) -> openDeploymentCreationDialog(c, e.getProject());
        am.registerHandler(CognitiveServicesActionsContributor.CREATE_DEPLOYMENT, accountCondition, createDeploymentHandler);

        final BiPredicate<CognitiveDeployment, AnActionEvent> deploymentCondition = (r, e) -> r instanceof CognitiveDeployment;
        final BiConsumer<CognitiveDeployment, AnActionEvent> openDeploymentHandler = (c, e) -> System.out.println("CognitiveServicesActionsContributor");
        am.registerHandler(CognitiveServicesActionsContributor.OPEN_DEPLOYMENT_IN_PLAYGROUND, deploymentCondition, openDeploymentHandler);
    }

    public static void openAccountCreationDialog(@Nullable Project project, @Nullable ResourceGroup resourceGroup) {
        // action is auth required, so skip validation for authentication
        final String account = Utils.generateRandomResourceName("account", 40);
        final String rgName = Optional.ofNullable(resourceGroup).map(AzResource::getName)
                .orElseGet(() -> String.format("rg-%s", account));
        final Subscription subscription = Optional.ofNullable(resourceGroup).map(ResourceGroup::getSubscription)
                .orElseGet(() -> Azure.az(AzureAccount.class).account().getSelectedSubscriptions().get(0));
        final ResourceGroup group = Optional.ofNullable(resourceGroup)
                .orElseGet(() -> Azure.az(AzureResources.class).groups(subscription.getId()).create(rgName, rgName));
        final CognitiveAccountDraft accountDraft =
                Azure.az(AzureCognitiveServices.class).accounts(subscription.getId()).create(account, rgName);
        accountDraft.setConfig(CognitiveAccountDraft.Config.builder().resourceGroup(group).build());
        AzureTaskManager.getInstance().runLater(() -> {
            final CognitiveAccountCreationDialog dialog = new CognitiveAccountCreationDialog(project);
            dialog.setValue(accountDraft);
            dialog.setOkAction(new Action<CognitiveAccountDraft>(Action.Id.of("user/cognitiveservices.create_account"))
                    .withLabel("Create")
                    .withAuthRequired(true)
                    .withHandler(AzResource.Draft::commit));
            dialog.show();
        });
    }

    public static void openDeploymentCreationDialog(@Nonnull CognitiveAccount account, @Nullable Project project) {
        final String name = Utils.generateRandomResourceName("deployment", 40);
        final CognitiveDeploymentDraft draft = account.deployments().create(name, account.getResourceGroupName());
        AzureTaskManager.getInstance().runLater(() -> {
            final CognitiveDeploymentCreationDialog dialog = new CognitiveDeploymentCreationDialog(account, project);
            dialog.setOkAction(new Action<CognitiveDeploymentDraft>(Action.Id.of("user/cognitiveservices.create_deployment.account"))
                    .withLabel("Create")
                    .withIdParam(account.getName())
                    .withAuthRequired(true)
                    .withHandler(AzResource.Draft::commit));
            dialog.setValue(draft);
            dialog.show();
        });
    }
}
