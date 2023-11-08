/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.WebAppComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.common.action.Action
import java.util.stream.Collectors

class WebAppContainersComboBox(project: Project) : WebAppComboBox(project) {
    init {
        setRenderer(AppComboBoxRender(true))
    }

    override fun loadAppServiceModels(): MutableList<WebAppConfig> {
        val webApps = Azure.az(AzureWebApp::class.java).webApps()
        return webApps.stream().parallel()
                .filter { a -> a.runtime != null && a.runtime?.isWindows == false }
                .sorted { a, b -> a.name.compareTo(b.name, true) }
                .map { webApp -> convertAppServiceToConfig({ WebAppConfig() }, webApp) }
                .collect(Collectors.toList())
    }

    override fun createResource() {
        val dialog = WebAppContainersCreationDialog(project)
        dialog.setDeploymentVisible(false)
        val actionId: Action.Id<WebAppConfig> = Action.Id.of("user/webapp.create_app.app")
        dialog.setOkAction(Action(actionId)
                .withLabel("Create")
                .withIdParam(WebAppConfig::getName)
                .withSource { it }
                .withAuthRequired(false)
                .withHandler(this::setValue)
        )
        dialog.show()
    }
}

fun Row.dockerWebAppComboBox(project: Project): Cell<WebAppContainersComboBox> {
    val component = WebAppContainersComboBox(project)
    return cell(component)
}