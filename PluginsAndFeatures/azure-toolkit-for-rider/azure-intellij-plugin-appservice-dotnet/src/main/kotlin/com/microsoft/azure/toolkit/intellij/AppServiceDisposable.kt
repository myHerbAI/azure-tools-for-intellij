/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
class AppServiceDisposable : Disposable {
    companion object {
        fun getInstance() = service<AppServiceDisposable>()
    }

    override fun dispose() {
    }
}