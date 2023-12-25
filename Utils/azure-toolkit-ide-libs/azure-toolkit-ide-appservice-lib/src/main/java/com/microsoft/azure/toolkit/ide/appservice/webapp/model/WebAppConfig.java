/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.webapp.model;

import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.Azure.az;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class WebAppConfig extends AppServiceConfig {
    public static final Runtime DEFAULT_RUNTIME = WebAppRuntime.DEFAULT_JAVASE_RUNTIME;
    public static final PricingTier DEFAULT_PRICING_TIER = PricingTier.BASIC_B2;
    @Builder.Default
    protected Runtime runtime = DEFAULT_RUNTIME;

    public static WebAppConfig getWebAppDefaultConfig() {
        return getWebAppDefaultConfig(StringUtils.EMPTY);
    }

    public static WebAppConfig getWebAppDefaultConfig(final String name) {
        final String namePrefix = StringUtils.isEmpty(name) ? "app" : String.format("app-%s", name);
        final String appName = Utils.generateRandomResourceName(namePrefix, APP_SERVICE_NAME_MAX_LENGTH);
        final List<Subscription> subs = Azure.az(IAzureAccount.class).account().getSelectedSubscriptions();

        final Subscription historySub = CacheManager.getUsageHistory(Subscription.class).peek(subs::contains);
        final Subscription sub = Optional.ofNullable(historySub).orElseGet(() -> subs.stream().findFirst().orElse(null));

        final List<Region> regions = az(AzureAccount.class).listRegions(sub.getId());
        final Region historyRegion = CacheManager.getUsageHistory(Region.class).peek(regions::contains);
        final Region region = Optional.ofNullable(historyRegion).orElseGet(AppServiceConfig::getDefaultRegion);

        final String rgName = Utils.generateRandomResourceName(String.format("rg-%s", namePrefix), RG_NAME_MAX_LENGTH);
        final ResourceGroup historyRg = CacheManager.getUsageHistory(ResourceGroup.class)
            .peek(r -> r.getSubscriptionId().equals(sub.getId()));
        final Subscription subscription = Optional.of(sub).orElseGet(() -> Optional.ofNullable(historyRg).map(AzResource::getSubscription).orElse(null));
        final ResourceGroupConfig group = Optional.ofNullable(historyRg).map(ResourceGroupConfig::fromResource).orElseGet(() -> ResourceGroupConfig.builder().subscriptionId(sub.getId()).name(rgName).region(region).build());

        final Runtime historyRuntime = CacheManager.getUsageHistory(Runtime.class).peek(runtime -> WebAppRuntime.getMajorRuntimes().contains(runtime));
        final Runtime runtime = Optional.ofNullable(historyRuntime).orElse(WebAppConfig.DEFAULT_RUNTIME);

        final PricingTier historyPricingTier = CacheManager.getUsageHistory(PricingTier.class).peek();
        final PricingTier pricingTier = Optional.ofNullable(historyPricingTier).orElse(WebAppConfig.DEFAULT_PRICING_TIER);

        final String planName = Utils.generateRandomResourceName(String.format("sp-%s", namePrefix), SP_NAME_MAX_LENGTH);
        final AppServicePlan historyPlan = CacheManager.getUsageHistory(AppServicePlan.class).peek();
        final AppServicePlanConfig plan = Optional.ofNullable(historyPlan)
            .filter(p -> p.getSubscriptionId().equals(subscription.getId()))
            .filter(p -> p.getResourceGroupName().equals(group.getName()))
            .filter(p -> p.getOperatingSystem() == runtime.getOperatingSystem())
            .map(AppServicePlanConfig::fromResource)
            .orElseGet(() -> AppServicePlanConfig.builder()
                .subscriptionId(subscription.getId())
                .resourceGroupName(rgName)
                .name(planName)
                .region(region)
                .os(runtime.getOperatingSystem())
                .pricingTier(pricingTier).build());
        return WebAppConfig.builder()
                .subscription(sub)
                .resourceGroup(group)
                .name(appName)
                .servicePlan(plan)
                .runtime(runtime)
                .pricingTier(pricingTier)
                .region(region).build();
    }

    public static com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig convertToTaskConfig(WebAppConfig config) {
        final com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig result =
                new com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig();
        result.appName(config.getName());
        result.resourceGroup(config.getResourceGroupName());
        result.subscriptionId(config.getSubscriptionId());
        result.pricingTier(Optional.ofNullable(config.getServicePlan()).map(AppServicePlanConfig::getPricingTier).orElseGet(config::getPricingTier));
        result.region(config.getRegion());
        result.servicePlanName(Optional.ofNullable(config.getServicePlan()).map(AppServicePlanConfig::getName).orElse(null));
        result.servicePlanResourceGroup(Optional.ofNullable(config.getServicePlan())
            .map(AppServicePlanConfig::getResourceGroupName).orElseGet(config::getResourceGroupName));
        Optional.ofNullable(config.getRuntime()).map(r-> ((WebAppRuntime) r)).ifPresent(runtime -> result.runtime(new RuntimeConfig()
            .os(runtime.getOperatingSystem()).webContainer(runtime.getContainerUserText()).javaVersion(runtime.getJavaVersionUserText())
        ));
        return result;
    }
}
