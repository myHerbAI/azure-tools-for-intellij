/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.action.Action
import java.util.stream.Collectors

open class WebAppComboBox(project: Project) : AppServiceComboBox<AppServiceConfig>(project) {
    override fun refreshItems() {
        Azure.az(AzureWebApp::class.java).refresh()
        super.refreshItems()
    }

    override fun loadAppServiceModels(): MutableList<AppServiceConfig> {
        val account = Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) {
            return mutableListOf()
        }

        return Azure.az(AzureWebApp::class.java).webApps().parallelStream()
            .map { webApp -> convertAppServiceToConfig({ AppServiceConfig() }, webApp) }
            .filter { a -> a.subscriptionId != null }
            .sorted { a, b -> a.appName.compareTo(b.appName, true) }
            .collect(Collectors.toList())
    }

    override fun createResource() {
        val dialog = WebAppCreationDialog(project)
        Disposer.register(this, dialog)
        val actionId: Action.Id<AppServiceConfig> = Action.Id.of("user/webapp.create_app.app")
        dialog.setOkAction(
            Action(actionId)
                .withLabel("Create")
                .withIdParam(AppServiceConfig::appName)
                .withSource { it }
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