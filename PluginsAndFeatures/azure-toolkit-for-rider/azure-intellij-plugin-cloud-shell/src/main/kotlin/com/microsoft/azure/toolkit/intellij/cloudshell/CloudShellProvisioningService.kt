/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell

import com.azure.core.credential.TokenRequestContext
import com.azure.identity.implementation.util.ScopeUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleProvisionParameterProperties
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleProvisionParameters
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleProvisionTerminalParameters
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleService
import com.microsoft.azure.toolkit.intellij.cloudshell.terminal.AzureCloudTerminalFactory
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

@Service(Service.Level.PROJECT)
class CloudShellProvisioningService(private val project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<CloudShellProvisioningService>()
        private val LOG = logger<CloudShellProvisioningService>()
    }

    private val defaultTerminalColumns = 100
    private val defaultTerminalRows = 30

    suspend fun provision(subscription: Subscription) = withBackgroundProgress(project, "Provisioning cloud shell...") {
        val account = Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) {
            LOG.warn("Account is not logged in")
            return@withBackgroundProgress
        }

        val managementEndpoint = account.environment.managementEndpoint
        val scopes = ScopeUtil.resourceToScopes(managementEndpoint)
        val request = TokenRequestContext().apply { addScopes(*scopes) }
        val accessToken = account
            .getTenantTokenCredential(subscription.tenantId)
            .getToken(request)
            .awaitSingleOrNull()
            ?.token

        if (accessToken == null) {
            LOG.warn("Unable to get access token")
            return@withBackgroundProgress
        }

        val cloudConsoleService = CloudConsoleService.getInstance(project)
        cloudConsoleService.setAccessToken(accessToken)

        val resourceManagerEndpoint = account.environment.resourceManagerEndpoint

        val settings = cloudConsoleService.getUserSettings(resourceManagerEndpoint)
        if (settings == null) {
            LOG.warn("Azure Cloud Shell is not configured in any Azure subscription")
            return@withBackgroundProgress
        }

        val preferredOsType = settings.properties?.preferredOsType ?: "Linux"
        val provisionParameters = CloudConsoleProvisionParameters(
            CloudConsoleProvisionParameterProperties(preferredOsType)
        )
        val provisionResult = cloudConsoleService.provision(
            resourceManagerEndpoint,
            settings.properties?.preferredLocation,
            provisionParameters
        )
        if (provisionResult == null || provisionResult.properties.provisioningState != "Succeeded") {
            LOG.warn("Unable to provision cloud shell")
            return@withBackgroundProgress
        }

        val provisionUrl = provisionResult.properties.uri
        if (provisionUrl.isNullOrEmpty()) {
            LOG.error("Cloud shell URL was empty")
            return@withBackgroundProgress
        }

        val shellUrl = "$provisionUrl/terminals"

        val keyVaultDnsSuffix = "https://" + account.environment.keyVaultDnsSuffix.trimStart('.') + "/"
        val vaultScopes = ScopeUtil.resourceToScopes(keyVaultDnsSuffix)
        val vaultToken = account
            .getTenantTokenCredential(subscription.tenantId)
            .getToken(TokenRequestContext().apply { addScopes(*vaultScopes) })
            .awaitSingleOrNull()
            ?.token

        if (vaultToken == null) {
            LOG.error("Unable to obtain graph or vault token")
            return@withBackgroundProgress
        }

        val provisionTerminalParameters = CloudConsoleProvisionTerminalParameters(
            listOf(vaultToken)
        )
        val provisionTerminalResult = cloudConsoleService.provisionTerminal(
            shellUrl,
            defaultTerminalColumns,
            defaultTerminalRows,
            provisionTerminalParameters,
        )
        if (provisionTerminalResult == null) {
            LOG.warn("Unable to provision cloud shell")
            return@withBackgroundProgress
        }

        val socketUri = provisionTerminalResult.socketUri
        if (socketUri.isNullOrEmpty()) {
            LOG.error("Socket URI was empty")
            return@withBackgroundProgress
        }

        val runner = AzureCloudTerminalFactory
            .getInstance(project)
            .createTerminalRunner(provisionUrl, URI(socketUri))

        val terminalWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
            ?: return@withBackgroundProgress

        withContext(Dispatchers.EDT) {
            terminalWindow.show()

            delay(500.milliseconds)

            TerminalToolWindowManager
                .getInstance(project).createNewSession(runner)
        }
    }
}