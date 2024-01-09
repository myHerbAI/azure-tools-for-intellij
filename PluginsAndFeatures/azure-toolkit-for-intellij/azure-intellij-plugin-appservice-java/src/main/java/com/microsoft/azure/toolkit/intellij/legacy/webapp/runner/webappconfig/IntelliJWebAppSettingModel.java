/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig;

import com.microsoft.azure.toolkit.intellij.common.AzureArtifactType;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.LogLevel;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
		if (Objects.nonNull(config)) {
			return config;
		}
		if (StringUtils.isAllEmpty(getWebAppId(), getWebAppName())) {
			return new AppServiceConfig();
		}
		final WebApp app = isCreatingNew() || StringUtils.isBlank(getWebAppId()) ? null : Azure.az(AzureWebApp.class).getById(getWebAppId());
		this.config = Optional.ofNullable(app)
							  .map(a -> AppServiceConfigUtils.fromAppService(app, Objects.requireNonNull(app.getAppServicePlan())))
							  .orElseGet(AppServiceConfig::new);
		this.config.appName(getWebAppName());
		this.config.resourceGroup(getResourceGroup());
		this.config.region(StringUtils.isEmpty(getRegion()) ? null : Region.fromName(getRegion()));
		this.config.runtime(Optional.ofNullable(getRuntime()).map(RuntimeConfig::fromRuntime).orElse(null));
		this.config.pricingTier(StringUtils.isEmpty(getPricing()) ? null : PricingTier.fromString(getPricing()));
		this.config.servicePlanName(getAppServicePlanName());
		this.config.servicePlanResourceGroup(getAppServicePlanResourceGroupName());
		this.config.deploymentSlotName(isDeployToSlot() ? getSlotName() : null);
		this.config.deploymentSlotConfigurationSource(isDeployToSlot() ? getNewSlotConfigurationSource() : null);

		final DiagnosticConfig diagnosticConfig = DiagnosticConfig.builder().enableApplicationLog(isEnableApplicationLog())
																  .applicationLogLevel(LogLevel.fromString(getApplicationLogLevel()))
																  .enableDetailedErrorMessage(isEnableDetailedErrorMessage())
																  .enableFailedRequestTracing(isEnableFailedRequestTracing())
																  .enableWebServerLogging(isEnableWebServerLogging())
																  .webServerRetentionPeriod(getWebServerRetentionPeriod())
																  .webServerLogQuota(getWebServerLogQuota()).build();
		this.config.diagnosticConfig(diagnosticConfig);
		return this.config;
	}
}
