/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.applicationinsights;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.common.action.*;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class ApplicationInsightsActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.applicationinsights.service";
    public static final String INSIGHT_ACTIONS = "actions.applicationinsights.management";

    public static final Action.Id<ApplicationInsight> OPEN_APPLICATION_MAP = Action.Id.of("ai.open_application_map.ai");
    public static final Action.Id<ApplicationInsight> OPEN_LIVE_METRICS = Action.Id.of("ai.open_live_metrics.ai");
    public static final Action.Id<ApplicationInsight> OPEN_TRANSACTION_SEARCH = Action.Id.of("ai.open_transaction_search.ai");
    public static final Action.Id<ApplicationInsight> COPY_INSTRUMENTATION_KEY = Action.Id.of("ai.copy_instrumentation_key.ai");
    public static final Action.Id<ApplicationInsight> COPY_CONNECTION_STRING = Action.Id.of("ai.copy_connection_string.ai");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_APPLICATIONINSIGHT = Action.Id.of("ai.create_ai.group");

    @Override
    public void registerActions(AzureActionManager am) {
        final Consumer<ApplicationInsight> copyConnectionStringConsumer = insight -> {
            am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(insight.getConnectionString());
            AzureMessager.getMessager().info("Connection string copied");
        };
        final ActionView.Builder copyConnectionStringView = new ActionView.Builder("Copy Connection String")
                .title(s -> Optional.ofNullable(s).map(r -> description("ai.copy_connection_string.ai",
                        ((ApplicationInsight) r).getName())).orElse(null))
                .enabled(s -> s instanceof ApplicationInsight && ((ApplicationInsight) s).getFormalStatus().isConnected());
        am.registerAction(new Action<>(COPY_CONNECTION_STRING, copyConnectionStringConsumer, copyConnectionStringView));

        final Consumer<ApplicationInsight> copyInstrumentationKeyConsumer = insight -> {
            am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(insight.getInstrumentationKey());
            AzureMessager.getMessager().info("Instrumentation key copied");
        };
        final ActionView.Builder copyInstrumentationKeyView = new ActionView.Builder("Copy Instrumentation Key")
                .title(s -> Optional.ofNullable(s).map(r -> description("ai.copy_instrumentation_key.ai",
                        ((ApplicationInsight) r).getName())).orElse(null))
                .enabled(s -> s instanceof ApplicationInsight && ((ApplicationInsight) s).getFormalStatus().isConnected());
        am.registerAction(new Action<>(COPY_INSTRUMENTATION_KEY, copyInstrumentationKeyConsumer, copyInstrumentationKeyView));

        final Consumer<ApplicationInsight> applicationMapConsumer = insight -> am.getAction(ResourceCommonActionsContributor.OPEN_URL)
                .handle(insight.getPortalUrl() + "/applicationMap");
        final ActionView.Builder applicationMapView = new ActionView.Builder("Open Application Map")
                .title(s -> Optional.ofNullable(s).map(r -> description("ai.open_application_map.ai",
                        ((ApplicationInsight) r).getName())).orElse(null))
                .enabled(s -> s instanceof ApplicationInsight && ((ApplicationInsight) s).getFormalStatus().isConnected());
        am.registerAction(new Action<>(OPEN_APPLICATION_MAP, applicationMapConsumer, applicationMapView));

        final Consumer<ApplicationInsight> liveMetricsConsumer = insight -> am.getAction(ResourceCommonActionsContributor.OPEN_URL)
                .handle(insight.getPortalUrl() + "/quickPulse");
        final ActionView.Builder liveMetricsView = new ActionView.Builder("Open Live Metrics")
                .title(s -> Optional.ofNullable(s).map(r -> description("ai.open_live_metrics.ai",
                        ((ApplicationInsight) r).getName())).orElse(null))
                .enabled(s -> s instanceof ApplicationInsight && ((ApplicationInsight) s).getFormalStatus().isConnected());
        am.registerAction(new Action<>(OPEN_LIVE_METRICS, liveMetricsConsumer, liveMetricsView));

        final Consumer<ApplicationInsight> transactionSearchConsumer = insight -> am.getAction(ResourceCommonActionsContributor.OPEN_URL)
                .handle(insight.getPortalUrl() + "/searchV1");
        final ActionView.Builder transactionSearchView = new ActionView.Builder("Open Transaction Search")
                .title(s -> Optional.ofNullable(s).map(r -> description("ai.open_transaction_search.ai",
                        ((ApplicationInsight) r).getName())).orElse(null))
                .enabled(s -> s instanceof ApplicationInsight && ((ApplicationInsight) s).getFormalStatus().isConnected());
        am.registerAction(new Action<>(OPEN_TRANSACTION_SEARCH, transactionSearchConsumer, transactionSearchView));

        final ActionView.Builder createInsightView = new ActionView.Builder("Application Insights")
                .title(s -> Optional.ofNullable(s).map(r ->
                        description("ai.create_ai.group", ((ResourceGroup) r).getName())).orElse(null))
                .enabled(s -> s instanceof ResourceGroup && ((ResourceGroup) s).getFormalStatus().isConnected());
        am.registerAction(new Action<>(GROUP_CREATE_APPLICATIONINSIGHT, createInsightView));
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                "---",
                ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup accountActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                "---",
                ApplicationInsightsActionsContributor.COPY_CONNECTION_STRING,
                ApplicationInsightsActionsContributor.COPY_INSTRUMENTATION_KEY,
                "---",
                ResourceCommonActionsContributor.CONNECT,
                ResourceCommonActionsContributor.DELETE,
                "---",
                ApplicationInsightsActionsContributor.OPEN_APPLICATION_MAP,
                ApplicationInsightsActionsContributor.OPEN_LIVE_METRICS,
                ApplicationInsightsActionsContributor.OPEN_TRANSACTION_SEARCH
        );
        am.registerGroup(INSIGHT_ACTIONS, accountActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_APPLICATIONINSIGHT);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
