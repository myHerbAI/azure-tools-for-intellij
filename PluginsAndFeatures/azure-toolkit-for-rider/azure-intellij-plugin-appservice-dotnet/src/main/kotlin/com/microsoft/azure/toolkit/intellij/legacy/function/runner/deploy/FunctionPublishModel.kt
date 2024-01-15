/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy


class FunctionPublishModel : FunctionDeployModel() {
    var storageAccountName: String? = null
    var storageAccountResourceGroup: String? = null
    var publishableProjectPath: String? = null
    var projectConfiguration: String = ""
    var projectPlatform: String = ""
}