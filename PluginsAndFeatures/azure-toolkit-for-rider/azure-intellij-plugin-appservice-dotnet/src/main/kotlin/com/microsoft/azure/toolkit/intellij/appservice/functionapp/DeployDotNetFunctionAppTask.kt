/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.functionapp

import com.microsoft.azure.toolkit.intellij.common.RiderRunProcessHandlerMessager
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.operation.Operation
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.Callable

class DeployDotNetFunctionAppTask(
    private val target: FunctionAppBase<*, *, *>,
    private val stagingFolder: File,
    private val processHandlerMessager: RiderRunProcessHandlerMessager?
) : AzureTask<FunctionAppBase<*, *, *>>() {
    companion object {
        private const val LOCAL_SETTINGS_FILE = "local.settings.json"
        private const val RUNNING = "Running"
    }

    private val subTasks: MutableList<AzureTask<*>> = mutableListOf()

    private var functionApp: FunctionAppBase<*, *, *>? = null

    init {
        registerSubTask(getDeploymentTask()) { functionApp = it }
    }

    override fun getDescription() = AzureString.format("Deploy artifact to Function App %s", target.name)

    private fun getDeploymentTask() =
        AzureTask("Deploying Function App",
            Callable {
                AzureMessager.getMessager().info("Starting deployment...")

                val file = packageStagingDirectory()
                AzureMessager.getMessager().info("Deploying temporary file: ${file.absolutePath}")
                target.deploy(file, FunctionDeployType.ZIP)

                if (!target.status.equals(RUNNING, true)) {
                    AzureMessager.getMessager().info("Starting Function App after deploying artifacts...")
                    target.start()
                    AzureMessager.getMessager().info("Successfully started Function App.")
                }

                AzureMessager.getMessager().info("Deployment succeed.")

                return@Callable target
            })

    private fun packageStagingDirectory(): File {
        try {
            val zipFile = Files.createTempFile("azure-functions", ".zip").toFile()
            ZipUtil.pack(stagingFolder, zipFile)
            ZipUtil.removeEntry(zipFile, LOCAL_SETTINGS_FILE)
            return zipFile
        } catch (e: IOException) {
            throw AzureToolkitRuntimeException("Failed to package function to deploy", e)
        }
    }

    private fun <T> registerSubTask(task: AzureTask<T>?, consumer: (result: T) -> Unit) {
        if (task != null) {
            subTasks.add(AzureTask<T>(Callable {
                val result = task.body.call()
                consumer(result)
                return@Callable result
            }))
        }
    }

    override fun doExecute(): FunctionAppBase<*, *, *> {
        Operation.execute(AzureString.fromString("Deploying Function App"), {
            processHandlerMessager?.let { OperationContext.current().messager = it }

            for (task in subTasks) {
                task.body.call()
            }
        }, null)

        return requireNotNull(functionApp)
    }
}
