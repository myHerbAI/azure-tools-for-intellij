/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappcontainers

import com.microsoft.azure.toolkit.intellij.common.DockerImageModel

data class DockerPushConfiguration(
        val registryAddress: String?,
        val repositoryName: String?,
        val tagName: String?,
        val dockerImage: DockerImageModel?,
        val dockerServer: String?
)