/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.LogLevel;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppDockerRuntime;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppOnLinuxDeployModel;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

@Data
public class IntelliJWebAppOnLinuxDeployModel extends WebAppOnLinuxDeployModel {
    private AppServiceConfig config;

    @Nonnull
    public AppServiceConfig getConfig() {
        if (Objects.nonNull(config) && StringUtils.isNotBlank(config.appName())) {
            return config;
        }
        if (StringUtils.isAllEmpty(getWebAppId(), getWebAppName())) {
            return new AppServiceConfig();
        }
        this.config = getConfigFromDeprecatedModel();
        return this.config;
    }

    @Nonnull
    private AppServiceConfig getConfigFromDeprecatedModel() {
        final AppServiceConfig result = new AppServiceConfig();
        final ResourceId id = StringUtils.isBlank(getWebAppId()) ? null : ResourceId.fromString(getWebAppId());
        result.appName(Optional.ofNullable(id).map(ResourceId::name).orElseGet(this::getWebAppName));
        result.resourceGroup(Optional.ofNullable(id).map(ResourceId::resourceGroupName).orElseGet(this::getResourceGroupName));
        result.subscriptionId(Optional.ofNullable(id).map(ResourceId::subscriptionId).orElseGet(this::getSubscriptionId));
        Optional.ofNullable(getLocationName()).map(Region::fromName).ifPresent(result::region);
        final PricingTier pricingTier = StringUtils.isAnyEmpty(this.getPricingSkuTier(), this.getPricingSkuSize()) ? null :
                                        PricingTier.fromString(this.getPricingSkuTier(), this.getPricingSkuSize());
        Optional.ofNullable(pricingTier).ifPresent(result::pricingTier);
        final RuntimeConfig runtimeConfig = RuntimeConfig.fromRuntime(WebAppDockerRuntime.INSTANCE);
        final ContainerRegistry registry = Azure.az(AzureContainerRegistry.class).getById(getContainerRegistryId());
        Optional.ofNullable(registry).ifPresent(r -> runtimeConfig.registryUrl(registry.getLoginServerUrl())
                                                                  .image(getFinalRepositoryName() + ":" + getFinalTagName())
                                                                  .username(registry.getUserName())
                                                                  .password(registry.getPrimaryCredential()));
        result.runtime(runtimeConfig);
        Optional.ofNullable(getAppServicePlanName()).ifPresent(result::servicePlanName);
        Optional.ofNullable(getAppServicePlanResourceGroupName()).ifPresent(result::servicePlanResourceGroup);
        final DiagnosticConfig diagnosticConfig = DiagnosticConfig.builder()
                                                                  .enableApplicationLog(isEnableApplicationLog())
                                                                  .applicationLogLevel(LogLevel.fromString(this.getApplicationLogLevel()))
                                                                  .enableDetailedErrorMessage(this.isEnableDetailedErrorMessage())
                                                                  .enableFailedRequestTracing(this.isEnableFailedRequestTracing())
                                                                  .enableWebServerLogging(this.isEnableWebServerLogging())
                                                                  .webServerRetentionPeriod(this.getWebServerRetentionPeriod())
                                                                  .webServerLogQuota(this.getWebServerLogQuota()).build();
        result.diagnosticConfig(diagnosticConfig);
        return result;
    }
}
