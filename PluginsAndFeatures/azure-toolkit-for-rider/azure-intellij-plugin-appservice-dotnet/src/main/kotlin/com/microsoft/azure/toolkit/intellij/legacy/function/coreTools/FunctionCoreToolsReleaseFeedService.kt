/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*

@Service
class FunctionCoreToolsReleaseFeedService : Disposable {
    companion object {
        fun getInstance(): FunctionCoreToolsReleaseFeedService = service()
    }

    private val client: HttpClient = HttpClient(CIO)

    suspend fun getReleaseFeed(feedUrl: String) = client.get(feedUrl).body<ReleaseFeed>()

    override fun dispose() = client.close()
}

data class ReleaseFeed(
    val tags: Map<String, Tag>,
    val releases: Map<String, Release>
)

data class Tag(
    val release: String?,
    val releaseQuality: String?,
    val hidden: Boolean
)

data class Release(
    val templates: String?,
    val coreTools: List<ReleaseCoreTool>
)

data class ReleaseCoreTool(
    val os: String?,
    val architecture: String?,
    val downloadLink: String?,
    val sha2: String?,
    val size: String?,
    val default: Boolean
)