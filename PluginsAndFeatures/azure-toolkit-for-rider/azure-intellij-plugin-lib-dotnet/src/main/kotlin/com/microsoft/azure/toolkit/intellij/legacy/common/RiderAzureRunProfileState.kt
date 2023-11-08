/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.common

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandlerMessenger
import com.microsoft.azure.toolkit.intellij.common.runconfig.RunConfigurationUtils
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

abstract class RiderAzureRunProfileState<T>(protected val project: Project) : RunProfileState {
    protected var processHandlerMessenger: RunProcessHandlerMessenger? = null

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        val processHandler = RunProcessHandler()
        processHandler.addDefaultListener()
        processHandlerMessenger = RunProcessHandlerMessenger(processHandler)
        val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        processHandler.startNotify()
        consoleView.attachToProcess(processHandler)

        val subscribe = Mono.fromCallable { executeSteps(processHandler) }
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        { res: T ->
                            processHandler.putUserData(RunConfigurationUtils.AZURE_RUN_STATE_RESULT, true)
                            this.onSuccess(res, processHandler)
                        },
                        { err: Throwable? ->
                            err?.let { processHandlerMessenger?.error(it)}
                            onFail(err, processHandler)
                        }
                )

        processHandler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                subscribe.dispose()
            }
        })

        return DefaultExecutionResult(consoleView, processHandler)
    }

    protected fun setText(runProcessHandler: RunProcessHandler, text: String?) {
        if (runProcessHandler.isProcessRunning) {
            runProcessHandler.setText(text)
        }
    }

    protected abstract fun executeSteps(processHandler: RunProcessHandler): T

    protected abstract fun onSuccess(result: T, processHandler: RunProcessHandler)

    protected fun onFail(error: Throwable?, processHandler: RunProcessHandler) {
        processHandler.putUserData(RunConfigurationUtils.AZURE_RUN_STATE_RESULT, false)
        processHandler.putUserData(RunConfigurationUtils.AZURE_RUN_STATE_EXCEPTION, error)
        processHandler.notifyProcessTerminated(-1)
    }
}