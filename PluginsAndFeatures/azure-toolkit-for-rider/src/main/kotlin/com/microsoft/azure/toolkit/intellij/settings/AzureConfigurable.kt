/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.settings

import com.intellij.openapi.options.Configurable

class AzureConfigurable : Configurable {
    override fun createComponent() = null

    override fun isModified() = false

    override fun apply() {}

    override fun getDisplayName() = "Azure"
}