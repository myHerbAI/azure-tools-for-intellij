/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.microsoft.azure.toolkit.intellij.common.RiderRunProcessHandlerMessager
import com.microsoft.azure.toolkit.lib.appservice.model.DeployOptions
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.operation.Operation
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import java.util.concurrent.Callable

class DeployDotNetWebAppTask(
    private val target: WebAppBase<*, *, *>,
    private val artifact: WebAppArtifact,
    private val processHandlerMessager: RiderRunProcessHandlerMessager?
) : AzureTask<WebAppBase<*, *, *>>() {

    private val subTasks: MutableList<AzureTask<*>> = mutableListOf()

    private var webApp: WebAppBase<*, *, *>? = null

    init {
        registerSubTask(getDeploymentTask()) { webApp = it }
    }

    override fun getDescription() = AzureString.format("Deploy artifact to Web App %s", target.name)

    private fun getDeploymentTask() =
        AzureTask("Deploying Web App",
            Callable {
                AzureMessager.getMessager().info("Starting deployment...")

                AzureMessager.getMessager().info("Trying to deploy artifact to ${target.name}...")

                val deployOptions = DeployOptions
                    .builder()
                    .cleanDeployment(false)
                    .restartSite(true)
                    .build()
                target.deploy(artifact.deployType, artifact.file, deployOptions)

                if (!target.getFormalStatus().isRunning) {
                    AzureMessager.getMessager().info("Starting Web App after deploying artifacts...")
                    target.start()
                    AzureMessager.getMessager().info("Successfully started Web App.")
                }

                AzureMessager.getMessager().info("Deployment succeed.")

                return@Callable target
            }
        )

    private fun <T> registerSubTask(task: AzureTask<T>?, consumer: (result: T) -> Unit) {
        if (task != null) {
            subTasks.add(AzureTask<T>(Callable {
                val result = task.body.call()
                consumer(result)
                return@Callable result
            }))
        }
    }

    override fun doExecute(): WebAppBase<*, *, *> {
        Operation.execute(AzureString.fromString("Deploying Web App"), {
            processHandlerMessager?.let { OperationContext.current().messager = it }

            for (task in subTasks) {
                task.body.call()
            }
        }, null)

        return requireNotNull(webApp)
    }
}