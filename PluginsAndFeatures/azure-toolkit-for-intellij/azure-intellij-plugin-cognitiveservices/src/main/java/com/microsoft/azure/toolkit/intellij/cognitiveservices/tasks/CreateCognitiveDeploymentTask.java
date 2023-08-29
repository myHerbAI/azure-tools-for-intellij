/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.tasks;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.input.CognitiveDeploymentInput;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.cognitiveservices.*;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountModel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountSku;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.DeploymentModel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.DeploymentSku;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public class CreateCognitiveDeploymentTask implements Task {
    public static final String DEFAULT_COGNITIVE_DEPLOYMENT = "default_cognitive_deployment";
    public static final String GETTING_START_ACCOUNT = "getting-started-account";
    public static final String GETTING_START_DEPLOYMENT = "getting-started-deployment";
    public static final String ACCOUNT = "cognitive_account";
    public static final String DEPLOYMENT = "cognitive_deployment";
    private final ComponentContext context;

    public CreateCognitiveDeploymentTask(ComponentContext taskContext) {
        this.context = taskContext;
    }

    @Override
    public void prepare() {
        final Phase currentPhase = context.getCourse().getCurrentPhase();
        currentPhase.expandPhasePanel();
        this.init();
    }

    @Override
    public void execute() {
        final CognitiveAccount account = (CognitiveAccount) context.getParameter(CognitiveDeploymentInput.COGNITIVE_ACCOUNT);
        final CognitiveDeployment deployment = (CognitiveDeployment) context.getParameter(CognitiveDeploymentInput.COGNITIVE_DEPLOYMENT);
        if (account.isDraftForCreating()) {
            ((CognitiveAccountDraft) account).commit();
        } else {
            AzureMessager.getMessager().info(String.format("Cognitive account %s already exists.", account.getName()));
        }
        if (deployment.isDraftForCreating()) {
            final CognitiveDeploymentDraft draft = (CognitiveDeploymentDraft) deployment;
            final CognitiveDeploymentDraft.Config config = new CognitiveDeploymentDraft.Config();
            final AccountModel model = account.listModels().stream().filter(AccountModel::isGPTModel).findFirst()
                    .orElseThrow(() -> new AzureToolkitRuntimeException(String.format("GPT model is not supported in account %s, please try with another one.", account.getName())));
            config.setSku(DeploymentSku.fromModelSku(model.getSkus().get(0)));
            config.setModel(DeploymentModel.fromAccountModel(model));
            draft.setConfig(config);
            (draft).commit();
        } else {
            AzureMessager.getMessager().info(String.format("Cognitive deployment %s already exists.", deployment.getName()));
        }
        context.applyResult("resourceId", account.getId());
        context.applyResult(ACCOUNT, account);
        context.applyResult(DEPLOYMENT, deployment);
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.openai.create_deployment";
    }

    private void init() {
        final Subscription subscription = (Subscription) context.getParameter(SelectSubscriptionTask.SUBSCRIPTION);
        final CognitiveAccountModule module = Azure.az(AzureCognitiveServices.class).forSubscription(subscription.getId()).accounts();
        final CognitiveAccount cognitiveAccount = module.list().stream()
                .filter(account -> StringUtils.startsWith(account.getName(), GETTING_START_ACCOUNT))
                .findFirst().orElseGet(() -> createAccountDraft(subscription));
        final CognitiveDeployment draft = cognitiveAccount.deployments().list().stream()
                .filter(deployment -> StringUtils.startsWith(deployment.getName(), GETTING_START_DEPLOYMENT))
                .findFirst().orElseGet(() -> createDeploymentDraft(cognitiveAccount));
        context.applyResult(DEFAULT_COGNITIVE_DEPLOYMENT, draft);
    }

    protected CognitiveDeploymentDraft createDeploymentDraft(final CognitiveAccount account) {
        final String name = Utils.generateRandomResourceName(GETTING_START_DEPLOYMENT, 40);
        final CognitiveDeploymentDraft draft = account.deployments().create(name, account.getResourceGroupName());
        draft.setConfig(new CognitiveDeploymentDraft.Config());
        return draft;
    }

    protected CognitiveAccountDraft createAccountDraft(final Subscription subscription) {
        final String account = Utils.generateRandomResourceName(GETTING_START_ACCOUNT, 40);
        final String rgName = String.format("rg-%s", account);
        final ResourceGroup group = Azure.az(AzureResources.class).groups(subscription.getId()).create(rgName, rgName);
        final CognitiveAccountModule accounts = Azure.az(AzureCognitiveServices.class).accounts(subscription.getId());
        final AccountSku accountSku = accounts.listSkus(null).get(0);
        final Region region = accounts.listRegion(accountSku).get(0);
        CognitiveAccountDraft draft = accounts.create(account, rgName);
        draft.setConfig(CognitiveAccountDraft.Config.builder().resourceGroup(group).sku(accountSku).region(region).build());
        return draft;
    }
}
