/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.localsettings

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rider.model.RunnableProject
import java.io.File

object FunctionLocalSettingsUtil {
    fun readFunctionLocalSettings(project: Project, runnableProject: RunnableProject): FunctionLocalSettings? =
        readFunctionLocalSettings(project, File(runnableProject.projectFilePath).parent)

    fun readFunctionLocalSettings(project: Project, basePath: String): FunctionLocalSettings? {
        val localSettingsFile = getLocalSettingsVirtualFile(basePath) ?: return null
        return FunctionLocalSettingsParser.getInstance().readLocalSettingsJsonFrom(localSettingsFile, project)
    }

    fun getLocalSettingsVirtualFile(basePath: String): VirtualFile? {
        val localSettingsFile = getLocalSettingFile(basePath)
        if (!localSettingsFile.exists()) return null

        return VfsUtil.findFileByIoFile(localSettingsFile, true)
    }

    fun getLocalSettingFile(basePath: String): File = File(basePath).resolve("local.settings.json")
}