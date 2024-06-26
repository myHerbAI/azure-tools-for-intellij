/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.cloudshell.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.microsoft.azure.toolkit.intellij.cloudshell.CloudShellProvisioningService
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import kotlinx.coroutines.launch

class StartAzureCloudShellAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val selectedSubscriptions = Azure.az(AzureAccount::class.java).account().selectedSubscriptions
        if (selectedSubscriptions.size == 0) return

        val subscription = if (selectedSubscriptions.size == 1) {
            selectedSubscriptions.first()
        } else {
            selectedSubscriptions.first()
        }

        currentThreadCoroutineScope().launch {
            val provisioningService = CloudShellProvisioningService.getInstance(project)
            provisioningService.provision(subscription)
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val account = Azure.az(AzureAccount::class.java).account()
        e.presentation.isEnabled = account.isLoggedIn
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}