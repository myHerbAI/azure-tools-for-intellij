/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.intellij.openapi.command.impl.DummyProject
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IOptProperty
import com.jetbrains.rd.util.reactive.OptProperty
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rider.projectView.actions.projectTemplating.RiderProjectTemplateProvider
import com.jetbrains.rider.projectView.actions.projectTemplating.RiderProjectTemplateState
import com.jetbrains.rider.projectView.actions.projectTemplating.impl.ProjectTemplateDialogContext

class FunctionTemplateProvider : RiderProjectTemplateProvider {

    override val isReady = Property(false)

    override fun load(
        lifetime: Lifetime,
        context: ProjectTemplateDialogContext
    ): IOptProperty<RiderProjectTemplateState> {
        val result = OptProperty<RiderProjectTemplateState>()

        val state = RiderProjectTemplateState(arrayListOf(), arrayListOf())
        result.set(state)

        val templateManager = FunctionTemplateManager.getInstance()

        if (!templateManager.areRegistered()) {
            runWithModalProgressBlocking(DummyProject.getInstance(), "Reloading Azure templates...") {
                templateManager.tryReload()
            }
            isReady.set(true)
        } else {
            isReady.set(true)
        }

        if (!templateManager.areRegistered()) {
            state.new.add(InstallFunctionProjectTemplate())
        }

        return result
    }
}