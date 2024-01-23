/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy;

import com.microsoft.azure.toolkit.ide.appservice.model.MonitorConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunctionDeployModel {
    private String appSettingsKey;
    private String appSettingsHash;
    private String hostJsonPath;
    private String deploymentStagingDirectoryPath;
    private String moduleName;

    private com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig functionAppConfig =
        com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig.builder().build();
    private FunctionAppConfig config = new FunctionAppConfig();

    @Nonnull
    public FunctionAppConfig getConfig() {
        if (Objects.isNull(this.config)) {
            this.config = Objects.isNull(functionAppConfig) ? new FunctionAppConfig() : getConfigFromIDEConfig(functionAppConfig);
        }
        return this.config;
    }

    @Nonnull
    private FunctionAppConfig getConfigFromIDEConfig(@Nonnull com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig config) {
        final FunctionAppConfig result = new FunctionAppConfig();
        Optional.ofNullable(config.getSubscription()).map(Subscription::getId).ifPresent(result::subscriptionId);
        Optional.ofNullable(config.getName()).ifPresent(result::subscriptionId);
        Optional.ofNullable(config.getResourceGroupName()).ifPresent(result::resourceGroup);
        Optional.ofNullable(config.getRegion()).ifPresent(result::region);
        Optional.ofNullable(config.getPricingTier()).ifPresent(result::pricingTier);
        Optional.ofNullable(config.getRuntime()).map(RuntimeConfig::fromRuntime).ifPresent(result::runtime);
        Optional.ofNullable(config.getServicePlan()).map(AppServicePlanConfig::getName).ifPresent(result::servicePlanName);
        Optional.ofNullable(config.getServicePlan()).map(AppServicePlanConfig::getResourceGroupName).ifPresent(result::servicePlanResourceGroup);
        Optional.ofNullable(config.getAppSettings()).ifPresent(result::appSettings);
        Optional.ofNullable(config.getMonitorConfig()).map(MonitorConfig::getApplicationInsightsConfig).ifPresent(result::applicationInsightsConfig);
        Optional.ofNullable(config.getMonitorConfig()).map(MonitorConfig::getDiagnosticConfig).ifPresent(result::diagnosticConfig);
        Optional.ofNullable(config.getFlexConsumptionConfiguration()).ifPresent(result::flexConsumptionConfiguration);
        return result;
    }

    public Map<String, String> getTelemetryProperties() {
        return Collections.emptyMap();
    }
}
