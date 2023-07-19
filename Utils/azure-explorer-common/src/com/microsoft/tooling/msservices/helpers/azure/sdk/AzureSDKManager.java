/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.helpers.azure.sdk;

import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.applicationinsights.task.GetOrCreateApplicationInsightsTask;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceConfig;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

@Slf4j
public class AzureSDKManager {

    private static final String INSIGHTS_REGION_LIST_URL = "https://management.azure.com/providers/microsoft.insights?api-version=2015-05-01";

    public static List<ApplicationInsight> getInsightsResources(@Nonnull String subscriptionId) {
        return com.microsoft.azure.toolkit.lib.Azure.az(AzureApplicationInsights.class).applicationInsights(subscriptionId).list();
    }

    public static List<ApplicationInsight> getInsightsResources(@Nonnull Subscription subscription) {
        return getInsightsResources(subscription.getId());
    }

    // SDK will return existing application insights component when you create new one with existing name
    // Use this method in case SDK service update their behavior
    public static ApplicationInsight getOrCreateApplicationInsights(@Nonnull String subscriptionId,
                                                                    @Nonnull String resourceGroupName,
                                                                    @Nonnull String resourceName,
                                                                    @Nonnull String location,
                                                                    @Nonnull LogAnalyticsWorkspaceConfig workspaceConfig) throws IOException {
        return new GetOrCreateApplicationInsightsTask(subscriptionId, resourceGroupName, Region.fromName(location), resourceName, workspaceConfig).execute();
    }

    public static ApplicationInsight createInsightsResource(@Nonnull String subscriptionId,
                                                                      @Nonnull String resourceGroupName,
                                                                      @Nonnull String resourceName,
                                                                      @Nonnull String location,
                                                                      @Nonnull LogAnalyticsWorkspaceConfig workspaceConfig) throws IOException {
        return getOrCreateApplicationInsights(subscriptionId, resourceGroupName, resourceName, location, workspaceConfig);
    }

    public static ApplicationInsight createInsightsResource(@Nonnull Subscription subscription,
                                                                      @Nonnull String resourceGroupName,
                                                                      boolean isNewGroup,
                                                                      @Nonnull String resourceName,
                                                                      @Nonnull String location,
                                                                      @Nonnull LogAnalyticsWorkspaceConfig workspaceConfig) throws IOException {
        return createInsightsResource(subscription.getId(), resourceGroupName, resourceName, location, workspaceConfig);
    }

}
