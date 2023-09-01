package com.microsoft.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import com.microsoft.intellij.ui.ServerExplorerToolWindowFactory

class AzureExplorerOpenAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(event.dataContext) ?: return
        ToolWindowManager
            .getInstance(project)
            .getToolWindow(ServerExplorerToolWindowFactory.EXPLORER_WINDOW)
            ?.activate(null)
    }
}