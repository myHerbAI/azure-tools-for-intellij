/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.rest

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@Service(Service.Level.PROJECT)
class CloudConsoleService : Disposable {
    companion object {
        fun getInstance(project: Project) = project.service<CloudConsoleService>()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                explicitNulls = false
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getUserSettings(
        resourceManagerEndpoint: String,
        accessToken: String
    ): CloudConsoleUserSettings? {
        val response =
            client.get("${resourceManagerEndpoint}providers/Microsoft.Portal/userSettings/cloudconsole?api-version=2023-02-01-preview") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }
        if (!response.status.isSuccess()) return null

        return response.body<CloudConsoleUserSettings>()
    }

    suspend fun provision(
        resourceManagerEndpoint: String,
        accessToken: String,
        parameters: CloudConsoleProvisionParameters
    ): CloudConsoleProvisionResult? {
        val response =
            client.put("${resourceManagerEndpoint}providers/Microsoft.Portal/consoles/default?api-version=2023-02-01-preview") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                contentType(ContentType.Application.Json)
                setBody(parameters)
            }
        if (!response.status.isSuccess()) return null

        return response.body<CloudConsoleProvisionResult>()
    }

    suspend fun provisionTerminal(
        url: String,
        columns: Int,
        rows: Int,
        provisionParameters: CloudConsoleProvisionTerminalParameters,
        accessToken: String
    ): CloudConsoleProvisionTerminalResult? {
        val response = client.post(url) {
            url {
                parameters.append("cols", columns.toString())
                parameters.append("rows", rows.toString())
            }
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            contentType(ContentType.Application.Json)
            setBody(provisionParameters)
        }
        if (!response.status.isSuccess()) return null

        return response.body<CloudConsoleProvisionTerminalResult>()
    }

    override fun dispose() = client.close()
}