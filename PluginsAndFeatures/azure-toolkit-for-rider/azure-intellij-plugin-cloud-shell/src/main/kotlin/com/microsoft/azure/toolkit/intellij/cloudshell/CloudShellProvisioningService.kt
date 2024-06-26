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
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleService
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.model.Subscription

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
            .block()
            ?.token

        if (accessToken == null) {
            LOG.warn("Unable to get access token")
            return
        }


        val cloudConsoleService = CloudConsoleService.getInstance()
        val settings = cloudConsoleService.getUserSettings(resourceManagerEndpoint, accessToken)
        if (settings != null) {

        }

    }
}