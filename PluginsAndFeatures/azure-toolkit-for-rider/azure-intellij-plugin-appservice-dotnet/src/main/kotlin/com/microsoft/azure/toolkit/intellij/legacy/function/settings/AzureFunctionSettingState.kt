/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.settings

import com.intellij.openapi.components.BaseState
import com.intellij.util.PathUtil
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class AzureFunctionSettingState(azureToolsHomeFolder: Path) : BaseState() {
    var functionV2Path by string(null)
    var functionV3Path by string(null)
    var functionV4Path by string(null)
    var functionDownloadPath by string(PathUtil.toSystemIndependentName(azureToolsHomeFolder.absolutePathString()))
    var checkForFunctionMissingPackages by property(true)
}