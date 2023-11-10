/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.jetbrains.rider.projectView.actions.projectTemplating.backend.ReSharperProjectTemplateCustomizer
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import javax.swing.Icon

class FunctionTemplateCustomizer : ReSharperProjectTemplateCustomizer {
    override val categoryName: String
        get() = "Azure Functions"

    override val newIcon: Icon
        get() = IntelliJAzureIcons.getIcon("/icons/FunctionApp/TemplateAzureFunc.svg")

    override val newName: String
        get() = "Azure Functions"
}