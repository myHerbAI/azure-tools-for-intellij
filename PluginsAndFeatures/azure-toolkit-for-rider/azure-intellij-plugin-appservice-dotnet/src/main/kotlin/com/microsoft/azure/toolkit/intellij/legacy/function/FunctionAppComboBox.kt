/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.common.action.Action
import java.util.stream.Collectors

class FunctionAppComboBox(project: Project) : AppServiceComboBox<FunctionAppConfig>(project) {
    override fun refreshItems() {
        Azure.az(AzureFunctions::class.java).refresh()
        super.refreshItems()
    }

    override fun loadAppServiceModels(): MutableList<FunctionAppConfig> =
        Azure.az(AzureFunctions::class.java).functionApps().parallelStream()
            .map { functionApp -> convertAppServiceToConfig({ FunctionAppConfig() }, functionApp) }
            .filter { a -> a.subscription != null }
            .sorted { a, b -> a.name.compareTo(b.name, true) }
            .collect(Collectors.toList())

    override fun createResource() {
        val dialog = FunctionAppCreationDialog(project)
        val actionId: Action.Id<FunctionAppConfig> = Action.Id.of("user/function.create_app.app")
        dialog.setOkAction(
            Action(actionId)
                .withLabel("Create")
                .withIdParam(FunctionAppConfig::getName)
                .withSource { it }
                .withAuthRequired(false)
                .withHandler(this::setValue)
        )
        dialog.show()
    }
}

fun Row.functionAppComboBox(project: Project): Cell<FunctionAppComboBox> {
    val component = FunctionAppComboBox(project)
    return cell(component)
}