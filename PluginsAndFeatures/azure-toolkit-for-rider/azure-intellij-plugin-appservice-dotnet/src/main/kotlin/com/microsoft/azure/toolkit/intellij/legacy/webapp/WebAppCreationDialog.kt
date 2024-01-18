/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceInfoAdvancedPanel
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceInfoBasicPanel
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.auth.IAccountActions
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import javax.swing.JPanel

open class WebAppCreationDialog(project: Project) : ConfigDialog<WebAppConfig>(project), Disposable {
    private val basicPanel: AppServiceInfoBasicPanel<WebAppConfig>
    private val advancedPanel: AppServiceInfoAdvancedPanel<WebAppConfig>
    private val panel: JPanel

    companion object {
        const val RIDER_PROJECT_CONFIGURATION = "RIDER_PROJECT_CONFIGURATION"
        const val RIDER_PROJECT_PLATFORM = "RIDER_PROJECT_PLATFORM"
    }

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
            WebAppConfig.getWebAppDefaultConfig(project.name)
        }
        Disposer.register(this, basicPanel)

        advancedPanel = AppServiceInfoAdvancedPanel(project) {
            WebAppConfig.getWebAppDefaultConfig(project.name)
        }
        Disposer.register(this, advancedPanel)

        panel = panel {
            row { cell(basicPanel) }
            row { cell(advancedPanel) }
        }

        advancedPanel.setValidPricingTier(PricingTier.WEB_APP_PRICING.toList(), WebAppConfig.DEFAULT_PRICING_TIER)

        this.init()

        setFrontPanel(basicFormPanel)
    }

    override fun createCenterPanel() = panel

    override fun getDialogTitle() = "Create Web App"

    override fun getBasicFormPanel() = basicPanel

    override fun getAdvancedFormPanel() = advancedPanel

    fun setDeploymentVisible(visible: Boolean) {
        basicPanel.setDeploymentVisible(visible)
        advancedPanel.setDeploymentVisible(visible)
        pack()
    }

    override fun dispose() {
        super.dispose()
    }
}