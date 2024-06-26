/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.terminal

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.cloudshell.rest.CloudConsoleService
import java.net.URI

@Service(Service.Level.PROJECT)
class AzureCloudTerminalFactory(private val project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<AzureCloudTerminalFactory>()
    }

//    fun createTerminalRunner(
//        cloudConsoleService: CloudConsoleService,
//        cloudConsoleBaseUrl: String,
//        socketUri: URI
//    ): AzureCloudTerminalRunner {
//
//    }
}