/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package model.daemon

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.string
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.model.nova.ide.SolutionModel

@Suppress("unused")
object FunctionAppDaemonModel : Ext(SolutionModel.Solution) {
    private val FunctionAppRequest = structdef {
        field("methodName", string.nullable)
        field("functionName", string.nullable)
        field("projectFilePath", string)
    }

    private val AzureFunctionsVersionRequest = structdef {
        field("projectFilePath", string)
    }

    init {
        setting(Kotlin11Generator.Namespace, "com.jetbrains.rider.azure.model")
        setting(CSharp50Generator.Namespace, "JetBrains.Rider.Azure.Model")

        sink("runFunctionApp", FunctionAppRequest)
                .doc("Signal from backend to run a Function App locally.")

        sink("debugFunctionApp", FunctionAppRequest)
                .doc("Signal from backend to debug a Function App locally.")

        sink("triggerFunctionApp", FunctionAppRequest)
                .doc("Signal from backend to trigger a Function App.")

        call("getAzureFunctionsVersion", AzureFunctionsVersionRequest, string.nullable)
                .doc("Request from frontend to read the AzureFunctionsVersion MSBuild property.")
    }
}
