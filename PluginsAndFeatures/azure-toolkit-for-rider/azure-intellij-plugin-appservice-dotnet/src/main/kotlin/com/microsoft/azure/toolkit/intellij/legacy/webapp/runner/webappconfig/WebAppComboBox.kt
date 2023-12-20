/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.common.action.Action
import java.util.stream.Collectors

open class WebAppComboBox(project: Project) : AppServiceComboBox<WebAppConfig>(project) {
    override fun refreshItems() {
        Azure.az(AzureWebApp::class.java).refresh()
        super.refreshItems()
    }

    override fun loadAppServiceModels(): MutableList<WebAppConfig> =
        Azure.az(AzureWebApp::class.java).webApps().parallelStream()
            .map { webApp -> convertAppServiceToConfig({ WebAppConfig() }, webApp) }
            .filter { a -> a.subscription != null }
            .sorted { a, b -> a.name.compareTo(b.name, true) }
            .collect(Collectors.toList())

    override fun createResource() {
        val dialog = WebAppCreationDialog(project)
        dialog.setDeploymentVisible(false)
        val actionId: Action.Id<WebAppConfig> = Action.Id.of("user/webapp.create_app.app")
        dialog.setOkAction(
            Action(actionId)
                .withLabel("Create")
                .withIdParam(WebAppConfig::getName)
                .withAuthRequired(false)
                .withHandler(this::setValue)
        )
        dialog.show()
    }
}

fun Row.webAppComboBox(project: Project): Cell<WebAppComboBox> {
    val component = WebAppComboBox(project)
    return cell(component)
}