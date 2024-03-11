/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.actions

import com.intellij.httpClient.RestClientIcons
import com.intellij.httpClient.http.request.HttpRequestLanguage
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.io.IOException

class TriggerAzureFunctionAction(private val functionName: String) : AnAction(
    "Trigger '$functionName'",
    "Create trigger function HTTP request...",
    RestClientIcons.Http_requests_filetype
) {
    companion object {
        private val LOG = logger<TriggerAzureFunctionAction>()

        private const val TEMPLATE_NAME = "Trigger Azure Function"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val scratchFileName = "Trigger_$functionName.http"
        val scratchFileService = ScratchFileService.getInstance()
        val scratchRootType = ScratchRootType.getInstance()

        val scratchFile =
            scratchFileService.findFile(scratchRootType, scratchFileName, ScratchFileService.Option.existing_only)
                ?: scratchRootType.createScratchFile(
                    project,
                    scratchFileName,
                    HttpRequestLanguage.INSTANCE,
                    createContentFromTemplate(project, functionName) ?: "",
                    ScratchFileService.Option.create_if_missing
                )

        if (scratchFile != null) {
            FileEditorManager.getInstance(project).openFile(scratchFile, true)
        }
    }

    private fun createContentFromTemplate(project: Project, functionName: String): String? {
        val template = FileTemplateManager.getInstance(project).findInternalTemplate(TEMPLATE_NAME)
        if (template != null) {
            try {
                val properties = FileTemplateManager.getInstance(project).defaultProperties
                properties.setProperty("functionName", functionName)
                return template.getText(properties)
            } catch (e: IOException) {
                LOG.warn(e)
            }
        }
        return null
    }
}