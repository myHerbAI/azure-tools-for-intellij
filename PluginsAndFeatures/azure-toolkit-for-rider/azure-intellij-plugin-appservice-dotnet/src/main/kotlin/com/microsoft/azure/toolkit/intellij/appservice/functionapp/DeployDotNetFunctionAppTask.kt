/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.functionapp

import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.IOException
import java.nio.file.Files

class DeployDotNetFunctionAppTask(
    private val target: FunctionAppBase<*, *, *>,
    private val stagingFolder: File
) : AzureTask<FunctionAppBase<*, *, *>>() {
    companion object {
        private const val LOCAL_SETTINGS_FILE = "local.settings.json"
        private const val RUNNING = "Running"
    }

    private val messager = AzureMessager.getMessager()

    override fun getDescription() = AzureString.format("Deploy artifact to Function App %s", target.name)

    override fun doExecute(): FunctionAppBase<*, *, *> {
        messager.info("Starting deployment...")

        val file = packageStagingDirectory()
        messager.info(AzureString.format("Deploying temporary file: %s", file))
        target.deploy(file, FunctionDeployType.ZIP)

        if (!target.status.equals(RUNNING, true)) {
            messager.info("Starting Function App after deploying artifacts...")
            target.start()
            messager.info("Successfully started Function App.")
        }

        messager.info("Deployment succeed")

        return target
    }

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
}
