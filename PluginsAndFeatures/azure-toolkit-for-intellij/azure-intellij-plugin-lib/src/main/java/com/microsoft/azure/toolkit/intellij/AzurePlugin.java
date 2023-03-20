/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij;

import com.intellij.openapi.diagnostic.Logger;

public class AzurePlugin {
    public static final String PLUGIN_ID = "com.microsoft.tooling.msservices.intellij.azure";
    public static final String PLUGIN_NAME = "azure-toolkit-for-intellij";
    private static final Logger LOG = Logger.getInstance("#com.microsoft.intellij.AzurePlugin");
    public static void log(String message, Throwable ex) {
        LOG.error(message, ex);
    }
}
