/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.common

import com.intellij.openapi.options.SettingsEditor

abstract class RiderAzureSettingsEditor<T> : SettingsEditor<T>() where T : RiderAzureRunConfigurationBase<*> {
    protected abstract fun getPanel(): RiderAzureSettingPanel<T>

    override fun applyEditorTo(configuration: T) {
        getPanel().apply(configuration)
        configuration.checkConfiguration()
    }

    override fun resetEditorFrom(configuration: T) {
        getPanel().reset(configuration)
    }

    override fun createEditor() = getPanel().getMainPanel()

    override fun disposeEditor() {
        getPanel().disposeEditor()
        super.disposeEditor()
    }
}