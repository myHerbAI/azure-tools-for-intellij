/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.azure.resourcemanager.appservice.models.NetFrameworkVersion
import com.azure.resourcemanager.appservice.models.RuntimeStack
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.projectModelTasks
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem

fun PublishableProjectModel.canBePublishedToAzure() = isWeb && (isDotNetCore || SystemInfo.isWindows)

private val netCoreAppVersionRegex = Regex("\\.NETCoreApp,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
private val netAppVersionRegex = Regex("net([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
private val netFxAppVersionRegex = Regex("\\.NETFramework,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)

fun PublishableProjectModel.getStackAndVersion(
    project: Project,
    operatingSystem: OperatingSystem
): Pair<RuntimeStack?, NetFrameworkVersion?>? {
    if (operatingSystem == OperatingSystem.DOCKER) return null

    if (isDotNetCore) {
        val dotnetVersion = getProjectDotNetVersion(project, this)
        if (operatingSystem == OperatingSystem.LINUX) {
            val stack =
                if (dotnetVersion != null) RuntimeStack("DOTNETCORE", dotnetVersion)
                else RuntimeStack("DOTNETCORE", "8.0")

            return stack to null
        } else {
            val version =
                if (dotnetVersion != null) NetFrameworkVersion.fromString("v$dotnetVersion")
                else  NetFrameworkVersion.fromString("v8.0")

            return null to version
        }
    } else {
        val netFrameworkVersion = getProjectNetFrameworkVersion(project, this)
        val version =
            if (netFrameworkVersion == null)
                NetFrameworkVersion.fromString("v3.5")
            else if (netFrameworkVersion.startsWith("4"))
                NetFrameworkVersion.fromString("v4.8")
            else
                NetFrameworkVersion.fromString("v3.5")

        return null to version
    }
}

private fun getProjectDotNetVersion(project: Project, publishableProject: PublishableProjectModel): String? {
    val currentFramework = getCurrentFrameworkId(project, publishableProject) ?: return null
    return netAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value
        ?: netCoreAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value
}

private fun getProjectNetFrameworkVersion(project: Project, publishableProject: PublishableProjectModel): String? {
    val currentFramework = getCurrentFrameworkId(project, publishableProject) ?: return null
    return netFxAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value
}

private fun getCurrentFrameworkId(project: Project, publishableProject: PublishableProjectModel): String? {
    val targetFramework = project.solution.projectModelTasks.targetFrameworks[publishableProject.projectModelId]
    return targetFramework?.currentTargetFrameworkId?.valueOrNull?.framework?.id
}
