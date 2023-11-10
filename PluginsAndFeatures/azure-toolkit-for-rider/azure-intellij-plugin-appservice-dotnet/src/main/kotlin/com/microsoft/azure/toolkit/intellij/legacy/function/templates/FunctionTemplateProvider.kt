/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IOptProperty
import com.jetbrains.rd.util.reactive.OptProperty
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rider.projectView.actions.projectTemplating.RiderProjectTemplateProvider
import com.jetbrains.rider.projectView.actions.projectTemplating.RiderProjectTemplateState
import com.jetbrains.rider.projectView.actions.projectTemplating.impl.ProjectTemplateDialogContext

class FunctionTemplateProvider : RiderProjectTemplateProvider {

    override val isReady = Property(true)

    override fun load(
        lifetime: Lifetime,
        context: ProjectTemplateDialogContext
    ): IOptProperty<RiderProjectTemplateState> {
        val state = RiderProjectTemplateState(arrayListOf(), arrayListOf())

        state.new.add(FunctionProjectTemplate())

        return OptProperty(state)
    }
}