/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.rd.util.withBackgroundContext
import com.intellij.openapi.util.registry.Registry
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.util.reactive.IProperty
import com.jetbrains.rider.ui.components.base.Viewable
import com.microsoft.azure.toolkit.intellij.legacy.function.FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsInfoProvider
import javax.swing.JComponent
import javax.swing.JPanel

class InstallFunctionToolComponent(private val validationError: IProperty<String?>, reloadTemplates: Runnable) :
    Viewable<JComponent> {

    private val panel: JPanel
    private val isCoreToolsFeedEnabled = Registry.`is`("azure.function_app.core_tools.feed.enabled")

    init {
        panel = panel {
            row {
                text("Azure Functions Project Templates")
            }
            row {
                text("Rider requires the Azure Functions Core Tools to be installed and configured to create new Azure Functions projects.")
            }
            row {
                link("Download Azure Functions Core Tools... (recommended)") {
                    val project = ProjectManager.getInstance().defaultProject
                    runWithModalProgressBlocking(project, "Downloading Azure Functions Core Tools...") {
                        withBackgroundContext {
                            FunctionCoreToolsInfoProvider.getInstance()
                                .retrieveForVersion(
                                    FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION,
                                    true
                                )
                        }
                    }
                    validationError.set(null)
                    reloadTemplates.run()
                }
            }.visible(isCoreToolsFeedEnabled)
            row {
                link("Configure Azure Functions Core Tools...") {
                    val project = ProjectManager.getInstance().defaultProject
                    ShowSettingsUtilImpl.showSettingsDialog(
                        project,
                        "com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionConfigurable",
                        ""
                    )
                    validationError.set(null)
                    reloadTemplates.run()
                }
            }
        }
    }

    override fun getView() = panel
}