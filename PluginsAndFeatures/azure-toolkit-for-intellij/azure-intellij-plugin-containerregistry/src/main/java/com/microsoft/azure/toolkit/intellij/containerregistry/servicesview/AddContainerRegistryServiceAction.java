/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.servicesview;

import com.intellij.docker.registry.DockerRegistryConfigurable;
import com.intellij.docker.registry.DockerRegistryConfiguration;
import com.intellij.docker.registry.DockerRegistryManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class AddContainerRegistryServiceAction extends AnAction {
    @Override
    @AzureOperation("user/acr.edit_acr_registry_in_services_view")
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final DockerRegistryConfiguration registry = (new DockerRegistryConfiguration()).withName("New Azure Container Registry");
        registry.setRegistryProviderId(AzureContainerRegistryProvider.ID);
        final DockerRegistryConfigurable configurable = new DockerRegistryConfigurable(registry, null, false);
        if (ShowSettingsUtil.getInstance().editConfigurable(e.getProject(), configurable)) {
            final DockerRegistryManager registryManager = DockerRegistryManager.getInstance();
            final Set<DockerRegistryConfiguration> registries = new HashSet<>(registryManager.getRegistries());
            if (!registries.contains(registry)) {
                registryManager.addRegistry(registry);
            }
        }
    }
}
