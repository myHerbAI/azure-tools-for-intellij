/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.common

import javax.swing.JPanel

abstract class RiderAzureSettingPanel<T> where T : RiderAzureRunConfigurationBase<*> {
    abstract fun apply(configuration: T)
    abstract fun reset(configuration: T)
    abstract fun getMainPanel(): JPanel
    abstract fun disposeEditor()
}