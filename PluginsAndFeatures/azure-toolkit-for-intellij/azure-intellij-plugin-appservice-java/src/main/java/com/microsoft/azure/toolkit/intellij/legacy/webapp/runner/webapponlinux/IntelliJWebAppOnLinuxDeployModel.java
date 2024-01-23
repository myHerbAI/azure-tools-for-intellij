/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.LogLevel;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppDockerRuntime;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
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
        if (Objects.isNull(this.config)) {
            this.config = getConfigFromDeprecatedModel();
        }
        return this.config;
    }

    @Nonnull
    private AppServiceConfig getConfigFromDeprecatedModel() {
        if (StringUtils.isAllEmpty(getWebAppId(), getWebAppName())) {
            return new AppServiceConfig();
        }
        final WebApp app = this.isCreatingNewWebAppOnLinux() || StringUtils.isBlank(getWebAppId()) ? null : Azure.az(AzureWebApp.class).getById(getWebAppId());
        final AppServiceConfig result = Optional.ofNullable(app)
                                                .map(a -> AppServiceConfigUtils.fromAppService(app, Objects.requireNonNull(app.getAppServicePlan())))
                                                .orElseGet(AppServiceConfig::new);
        Optional.ofNullable(getSubscriptionId()).ifPresent(result::subscriptionId);
        Optional.ofNullable(getWebAppName()).ifPresent(result::appName);
        Optional.ofNullable(getResourceGroupName()).ifPresent(result::resourceGroup);
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
