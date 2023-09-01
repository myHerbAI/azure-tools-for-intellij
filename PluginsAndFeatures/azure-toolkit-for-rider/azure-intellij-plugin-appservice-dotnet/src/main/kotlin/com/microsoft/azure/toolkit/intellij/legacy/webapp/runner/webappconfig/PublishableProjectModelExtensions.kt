package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rider.model.PublishableProjectModel

fun PublishableProjectModel.canBePublishedToAzure() = isWeb && (isDotNetCore || SystemInfo.isWindows)