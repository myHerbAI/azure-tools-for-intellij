/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell

import com.azure.core.credential.TokenRequestContext
import com.azure.identity.implementation.util.ScopeUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleProvisionParameterProperties
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleProvisionParameters
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleService
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import kotlinx.coroutines.reactor.awaitSingleOrNull

@Service(Service.Level.PROJECT)
class CloudShellProvisioningService {
    companion object {
        fun getInstance(project: Project) = project.service<CloudShellProvisioningService>()
        private val LOG = logger<CloudShellProvisioningService>()
    }

    suspend fun provision(subscription: Subscription) {
        val account = Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) {
            LOG.warn("Account is not logged in")
            return
        }

        val resourceManagerEndpoint = account.environment.resourceManagerEndpoint
        val scopes = ScopeUtil.resourceToScopes(resourceManagerEndpoint)
        val request = TokenRequestContext().apply { addScopes(*scopes) }
        val accessToken = account
            .getTenantTokenCredential(subscription.tenantId)
            .getToken(request)
            .awaitSingleOrNull()
            ?.token

        if (accessToken == null) {
            LOG.warn("Unable to get access token")
            return
        }

        val cloudConsoleService = CloudConsoleService.getInstance()
        val settings = cloudConsoleService.getUserSettings(resourceManagerEndpoint, accessToken)
        if (settings == null) {
            LOG.warn("Azure Cloud Shell is not configured in any Azure subscription")
            return
        }

        val preferredOsType = settings.properties?.preferredOsType ?: "Linux"
        val provisionParameters = CloudConsoleProvisionParameters(
            CloudConsoleProvisionParameterProperties(preferredOsType)
        )
        val provisionResult = cloudConsoleService.provision(resourceManagerEndpoint, accessToken, provisionParameters)
        if (provisionResult == null) {
            LOG.warn("Could not provision cloud shell")
            return
        }

        val provisionUrl = provisionResult.properties.uri
        if (provisionUrl.isNullOrEmpty())
        {
            LOG.error("Cloud shell URL was empty")
            return
        }

        val shellUrl = "$provisionUrl/terminals"

    }
}