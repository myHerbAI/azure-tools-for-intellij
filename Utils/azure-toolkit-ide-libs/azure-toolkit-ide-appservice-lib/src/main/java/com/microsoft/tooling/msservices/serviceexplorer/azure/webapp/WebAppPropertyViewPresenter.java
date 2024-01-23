/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDraft;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class WebAppPropertyViewPresenter extends WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView, WebApp> {

    @Override
    protected WebApp getWebAppBase(@Nonnull final String sid, @Nonnull final String appId,
                                   @Nullable final String slotName) {
        return Azure.az(AzureWebApp.class).webApp(appId);
    }

    @Override
    protected void updateAppSettings(@Nonnull String sid, @Nonnull String webAppId, @Nullable String name, @Nonnull Map toUpdate, @Nonnull Set toRemove) throws Exception {
        final WebApp webApp = getWebAppBase(sid, webAppId, name);
        final WebAppDraft draft = (WebAppDraft) webApp.update();
        draft.setAppSettings(toUpdate);
        toRemove.forEach(s -> draft.removeAppSetting((String) s));
        draft.updateIfExist();
    }

}
