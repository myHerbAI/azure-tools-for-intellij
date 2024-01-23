/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.appservice;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class DockerUtils {

    @Nullable
    public static ContainerAppDraft.ImageConfig convertRuntimeConfigToImageConfig(@Nonnull final RuntimeConfig config) {
        if (config.os() != OperatingSystem.DOCKER || StringUtils.isBlank(config.getImage())) {
            return null;
        }
        final ContainerAppDraft.ImageConfig imageConfig = new ContainerAppDraft.ImageConfig(config.getImage());
        Azure.az(AzureContainerRegistry.class).list().stream()
             .flatMap(s -> s.getAzureContainerRegistryModule().list().stream())
             .filter(registry -> StringUtils.equalsIgnoreCase(registry.getLoginServerUrl(), config.getRegistryUrl()))
             .findFirst().ifPresent(imageConfig::setContainerRegistry);
        return imageConfig;
    }

    @Nonnull
    public static RuntimeConfig convertImageConfigToRuntimeConfig(@Nonnull final ContainerAppDraft.ImageConfig image) {
        final RuntimeConfig runtime = new RuntimeConfig();
        runtime.os(OperatingSystem.DOCKER);
        runtime.image(image.getFullImageName());
        Optional.ofNullable(image.getContainerRegistry()).ifPresent(registry -> {
            runtime.registryUrl(registry.getLoginServerUrl());
            runtime.username(registry.getUserName());
            runtime.password(registry.getPrimaryCredential());
        });
        return runtime;
    }
}
