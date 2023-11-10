/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.util.reactive.IProperty
import com.jetbrains.rider.ui.components.base.Viewable
import javax.swing.JComponent
import javax.swing.JPanel

class InstallFunctionToolComponent(private val validationError: IProperty<String?>, reloadTemplates: Runnable) :
    Viewable<JComponent> {

    private val panel: JPanel

    init {
        panel = panel {

        }
    }

    override fun getView() = panel
}