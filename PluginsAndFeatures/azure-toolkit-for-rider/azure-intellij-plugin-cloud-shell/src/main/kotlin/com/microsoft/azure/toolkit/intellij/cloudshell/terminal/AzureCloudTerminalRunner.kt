/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.terminal

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleService
import java.net.URI

class AzureCloudTerminalRunner(
    project: Project,
    private val cloudConsoleService: CloudConsoleService,
    private val cloudConsoleBaseUrl: String,
    socketUri: URI,
    process: AzureCloudTerminalProcess
) {
}