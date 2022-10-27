/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner;

import com.microsoft.azure.toolkit.intellij.legacy.function.runner.library.IFunctionContext;
import com.microsoft.azure.toolkit.lib.common.IProject;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class IntelliJFunctionContext implements IFunctionContext {

    private Map<Class<? extends Object>, Object> providerMap = new HashMap<>();

    private String subscription;

    private String resourceGroup;

    private String appName;

    private String region;

    private String pricingTier;

    private String appServicePlanResourceGroup;

    private String appServicePlanName;

    private String deploymentStagingDirectoryPath;

    private String deployment;

    // todo: remove app settings and related codes
    @Deprecated
    private Map<String, String> appSettings = new HashMap<>();

    private String appSettingsKey = UUID.randomUUID().toString();

    private String moduleName;

    private String insightsName;

    private String instrumentationKey;

    private String os;

    private String javaVersion;

    @Override
    public IntelliJFunctionRuntimeConfiguration getRuntime() {
        IntelliJFunctionRuntimeConfiguration result = new IntelliJFunctionRuntimeConfiguration();
        result.setJavaVersion(javaVersion);
        result.setOs(os);
        return result;
    }

    public void saveRuntime(IntelliJFunctionRuntimeConfiguration runtime) {
        setOs(runtime.getOs());
        setJavaVersion(runtime.getJavaVersion());
    }

    @Override
    public String getDeploymentType() {
        return "";
    }

    @Override
    public IProject getProject() {
        return null;
    }

    public Map<String, String> getTelemetryProperties() {
        final HashMap result = new HashMap();
        try {
            result.put("runtime", this.getOs());
            result.put("subscriptionId", this.getSubscription());
            result.put("region", this.getRegion());
            result.put("functionJavaVersion", this.getJavaVersion());
        } catch (Exception e) {
            // swallow exception as telemetry should not break users operation
        }
        return result;
    }
}
