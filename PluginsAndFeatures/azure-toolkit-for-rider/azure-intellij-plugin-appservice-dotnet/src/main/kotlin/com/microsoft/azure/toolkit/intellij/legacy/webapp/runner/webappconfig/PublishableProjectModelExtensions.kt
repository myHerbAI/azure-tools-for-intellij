package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.projectModelTasks
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer

fun PublishableProjectModel.canBePublishedToAzure() = isWeb && (isDotNetCore || SystemInfo.isWindows)

private val netCoreAppVersionRegex = Regex("\\.NETCoreApp,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
private val netAppVersionRegex = Regex("net([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
private val netFxAppVersionRegex = Regex("\\.NETFramework,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)

private fun getCurrentFrameworkId(project: Project, publishableProject: PublishableProjectModel): String? {
    val targetFramework = project.solution.projectModelTasks.targetFrameworks[publishableProject.projectModelId]
    return targetFramework?.currentTargetFrameworkId?.valueOrNull?.framework?.id
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

fun PublishableProjectModel.toRuntime(project: Project, operatingSystem: OperatingSystem): Runtime? {
    if (operatingSystem == OperatingSystem.DOCKER) return null

    if (isDotNetCore) {
        val dotnetVersion = getProjectDotNetVersion(project, this)

        if (operatingSystem == OperatingSystem.LINUX) {
            return if (dotnetVersion != null) Runtime(operatingSystem, WebContainer("DOTNETCORE $dotnetVersion"), null)
            else Runtime(operatingSystem, WebContainer("DOTNETCORE 7.0"), null)
        } else if (operatingSystem == OperatingSystem.WINDOWS) {
            return if (dotnetVersion != null) Runtime(operatingSystem, WebContainer("dotnet $dotnetVersion"), null)
            else Runtime(operatingSystem, WebContainer("dotnet 7"), null)
        }
    } else {
        val netFrameworkVersion = getProjectNetFrameworkVersion(project, this)
                ?: return Runtime(OperatingSystem.WINDOWS, WebContainer("ASPNET V3.5"), null)

        return if (netFrameworkVersion.startsWith("4"))
            Runtime(OperatingSystem.WINDOWS, WebContainer("ASPNET V4.8"), null)
        else
            Runtime(OperatingSystem.WINDOWS, WebContainer("ASPNET V3.5"), null)
    }

    return null
}