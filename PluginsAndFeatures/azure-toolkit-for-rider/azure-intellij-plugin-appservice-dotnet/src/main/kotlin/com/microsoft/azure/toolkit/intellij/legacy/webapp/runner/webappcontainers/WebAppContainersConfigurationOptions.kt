package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

class WebAppContainersConfigurationOptions : LocatableRunConfigurationOptions() {
    var resourceId by string()
    var webAppName by string()
    var subscriptionId by string()
    var resourceGroupName by string()
    var region by string()
    var appServicePlanName by string()
    var appServicePlanResourceGroupName by string()
    var pricingTier by string()
    var pricingSize by string()
    var imageRepository by string()
    var imageTag by string()
    var port by property(80)
}