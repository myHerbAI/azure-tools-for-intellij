/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.jetbrains.rd.util.reactive.fire
import com.jetbrains.rider.projectView.actions.projectTemplating.RiderProjectTemplate
import com.jetbrains.rider.projectView.actions.projectTemplating.RiderProjectTemplateGenerator
import com.jetbrains.rider.projectView.actions.projectTemplating.common.InfoProjectTemplateGeneratorBase
import com.jetbrains.rider.projectView.actions.projectTemplating.impl.ProjectTemplateDialog
import com.jetbrains.rider.projectView.actions.projectTemplating.impl.ProjectTemplateDialogContext
import com.jetbrains.rider.projectView.actions.projectTemplating.impl.ProjectTemplateTransferableModel
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import javax.swing.Icon

class InstallFunctionProjectTemplate : RiderProjectTemplate {
    override val group: String
        get() = ".NET / .NET Core"
    override val localizedGroup: String
        get() = ".NET / .NET Core"
    override val name: String
        get() = "Azure Functions"
    override val localizedName: String
        get() = "Azure Functions"
    override val icon: Icon
        get() = IntelliJAzureIcons.getIcon("/icons/FunctionApp/TemplateAzureFunc.svg")

    override fun getKeywords() = arrayOf(name)

    override fun createGenerator(
        context: ProjectTemplateDialogContext,
        transferableModel: ProjectTemplateTransferableModel
    ): RiderProjectTemplateGenerator = object : InfoProjectTemplateGeneratorBase() {
        override val expandActionName: String
            get() = "Reload"

        override fun expand(): Runnable {
            // close dialog and show again to refresh templates
            return Runnable { ProjectTemplateDialog.show(context.project, context.entity) }
        }

        override fun getComponent() = InstallFunctionToolComponent(validationError) {

            context.restart.fire()
        }.getView()
    }
}