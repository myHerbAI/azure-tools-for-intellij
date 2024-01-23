/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceIntelliJActionsContributor;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.ui.WebAppComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppDockerRuntime;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.common.action.Action;

import java.util.List;
import java.util.stream.Collectors;

public class DockerWebAppComboBox extends WebAppComboBox {

    public DockerWebAppComboBox(Project project) {
        super(project);
        setRenderer(new AppServiceComboBox.AppComboBoxRender(true));
    }

    @Override
    protected List<AppServiceConfig> loadAppServiceModels() {
        final List<WebApp> webApps = Azure.az(AzureWebApp.class).webApps();
        return webApps.stream().parallel()
            .filter(a -> a.getRuntime() != null && !a.getRuntime().isWindows())
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .map(webApp -> convertAppServiceToConfig(AppServiceConfig::new, webApp))
            .collect(Collectors.toList());
    }

    @Override
    protected void createResource() {
        // todo: hide deployment part in creation dialog
        final DockerWebAppCreationDialog dialog = new DockerWebAppCreationDialog(project);
        final AppServiceConfig defaultConfig = AppServiceIntelliJActionsContributor.getDefaultWebAppConfig(null);
        defaultConfig.setRuntime(RuntimeConfig.fromRuntime(WebAppDockerRuntime.INSTANCE));
        dialog.setData(defaultConfig);
        dialog.setDeploymentVisible(false);
        final Action.Id<AppServiceConfig> actionId = Action.Id.of("user/webapp.create_app.app");
        dialog.setOkAction(new Action<>(actionId)
            .withLabel("Create")
            .withIdParam(AppServiceConfig::appName)
            .withSource(s -> s)
            .withAuthRequired(false)
            .withHandler(config -> this.setValue(config)));
        dialog.show();
    }
}
