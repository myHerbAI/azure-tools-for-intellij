/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDraft
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppModule

fun WebAppDraft.toDotNetWebAppDraft(): DotNetWebAppDraft {
    val draftOrigin = origin
    val draft =
        if (draftOrigin != null) DotNetWebAppDraft(draftOrigin)
        else DotNetWebAppDraft(name, resourceGroupName, module as WebAppModule)


    return draft
}