/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.help;

import com.intellij.openapi.help.WebHelpProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class AzureWebHelpProvider extends WebHelpProvider {
    public static final String HELP_SIGN_IN = "com.microsoft.tooling.msservices.intellij.azure.sign_in";
    public static final String HELP_AZURE_WEB_JOBS_STORAGE = "com.microsoft.tooling.msservices.intellij.azure.azure_web_jobs_storage";

    private static final Map<String, String> HELP_URLS = Map.of(
        AzureWebHelpProvider.HELP_SIGN_IN, "https://docs.microsoft.com/en-us/azure/azure-toolkit-for-intellij-sign-in-instructions",
        AzureWebHelpProvider.HELP_AZURE_WEB_JOBS_STORAGE, "https://learn.microsoft.com/en-us/azure/azure-functions/functions-app-settings#azurewebjobsstorage"
    );

    @Override
    @Nullable
    public String getHelpPageUrl(@Nonnull String helpTopicId) {
        return HELP_URLS.get(helpTopicId);
    }
}
