/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.azurite

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.azurite.services.AzuriteService
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import javax.swing.Icon

class AzuriteBeforeRunTask : BeforeRunTask<AzuriteBeforeRunTask>(AzuriteBeforeRunTaskProvider.ID)

class AzuriteBeforeRunTaskProvider : BeforeRunTaskProvider<AzuriteBeforeRunTask>() {
    companion object {
        val ID = Key.create<AzuriteBeforeRunTask>("RunAzuriteTask")
    }

    override fun getId() = ID

    override fun getName() = "Start Azurite"

    override fun getIcon(): Icon = IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.AZURITE)

    override fun createTask(runConfiguration: RunConfiguration): AzuriteBeforeRunTask {
        val task = AzuriteBeforeRunTask()
        task.isEnabled = true
        return task
    }

    override fun executeTask(context: DataContext, configuration: RunConfiguration, environment: ExecutionEnvironment, task: AzuriteBeforeRunTask): Boolean {
        val project = configuration.project
        val service = AzuriteService.getInstance()
        if (!service.isRunning) {
            service.start(project)
        }
        
        return true
    }
}