/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceIntelliJActionsContributor;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppModule;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionsServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppWindowsRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppWindowsRuntime;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppModule;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppServiceSubscription;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WebAppComboBox extends AppServiceComboBox<AppServiceConfig> {
    public WebAppComboBox(Project project) {
        super(project);
    }

    @Override
    protected void refreshItems() {
        Azure.az(AzureWebApp.class).refresh();
        super.refreshItems();
    }

    @Override
    protected List<AppServiceConfig> loadAppServiceModels() {
        final Account account = Azure.az(AzureAccount.class).account();
        if (!account.isLoggedIn()) {
            return Collections.emptyList();
        }
        if (!WebAppWindowsRuntime.isLoaded() && !WebAppLinuxRuntime.isLoaded()) {
            final WebAppModule module = Azure.az(AzureWebApp.class).webApps(account.getSelectedSubscriptions().get(0).getId());
            ((WebAppServiceSubscription) module.getParent()).loadRuntimes();
        }
        final List<WebApp> webApps = Azure.az(AzureWebApp.class).webApps();
        return webApps.stream().parallel()
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .map(webApp -> convertAppServiceToConfig(AppServiceConfig::new, webApp))
            .filter(a -> Objects.nonNull(a.subscriptionId()))
            .collect(Collectors.toList());
    }

    @Override
    protected void createResource() {
        // todo: hide deployment part in creation dialog
        final WebAppCreationDialog dialog = new WebAppCreationDialog(project);
        dialog.setDeploymentVisible(false);
        dialog.setData(AppServiceIntelliJActionsContributor.getDefaultWebAppConfig(null));
        final Action.Id<AppServiceConfig> actionId = Action.Id.of("user/webapp.create_app.app");
        dialog.setOkAction(new Action<>(actionId)
            .withLabel("Create")
            .withIdParam(AppServiceConfig::appName)
            .withAuthRequired(false)
            .withHandler(c -> this.setValue(c)));
        dialog.show();
    }
}
