/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactType;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.LogLevel;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.*;

@Data
public class IntelliJWebAppSettingModel extends WebAppSettingModel {
	private String appSettingsKey = UUID.randomUUID().toString();
	private Map<String, String> appSettings;
	private Set<String> appSettingsToRemove;
	private AzureArtifactType azureArtifactType;
	private boolean openBrowserAfterDeployment = true;
	private boolean slotPanelVisible = false;
	private String artifactIdentifier;
	private String packaging;

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
	public AppServiceConfig getConfigFromDeprecatedModel() {
		final AppServiceConfig result = new AppServiceConfig();
		final ResourceId id = StringUtils.isBlank(getWebAppId()) ? null : ResourceId.fromString(getWebAppId());
		result.appName(Optional.ofNullable(id).map(ResourceId::name).orElseGet(this::getWebAppName));
		result.resourceGroup(Optional.ofNullable(id).map(ResourceId::resourceGroupName).orElseGet(this::getResourceGroup));
		result.subscriptionId(Optional.ofNullable(id).map(ResourceId::subscriptionId).orElseGet(this::getSubscriptionId));
		Optional.ofNullable(getRegion()).filter(StringUtils::isNotBlank).map(Region::fromName).ifPresent(result::region);
		Optional.ofNullable(getRuntime()).map(RuntimeConfig::fromRuntime).ifPresent(result::runtime);
		Optional.ofNullable(getPricing()).filter(StringUtils::isNotBlank).map(PricingTier::fromString).ifPresent(result::pricingTier);
		Optional.ofNullable(getAppServicePlanName()).filter(StringUtils::isNotBlank).ifPresent(result::servicePlanName);
		Optional.ofNullable(getAppServicePlanResourceGroupName()).filter(StringUtils::isNotBlank).ifPresent(result::servicePlanResourceGroup);

		if (isDeployToSlot()) {
			Optional.ofNullable(getSlotName()).filter(StringUtils::isNotBlank).ifPresent(result::deploymentSlotName);
			Optional.ofNullable(getNewSlotConfigurationSource()).filter(StringUtils::isNotBlank).ifPresent(result::deploymentSlotConfigurationSource);
		}

		final DiagnosticConfig diagnosticConfig = DiagnosticConfig.builder().enableApplicationLog(isEnableApplicationLog())
																  .applicationLogLevel(LogLevel.fromString(getApplicationLogLevel()))
																  .enableDetailedErrorMessage(isEnableDetailedErrorMessage())
																  .enableFailedRequestTracing(isEnableFailedRequestTracing())
																  .enableWebServerLogging(isEnableWebServerLogging())
																  .webServerRetentionPeriod(getWebServerRetentionPeriod())
																  .webServerLogQuota(getWebServerLogQuota()).build();
		result.diagnosticConfig(diagnosticConfig);
		return result;
	}
}
