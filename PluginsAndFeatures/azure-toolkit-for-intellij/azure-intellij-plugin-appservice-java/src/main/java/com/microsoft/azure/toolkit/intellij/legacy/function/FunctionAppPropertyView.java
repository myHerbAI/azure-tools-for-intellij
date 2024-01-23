/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppBasePropertyView;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDraft;
import com.microsoft.azuretools.core.mvp.ui.webapp.WebAppProperty;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

public class FunctionAppPropertyView extends WebAppBasePropertyView {
    public static final String KEY_ENVIRONMENT = "environment";
    private static final String ID = "com.microsoft.azure.toolkit.intellij.function.FunctionAppPropertyView";

    public static WebAppBasePropertyView create(@Nonnull final Project project, @Nonnull final String sid,
                                                @Nonnull final String webAppId, @Nonnull final VirtualFile virtualFile) {
        final FunctionAppPropertyView view = new FunctionAppPropertyView(project, sid, webAppId, virtualFile);
        view.onLoadWebAppProperty(sid, webAppId, null);
        return view;
    }

    protected FunctionAppPropertyView(@Nonnull Project project, @Nonnull String sid, @Nonnull String resId, @Nonnull final VirtualFile virtualFile) {
        super(project, sid, resId, null, virtualFile);
    }

    @Override
    protected String getId() {
        return ID;
    }

    @Override
    public void showProperty(WebAppProperty webAppProperty) {
        super.showProperty(webAppProperty);
        final Object value = webAppProperty.getValue(KEY_ENVIRONMENT);
        if (value instanceof String env && StringUtils.isNotBlank(env)) {
            lblServicePlan.setText("Environment:");
            txtAppServicePlan.setText(env);
        } else {
            lblServicePlan.setText("App Service Plan:");
        }
    }

    @Override
    protected WebAppBasePropertyViewPresenter<FunctionAppPropertyView, FunctionApp> createPresenter() {
        return new WebAppBasePropertyViewPresenter<>() {
            @Override
            protected FunctionApp getWebAppBase(String subscriptionId, String functionAppId, String name) {
                return Azure.az(AzureFunctions.class).functionApp(functionAppId);
            }

            @Override
            protected void updateAppSettings(String subscriptionId, String functionAppId, String name, Map toUpdate, Set toRemove) {
                final FunctionApp functionApp = getWebAppBase(subscriptionId, functionAppId, name);
                final FunctionAppDraft draft = (FunctionAppDraft) functionApp.update();
                draft.setAppSettings(toUpdate);
                toRemove.forEach(key -> draft.removeAppSetting((String) key));
                draft.updateIfExist();
            }

            @Override
            protected void updateHostConfiguration(@Nonnull FunctionApp appService, Map<String, Object> propertyMap) {
                if (StringUtils.isNotBlank(appService.getEnvironmentId())) {
                    final ResourceId envId = ResourceId.fromString(appService.getEnvironmentId());
                    propertyMap.put(KEY_ENVIRONMENT, envId.name());
                } else {
                    super.updateHostConfiguration(appService, propertyMap);
                }
            }
        };
    }
}
