/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DialogTitleCapitalization")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunConfigurationBase
import com.microsoft.azure.toolkit.intellij.legacy.utils.isValidResourceName
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppOnLinuxDeployModel

class WebAppContainersConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
        LocatableConfigurationBase<WebAppContainersConfigurationOptions>(project, factory, name) {

    companion object {
        private const val REPO_COMPONENT_REGEX_PATTERN = "[a-z0-9]+(?:[._-][a-z0-9]+)*"
        private const val TAG_REGEX_PATTERN = "^\\w+[\\w.-]*\$"
    }

//    private val webAppContainersModel = WebAppOnLinuxDeployModel()

    private val repoComponentRegex = Regex(REPO_COMPONENT_REGEX_PATTERN)
    private val tagRegex = Regex(TAG_REGEX_PATTERN)

//    var webAppId: String?
//        get() = webAppContainersModel.webAppId
//        set(value) {
//            webAppContainersModel.webAppId = value
//        }
//    var webAppName: String
//        get() = webAppContainersModel.webAppName
//        set(value) {
//            webAppContainersModel.webAppName = value
//        }
//    var subscriptionId: String
//        get() = webAppContainersModel.subscriptionId
//        set(value) {
//            webAppContainersModel.subscriptionId = value
//        }
//    var locationName: String
//        get() = webAppContainersModel.locationName
//        set(value) {
//            webAppContainersModel.locationName = value
//        }
//    var resourceGroupName: String
//        get() = webAppContainersModel.resourceGroupName
//        set(value) {
//            webAppContainersModel.resourceGroupName = value
//        }
//    var pricingSkuTier: String?
//        get() = webAppContainersModel.pricingSkuTier
//        set(value) {
//            webAppContainersModel.pricingSkuTier = value
//        }
//    var pricingSkuSize: String
//        get() = webAppContainersModel.pricingSkuSize
//        set(value) {
//            webAppContainersModel.pricingSkuSize = value
//        }
//    var appServicePlanName: String?
//        get() = webAppContainersModel.appServicePlanName
//        set(value) {
//            webAppContainersModel.appServicePlanName = value
//        }
//    var appServicePlanResourceGroupName: String?
//        get() = webAppContainersModel.appServicePlanResourceGroupName
//        set(value) {
//            webAppContainersModel.appServicePlanResourceGroupName = value
//        }
//    var isCreatingNewWebAppOnLinux: Boolean
//        get() = webAppContainersModel.isCreatingNewWebAppOnLinux
//        set(value) {
//            webAppContainersModel.isCreatingNewWebAppOnLinux = value
//        }
//    var finalRepositoryName: String?
//        get() = webAppContainersModel.finalRepositoryName
//        set(value) {
//            webAppContainersModel.finalRepositoryName = value
//        }
//    var finalTagName: String?
//        get() = webAppContainersModel.finalTagName
//        set(value) {
//            webAppContainersModel.finalTagName = value
//        }
//    var port: Int
//        get() = webAppContainersModel.port
//        set(value) {
//            webAppContainersModel.port = value
//        }

    override fun getState() =
        options as? WebAppContainersConfigurationOptions

    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
        WebAppContainersRunState(project, this)

    override fun getConfigurationEditor() = WebAppContainersSettingEditor(project)

    override fun checkConfiguration() {
        val options = getState() ?: return
        with(options) {

        }
//        checkAzurePreconditions()
//        validateDockerImageConfiguration()
//        with(webAppContainersModel) {
//            if (isCreatingNewWebAppOnLinux) {
//                if (webAppName.isNullOrEmpty()) throw RuntimeConfigurationError("Please specify a Web App for Containers")
//                if (!isValidResourceName(webAppName)) throw RuntimeConfigurationError("Web App names only allow alphanumeric characters and hyphens, cannot start or end in a hyphen, and must be less than 60 chars")
//                if (subscriptionId.isNullOrEmpty()) throw RuntimeConfigurationError("Please specify a Subscription")
//                if (resourceGroupName.isNullOrEmpty()) throw RuntimeConfigurationError("Please specify a Resource Group")
//                if (!isValidResourceName(resourceGroupName)) throw RuntimeConfigurationError("Resource group names only allow alphanumeric characters and hyphens, cannot start or end in a hyphen, and must be less than 60 chars")
//                if (appServicePlanName.isNullOrEmpty()) throw RuntimeConfigurationError("Please specify an App Service plan")
//                if (appServicePlanName.isNullOrEmpty()) throw RuntimeConfigurationError("App Service plan name is not provided")
//            } else {
//                if (webAppId.isNullOrEmpty()) throw RuntimeConfigurationError("Please specify Web App for Containers")
//            }
//        }
    }

//    private fun validateDockerImageConfiguration() {
//        val repositoryName = finalRepositoryName
//        val tagName = finalTagName
//        if (repositoryName.isNullOrEmpty()) throw RuntimeConfigurationError("Please specify a container registry and an image.")
//        if (tagName.isNullOrEmpty()) throw RuntimeConfigurationError("Please specify an image tag.")
//        if (repositoryName.length > 255) throw RuntimeConfigurationError("The length of repository name must be less than 256 characters.")
//        val repositoryParts = repositoryName.split('/')
//        if (repositoryParts.last().isEmpty()) throw RuntimeConfigurationError("Please specify an image name.")
//        if (repositoryName.endsWith('/')) throw RuntimeConfigurationError("The repository name should not end with '/'.")
//        repositoryParts.forEach {
//            if (!repoComponentRegex.matches(it)) throw RuntimeConfigurationError("Invalid repository component: $it, should follow: $REPO_COMPONENT_REGEX_PATTERN")
//        }
//        if (tagName.length > 127) throw RuntimeConfigurationError("The length of tag name must be less than 128 characters")
//        if (!tagRegex.matches(tagName)) throw RuntimeConfigurationError("Invalid tag: $tagName, should follow: $TAG_REGEX_PATTERN")
//    }
}