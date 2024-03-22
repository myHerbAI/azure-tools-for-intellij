/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.microsoft.azure.toolkit.lib.appservice.model.DeployOptions
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.task.AzureTask

class DeployDotNetWebAppTask(
    private val target: WebAppBase<*, *, *>,
    private val artifact: WebAppArtifact
): AzureTask<WebAppBase<*, *, *>>() {

    private val messager = AzureMessager.getMessager()

    override fun getDescription() = AzureString.format("Deploy artifact to Web App %s", target.name)

    override fun doExecute(): WebAppBase<*, *, *>? {
        messager.info("Trying to deploy artifact to ${target.name}...")

        val deployOptions = DeployOptions.builder().restartSite(true).build()
        target.deploy(artifact.deployType, artifact.file, deployOptions)

        if (!target.getFormalStatus().isRunning()) {
            messager.info("Starting Web App after deploying artifacts...")
            target.start()
            messager.info("Successfully started Web App.")
        }

        messager.info("Deployment succeed")

        return target
    }
}