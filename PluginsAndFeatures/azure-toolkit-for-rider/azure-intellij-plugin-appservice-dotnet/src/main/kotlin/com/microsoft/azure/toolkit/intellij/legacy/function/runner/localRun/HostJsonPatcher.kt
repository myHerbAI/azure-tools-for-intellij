/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.google.gson.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import java.io.File

@Service
class HostJsonPatcher {
    companion object {
        fun getInstance(): HostJsonPatcher = service()

        private const val FUNCTION_PROPERTY_NAME = "functions"

        private fun determineHostJsonFile(workingDirectory: String) = listOf(
            File(workingDirectory, "host.json"),
            File(workingDirectory, "../host.json"),
            File(workingDirectory, "../../host.json")
        ).firstOrNull { it.exists() }?.canonicalFile

        private val LOG = logger<HostJsonPatcher>()
    }

    fun tryPatchHostJsonFile(workingDirectory: String, functionNames: String) = tryPatchHostJsonFile(
        determineHostJsonFile(workingDirectory),
        functionNames.split(',', ';', '|', ' ')
    )

    private fun tryPatchHostJsonFile(hostJsonFile: File?, functionNames: List<String>) {
        if (hostJsonFile == null || !hostJsonFile.exists()) {
            LOG.warn("Could not find host.json file to patch.")
            return
        }

        val functions = functionNames
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (functions.isEmpty()) {
            LOG.info("Skip patching " + hostJsonFile.absolutePath + " - no function names were specified.")
            return
        }

        LOG.info("Patching " + hostJsonFile.absolutePath + " with function names: ${functions.joinToString(", ")}")
        try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val hostJson = gson.fromJson(hostJsonFile.readText(), JsonElement::class.java).asJsonObject

            val functionArray = JsonArray()
            functions.forEach { functionArray.add(JsonPrimitive(it)) }
            hostJson.add(FUNCTION_PROPERTY_NAME, functionArray)

            hostJsonFile.writeText(gson.toJson(hostJson))
        } catch (e: JsonParseException) {
            LOG.error(e)
        }
    }
}