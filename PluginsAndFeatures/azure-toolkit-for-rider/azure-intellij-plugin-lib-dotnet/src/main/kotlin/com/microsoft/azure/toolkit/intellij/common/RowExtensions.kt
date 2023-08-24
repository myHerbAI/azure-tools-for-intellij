package com.microsoft.azure.toolkit.intellij.common

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.jetbrains.rider.model.PublishableProjectModel

fun Row.dotnetProjectComboBox(project: Project, canBePublishedAction: (PublishableProjectModel) -> Boolean): Cell<AzureDotnetProjectComboBox> {
    val component = AzureDotnetProjectComboBox(project, canBePublishedAction)
    return cell(component)
}
