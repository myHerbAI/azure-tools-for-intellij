/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlotDraft;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppBasePropertyMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DeploymentSlotPropertyViewPresenter extends WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView, WebAppDeploymentSlot> {
    @Override
    protected void updateAppSettings(@Nonnull final String sid, @Nonnull final String webAppId,
                                     @Nullable final String name, final Map toUpdate,
                                     final Set toRemove) {
        final WebAppDeploymentSlot slot = getWebAppBase(sid, webAppId, name);
        final WebAppDeploymentSlotDraft draft = (WebAppDeploymentSlotDraft) slot.update();
        draft.setAppSettings(toUpdate);
        toRemove.forEach(key -> draft.removeAppSetting((String) key));
        draft.updateIfExist();
    }

    @Override
    protected WebAppDeploymentSlot getWebAppBase(@Nonnull final String sid, @Nonnull final String appId,
                                                  @Nullable final String slotName) {
        final WebApp webApp = Azure.az(AzureWebApp.class).webApp(appId);
        return Objects.requireNonNull(webApp).slots().get(slotName, webApp.getResourceGroupName());
    }
}
