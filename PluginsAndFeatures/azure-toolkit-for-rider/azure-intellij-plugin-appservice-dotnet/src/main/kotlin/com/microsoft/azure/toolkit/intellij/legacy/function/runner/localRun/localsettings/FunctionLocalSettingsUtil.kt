/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.localsettings

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.RunnableProject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

object FunctionLocalSettingsUtil {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        decodeEnumsCaseInsensitive = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    fun readFunctionLocalSettings(publishableProject: PublishableProjectModel): FunctionLocalSettings? =
        readFunctionLocalSettings(Path(publishableProject.projectFilePath).parent.absolutePathString())

    fun readFunctionLocalSettings(runnableProject: RunnableProject): FunctionLocalSettings? =
        readFunctionLocalSettings(Path(runnableProject.projectFilePath).parent.absolutePathString())

    fun readFunctionLocalSettings(basePath: String): FunctionLocalSettings? {
        val localSettingsFile = getLocalSettingsVirtualFile(basePath) ?: return null
        val content = localSettingsFile.readText()
        return json.decodeFromString<FunctionLocalSettings>(content)
    }

    private fun getLocalSettingsVirtualFile(basePath: String): VirtualFile? {
        val localSettingsFile = getLocalSettingFile(basePath)
        if (!localSettingsFile.exists()) return null

        return VfsUtil.findFile(localSettingsFile, true)
    }

    private fun getLocalSettingFile(basePath: String): Path = Path(basePath).resolve("local.settings.json")
}