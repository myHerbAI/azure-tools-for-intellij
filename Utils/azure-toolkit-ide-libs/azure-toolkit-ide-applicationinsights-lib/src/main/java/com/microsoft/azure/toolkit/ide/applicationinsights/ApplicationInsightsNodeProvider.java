/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.applicationinsights;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationInsightsNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Application Insights";
    private static final String ICON = AzureIcons.ApplicationInsights.MODULE.getIconPath();

    @javax.annotation.Nullable
    @Override
    public Object getRoot() {
        return Azure.az(AzureApplicationInsights.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureApplicationInsights || data instanceof ApplicationInsight;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @javax.annotation.Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureApplicationInsights) {
            return new AzServiceNode<>((AzureApplicationInsights) data)
                .withIcon(ICON)
                .withLabel(NAME)
                .withActions(ApplicationInsightsActionsContributor.SERVICE_ACTIONS)
                .addChildren(this::listApplicationInsights, (insight, insightModule) -> this.createNode(insight, insightModule, manager));
        } else if (data instanceof ApplicationInsight) {
            return new AzResourceNode<>((ApplicationInsight) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .onDoubleClicked(ResourceCommonActionsContributor.OPEN_PORTAL_URL)
                .withActions(ApplicationInsightsActionsContributor.INSIGHT_ACTIONS);
        }
        return null;
    }

    private List<ApplicationInsight> listApplicationInsights(final AzureApplicationInsights azureApplicationInsights) {
        return azureApplicationInsights.list().stream().flatMap(m -> m.getApplicationInsightsModule().list().stream()).collect(Collectors.toList());
    }
}
