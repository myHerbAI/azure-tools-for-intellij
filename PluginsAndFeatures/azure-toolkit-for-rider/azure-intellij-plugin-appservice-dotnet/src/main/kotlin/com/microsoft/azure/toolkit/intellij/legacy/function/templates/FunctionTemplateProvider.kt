/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.jetbrains.rd.framework.util.launch
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IOptProperty
import com.jetbrains.rd.util.reactive.OptProperty
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.threading.SpinWait
import com.jetbrains.rider.projectView.actions.projectTemplating.RiderProjectTemplateProvider
import com.jetbrains.rider.projectView.actions.projectTemplating.RiderProjectTemplateState
import com.jetbrains.rider.projectView.actions.projectTemplating.impl.ProjectTemplateDialogContext
import kotlinx.coroutines.Dispatchers
import java.time.Duration

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
            var isReloaded = false
            lifetime.launch(Dispatchers.Default) {
                templateManager.tryReload()
                isReloaded = true
            }
            SpinWait.spinUntil(lifetime, Duration.ofSeconds(5)) { isReloaded }
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