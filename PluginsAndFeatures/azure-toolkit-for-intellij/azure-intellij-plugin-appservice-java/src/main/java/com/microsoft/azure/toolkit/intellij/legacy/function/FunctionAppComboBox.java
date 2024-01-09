/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceIntelliJActionsContributor;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppModule;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionsServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppWindowsRuntime;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FunctionAppComboBox extends AppServiceComboBox<FunctionAppConfig> {

    public FunctionAppComboBox(final Project project) {
        super(project);
    }

    @Override
    protected void refreshItems() {
        Azure.az(AzureFunctions.class).refresh();
        super.refreshItems();
    }

    @Override
    protected void createResource() {
        final FunctionAppCreationDialog dialog = new FunctionAppCreationDialog(project);
        dialog.setData(AppServiceIntelliJActionsContributor.getDefaultFunctionAppConfig(null));
        final Action.Id<FunctionAppConfig> actionId = Action.Id.of("user/function.create_app.app");
        dialog.setOkAction(new Action<>(actionId)
            .withLabel("Create")
            .withIdParam(FunctionAppConfig::appName)
            .withSource(s -> s)
            .withAuthRequired(false)
            .withHandler(draft -> this.setValue(draft)));
        dialog.show();
    }

    @Nonnull
    @Override
    @AzureOperation(name = "internal/function.list_java_apps")
    protected List<FunctionAppConfig> loadAppServiceModels() {
        final Account account = Azure.az(AzureAccount.class).account();
        if (!account.isLoggedIn()) {
            return Collections.emptyList();
        }
        if (!FunctionAppWindowsRuntime.isLoaded() && !FunctionAppLinuxRuntime.isLoaded()) {
            final FunctionAppModule functionAppModule = Azure.az(AzureFunctions.class).functionApps(account.getSelectedSubscriptions().get(0).getId());
            ((FunctionsServiceSubscription) functionAppModule.getParent()).loadRuntimes();
        }
        return Azure.az(AzureFunctions.class).functionApps().parallelStream()
            .map(functionApp -> convertAppServiceToConfig(FunctionAppConfig::new, functionApp))
            .filter(a -> Objects.nonNull(a.subscriptionId()))
            .sorted((app1, app2) -> app1.appName().compareToIgnoreCase(app2.appName()))
            .collect(Collectors.toList());
    }
}
