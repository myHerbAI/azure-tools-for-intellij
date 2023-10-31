/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.FunctionDeploymentConfigurationFactory

class AzureFunctionConfigurationType : ConfigurationTypeBase(
    "AzureFunctionAppPublish",
    "Azure - Function App",
    "Azure Publish Function App configuration",
    IntelliJAzureIcons.getIcon(AzureIcons.FunctionApp.MODULE)
) {
    init {
        addFactory(FunctionDeploymentConfigurationFactory(this))
    }
}