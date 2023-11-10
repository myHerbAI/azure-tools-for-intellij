/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.settings

import com.intellij.openapi.components.BaseState

class AzureFunctionSettingState : BaseState() {
    var functionV2Path by string(null)
    var functionV3Path by string(null)
    var functionV4Path by string(null)
    var functionDownloadPath by string(null)
}