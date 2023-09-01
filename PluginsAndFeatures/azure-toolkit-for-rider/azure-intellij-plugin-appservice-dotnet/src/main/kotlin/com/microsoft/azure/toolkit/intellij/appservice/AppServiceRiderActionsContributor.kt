package com.microsoft.azure.toolkit.intellij.appservice

import com.intellij.openapi.actionSystem.AnActionEvent
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor
import com.microsoft.azure.toolkit.ide.appservice.file.AppServiceFileActionsContributor
import com.microsoft.azure.toolkit.ide.common.IActionsContributor
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor
import com.microsoft.azure.toolkit.ide.containerregistry.ContainerRegistryActionsContributor
import com.microsoft.azure.toolkit.intellij.appservice.actions.AppServiceFileAction
import com.microsoft.azure.toolkit.intellij.legacy.webapp.action.CreateWebAppAction
import com.microsoft.azure.toolkit.intellij.legacy.webapp.action.DeployWebAppAction
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.common.action.Action
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import kotlin.math.max

class AppServiceRiderActionsContributor : IActionsContributor {

    private val initializeOrder = max(AppServiceActionsContributor.INITIALIZE_ORDER, ContainerRegistryActionsContributor.INITIALIZE_ORDER) + 1
    override fun getOrder() = initializeOrder

    override fun registerActions(am: AzureActionManager) {
        val tm = AzureTaskManager.getInstance()

        Action(AppServiceFileActionsContributor.APP_SERVICE_FILE_VIEW)
                .withLabel("Open File")
                .withIdParam { file: AppServiceFile -> file.name }
                .visibleWhen { file: Any? -> file is AppServiceFile }
                .withSource { file: AppServiceFile -> file.app }
                .withHandler { file: AppServiceFile, e: Any ->
                    tm.runLater {
                        AppServiceFileAction().openAppServiceFile(file, (e as AnActionEvent).project)
                    }
                }
                .withShortcut(am.ideDefaultShortcuts.edit())
                .register(am)

        Action(AppServiceFileActionsContributor.APP_SERVICE_FILE_DOWNLOAD)
                .withLabel("Download")
                .withIdParam { file: AppServiceFile -> file.name }
                .visibleWhen { file: Any? -> file is AppServiceFile }
                .withSource { file: AppServiceFile -> file.app }
                .withHandler { file: AppServiceFile, e: Any ->
                    tm.runLater {
                        AppServiceFileAction().saveAppServiceFile(file, (e as AnActionEvent).project, null)
                    }
                }
                .register(am)
    }

    override fun registerHandlers(am: AzureActionManager) {
        am.registerHandler(ResourceCommonActionsContributor.CREATE, { r, _ -> r is AzureWebApp }, { _, e: AnActionEvent -> e.project?.let { CreateWebAppAction.openDialog(it, null) } })
        am.registerHandler(ResourceCommonActionsContributor.DEPLOY, { r, _ -> r is WebApp }, { c, e: AnActionEvent -> DeployWebAppAction.deploy(c as? WebApp, e.project) })
    }
}