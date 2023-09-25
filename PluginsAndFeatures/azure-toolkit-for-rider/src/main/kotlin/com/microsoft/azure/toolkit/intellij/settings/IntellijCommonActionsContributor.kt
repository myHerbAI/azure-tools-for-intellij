package com.microsoft.azure.toolkit.intellij.settings

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.microsoft.azure.toolkit.ide.common.IActionsContributor
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager

class IntellijCommonActionsContributor : IActionsContributor {
    override fun registerHandlers(am: AzureActionManager) {
        am.registerHandler(
                ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS,
                { _, _ -> true }
        ) { _, e: AnActionEvent ->
            val project = if (e.project != null) e.project else {
                val openProjects = ProjectManagerEx.getInstanceEx().openProjects
                if (openProjects.isEmpty()) null else openProjects[0]
            }
            val title = OperationBundle.description("user/common.open_azure_settings")
            AzureTaskManager.getInstance().runLater(AzureTask<Any>(title) { openSettingsDialog(project) })
        }
    }

    private fun openSettingsDialog(project: Project?) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, AzureSettingsConfigurable::class.java)
    }
}