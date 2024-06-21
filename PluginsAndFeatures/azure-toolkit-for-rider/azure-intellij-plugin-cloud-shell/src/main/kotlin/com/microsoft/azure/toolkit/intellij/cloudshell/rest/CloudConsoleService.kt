/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.rest

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import io.ktor.client.*
import io.ktor.client.engine.cio.*

@Service(Service.Level.APP)
class CloudConsoleService : Disposable {
    companion object {
        fun getInstance() = service<CloudConsoleService>()
    }

    private val client = HttpClient(CIO)

    suspend fun getUserSettings() {

    }

    override fun dispose() = client.close()
}