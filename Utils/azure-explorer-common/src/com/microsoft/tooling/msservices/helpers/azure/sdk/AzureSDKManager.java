/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.helpers.azure.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.applicationinsights.task.GetOrCreateApplicationInsightsTask;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceConfig;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
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

    public static List<String> getLocationsForInsights(String subscriptionId) throws IOException {
        final HttpGet request = new HttpGet(INSIGHTS_REGION_LIST_URL);
        final Subscription subscription = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account().getSubscription(subscriptionId);
        final String accessToken = IdeAzureAccount.getInstance().getAccessTokenForTrack1(subscription.getTenantId());
        request.setHeader("Authorization", String.format("Bearer %s", accessToken));
        final CloseableHttpResponse response = HttpClients.createDefault().execute(request);
        final InputStream responseStream = response.getEntity().getContent();
        try (final InputStreamReader isr = new InputStreamReader(responseStream)) {
            final Gson gson = new Gson();
            final JsonObject jsonContent = (gson).fromJson(isr, JsonObject.class);
            final JsonArray jsonResourceTypes = jsonContent.getAsJsonArray("resourceTypes");
            for (int i = 0; i < jsonResourceTypes.size(); ++i) {
                final Object obj = jsonResourceTypes.get(i);
                if (obj instanceof JsonObject) {
                    final JsonObject jsonResourceType = (JsonObject) obj;
                    final String resourceType = jsonResourceType.get("resourceType").getAsString();
                    if (resourceType.equalsIgnoreCase("components")) {
                        final JsonArray jsonLocations = jsonResourceType.getAsJsonArray("locations");
                        return gson.fromJson(jsonLocations, new ArrayList().getClass());
                    }
                }
            }
        } catch (final IOException | JsonParseException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    // TODO: AI SDK doesn't provide a method to list regions which are available regions to create AI,
    // we are requiring the SDK to provide that API, before SDK side fix, we will use our own impl
    public static List<String> getLocationsForInsights(Subscription subscription) throws IOException {
        return getLocationsForInsights(subscription.getId());
    }
}
