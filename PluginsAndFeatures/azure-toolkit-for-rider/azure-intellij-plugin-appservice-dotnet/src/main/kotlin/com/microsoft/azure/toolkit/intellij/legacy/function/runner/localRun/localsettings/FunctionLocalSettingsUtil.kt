/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.localsettings

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.RunnableProject
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

object FunctionLocalSettingsUtil {
    fun readFunctionLocalSettings(project: Project, publishableProject: PublishableProjectModel): FunctionLocalSettings? =
        readFunctionLocalSettings(project, Path(publishableProject.projectFilePath).parent.absolutePathString())

    fun readFunctionLocalSettings(project: Project, runnableProject: RunnableProject): FunctionLocalSettings? =
        readFunctionLocalSettings(project, Path(runnableProject.projectFilePath).parent.absolutePathString())

    fun readFunctionLocalSettings(project: Project, basePath: String): FunctionLocalSettings? {
        val localSettingsFile = getLocalSettingsVirtualFile(basePath) ?: return null
        return FunctionLocalSettingsParser.getInstance().readLocalSettingsJsonFrom(localSettingsFile, project)
    }

    private fun getLocalSettingsVirtualFile(basePath: String): VirtualFile? {
        val localSettingsFile = getLocalSettingFile(basePath)
        if (!localSettingsFile.exists()) return null

        return VfsUtil.findFile(localSettingsFile, true)
    }

    private fun getLocalSettingFile(basePath: String): Path = Path(basePath).resolve("local.settings.json")
}