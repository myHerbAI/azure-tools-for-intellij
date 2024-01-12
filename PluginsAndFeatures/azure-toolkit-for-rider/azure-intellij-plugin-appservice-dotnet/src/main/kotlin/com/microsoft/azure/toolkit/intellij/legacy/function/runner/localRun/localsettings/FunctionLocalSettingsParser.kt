/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.localsettings

import com.intellij.json.JsonUtil
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.application
import com.microsoft.azure.toolkit.intellij.legacy.function.utils.JsonParser.findBooleanProperty
import com.microsoft.azure.toolkit.intellij.legacy.function.utils.JsonParser.findNumberProperty
import com.microsoft.azure.toolkit.intellij.legacy.function.utils.JsonParser.findStringProperty
import com.microsoft.azure.toolkit.intellij.legacy.function.utils.JsonParser.jsonFileFromVirtualFile

@Service
class FunctionLocalSettingsParser {
    companion object {
        fun getInstance(): FunctionLocalSettingsParser = service()

        const val PROPERTY_IS_ENCRYPTED = "IsEncrypted"

        const val OBJECT_VALUES = "Values"
        const val PROPERTY_VALUES_RUNTIME = "FUNCTIONS_WORKER_RUNTIME"
        const val PROPERTY_VALUES_WEB_JOBS_STORAGE = "AzureWebJobsStorage"
        const val PROPERTY_VALUES_WEB_JOBS_DASHBOARD = "AzureWebJobsDashboard"
        const val PROPERTY_VALUES_BINDING_CONNECTION = "MyBindingConnection"
        const val PROPERTY_VALUES_WEB_JOBS_HTTP_EXAMPLE_DISABLED = "AzureWebJobs.HttpExample.Disabled"

        const val OBJECT_HOST = "Host"
        const val PROPERTY_HOST_LOCAL_HTTP_PORT = "LocalHttpPort"
        const val PROPERTY_HOST_CORS = "CORS"
        const val PROPERTY_HOST_CORS_CREDENTIALS = "CORSCredentials"

        const val OBJECT_CONNECTION_STRINGS = "ConnectionStrings"
    }

    fun readLocalSettingsJsonFrom(virtualFile: VirtualFile, project: Project): FunctionLocalSettings? {
        val jsonFile = application.runReadAction(Computable {
            jsonFileFromVirtualFile(virtualFile, project)
        }) ?: return null

        return application.runReadAction(Computable {
            readFunctionLocalSettingsFrom(jsonFile)
        })
    }

    private fun readFunctionLocalSettingsFrom(jsonFile: JsonFile): FunctionLocalSettings? {
        val topLevelObject = JsonUtil.getTopLevelObject(jsonFile) ?: return null

        val isEncrypted = findBooleanProperty(topLevelObject, PROPERTY_IS_ENCRYPTED)

        val valueObject = topLevelObject.findProperty(OBJECT_VALUES)?.value as? JsonObject
        val runtime = findStringProperty(valueObject, PROPERTY_VALUES_RUNTIME)?.let { runtime ->
            FunctionWorkerRuntime.entries.firstOrNull { it.value.equals(runtime, ignoreCase = true) }
        }
        val webJobsStorage = findStringProperty(valueObject, PROPERTY_VALUES_WEB_JOBS_STORAGE)
        val webJobsDashboard = findStringProperty(valueObject, PROPERTY_VALUES_WEB_JOBS_DASHBOARD)
        val bindingConnection = findStringProperty(valueObject, PROPERTY_VALUES_BINDING_CONNECTION)
        val webJobsHttpExampleDisabled =
            findBooleanProperty(valueObject, PROPERTY_VALUES_WEB_JOBS_HTTP_EXAMPLE_DISABLED)

        val hostObject = topLevelObject.findProperty(OBJECT_HOST)?.value as? JsonObject
        val localHttpPort = findNumberProperty(hostObject, PROPERTY_HOST_LOCAL_HTTP_PORT)?.toInt()
        val cors = findStringProperty(hostObject, PROPERTY_HOST_CORS)
        val corsCredentials = findBooleanProperty(hostObject, PROPERTY_HOST_CORS_CREDENTIALS)

        val connectionStringsObject = topLevelObject.findProperty(OBJECT_CONNECTION_STRINGS)?.value as? JsonObject
        val connectionStrings: Map<String, String>? =
            connectionStringsObject?.propertyList?.mapNotNull { entry ->
                val value = entry?.value as? JsonStringLiteral ?: return@mapNotNull null
                entry.name to value.value
            }?.toMap()

        return FunctionLocalSettings(
            isEncrypted = isEncrypted,
            values = FunctionValuesModel(
                runtime,
                webJobsStorage,
                webJobsDashboard,
                webJobsHttpExampleDisabled,
                bindingConnection
            ),
            host = FunctionHostModel(localHttpPort, cors, corsCredentials),
            connectionStrings = connectionStrings
        )
    }
}