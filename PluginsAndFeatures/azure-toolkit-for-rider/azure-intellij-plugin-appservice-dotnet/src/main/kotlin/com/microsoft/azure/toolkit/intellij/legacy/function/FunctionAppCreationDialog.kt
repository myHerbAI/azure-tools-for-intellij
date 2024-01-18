/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceInfoAdvancedPanel
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceInfoBasicPanel
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.auth.IAccountActions
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import javax.swing.JPanel

class FunctionAppCreationDialog(project: Project) : ConfigDialog<FunctionAppConfig>(project), Disposable {
    private val basicPanel: AppServiceInfoBasicPanel<FunctionAppConfig>
    private val advancedPanel: AppServiceInfoAdvancedPanel<FunctionAppConfig>
    private val panel: JPanel

    init {
        val selectedSubscriptions = Azure.az(AzureAccount::class.java).account().selectedSubscriptions
        if (selectedSubscriptions.isEmpty()) {
            this.close()
            throw AzureToolkitRuntimeException(
                "There are no subscriptions selected in your account.",
                IAccountActions.SELECT_SUBS
            )
        }

        basicPanel = AppServiceInfoBasicPanel(project, selectedSubscriptions[0]) {
            FunctionAppConfig.getFunctionAppDefaultConfig(project.name)
        }
        Disposer.register(this, basicPanel)

        advancedPanel = AppServiceInfoAdvancedPanel(project) {
            FunctionAppConfig.builder().build()
        }
        Disposer.register(this, advancedPanel)

        panel = panel {
            row { cell(basicPanel) }
            row { cell(advancedPanel) }
        }

        advancedPanel.setValidPricingTier(PricingTier.FUNCTION_PRICING.toList(), PricingTier.CONSUMPTION)
        advancedPanel.setDeploymentVisible(false)

        this.init()

        setFrontPanel(basicFormPanel)
    }

    override fun createCenterPanel() = panel

    override fun getDialogTitle() = "Create Function"

    override fun getBasicFormPanel() = basicPanel

    override fun getAdvancedFormPanel() = advancedPanel

    override fun dispose() {
        super.dispose()
    }
}