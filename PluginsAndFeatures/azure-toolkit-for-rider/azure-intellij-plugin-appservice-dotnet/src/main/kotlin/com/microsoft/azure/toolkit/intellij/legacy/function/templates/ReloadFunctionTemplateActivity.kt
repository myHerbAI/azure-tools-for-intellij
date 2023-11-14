/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.rd.framework.util.launch
import com.jetbrains.rd.platform.util.lifetime
import kotlinx.coroutines.Dispatchers

class ReloadFunctionTemplateActivity: ProjectActivity {
    override suspend fun execute(project: Project) {
        project.lifetime.launch(Dispatchers.Default) {
            FunctionTemplateManager.getInstance().tryReload()
        }
    }
}