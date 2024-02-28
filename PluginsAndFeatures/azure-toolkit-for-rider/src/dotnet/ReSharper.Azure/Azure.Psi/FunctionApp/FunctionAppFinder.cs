// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Linq;
using JetBrains.Metadata.Reader.API;
using JetBrains.Metadata.Reader.Impl;
using JetBrains.Rd.Base;
using JetBrains.ReSharper.Psi;
using JetBrains.Util;
using JetBrains.Util.Logging;

namespace JetBrains.ReSharper.Azure.Psi.FunctionApp;

public static class FunctionAppFinder
{
    private static readonly ILogger OurLogger = Logger.GetLogger(typeof(FunctionAppFinder));

    private static class DefaultWorker
    {
        public static readonly IClrTypeName FunctionNameAttributeTypeName =
            new ClrTypeName("Microsoft.Azure.WebJobs.FunctionNameAttribute");

        public static readonly IClrTypeName TimerTriggerAttributeTypeName =
            new ClrTypeName("Microsoft.Azure.WebJobs.TimerTriggerAttribute");
    }

    private static class IsolatedWorker
    {
        public static readonly IClrTypeName FunctionAttributeTypeName =
            new ClrTypeName("Microsoft.Azure.Functions.Worker.FunctionAttribute");

        public static readonly IClrTypeName TimerTriggerAttributeTypeName =
            new ClrTypeName("Microsoft.Azure.Functions.Worker.TimerTriggerAttribute");
    }

    /// <summary>
    /// Get Function Name from Attribute for a provided method or null if attribute is missing
    /// </summary>
    /// <param name="method">A Method instance to check.</param>
    /// <returns>Function App name string value.</returns>
    public static string? GetFunctionNameFromMethod(IMethod? method)
    {
        if (method == null) return null;

        var functionAttribute = GetFunctionNameAttribute(method);
        if (functionAttribute == null) return null;

        var functionParameters = functionAttribute.PositionParameters().ToArray();
        if (functionParameters.Length < 1)
        {
            OurLogger.Warn($"Insufficient number of parameters in '{functionAttribute.GetAttributeShortName()}' attribute to get a Function name.");
            return null;
        }

        var functionNameParameter = functionParameters.First();

        if (functionNameParameter == null || !functionNameParameter.ConstantValue.IsString())
        {
            OurLogger.Error(
                $"Unable to get a Function name from '{method.ShortName}' method attribute parameter: '{functionNameParameter.PrintToString()}'.");
            return null;
        }

        return functionNameParameter.ConstantValue.StringValue;
    }

    /// <summary>
    /// Check whether a method define a Function App that can be run.
    /// Reference links:
    /// - https://docs.microsoft.com/en-us/azure/azure-functions/functions-dotnet-class-library#methods-recognized-as-functions
    /// - https://docs.microsoft.com/en-us/azure/azure-functions/functions-dotnet-dependency-injection
    /// </summary>
    /// <param name="method">A Method instance to check.</param>
    /// <returns>Flag whether provided method can be considered as a method to start a Function App of any type.</returns>
    public static bool IsSuitableFunctionAppMethod(IMethod? method)
    {
        return method != null &&
               method.GetAccessRights() == AccessRights.PUBLIC &&
               GetFunctionNameAttribute(method) != null;
    }

    /// <summary>
    /// Check whether declared type is a Function App Timer Trigger type.
    /// </summary>
    /// <param name="typeElement">A type element to check.</param>
    /// <returns>Flag whether type element match Function App Timer Trigger attribute type.</returns>
    public static bool IsTimerTriggerAttribute(ITypeElement typeElement)
    {
        return typeElement.GetClrName().Equals(DefaultWorker.TimerTriggerAttributeTypeName) ||
               typeElement.GetClrName().Equals(IsolatedWorker.TimerTriggerAttributeTypeName);
    }

    private static IAttributeInstance? GetFunctionNameAttribute(IMethod method)
    {
        var functionAttributes = method.GetAttributeInstances(DefaultWorker.FunctionNameAttributeTypeName, false)
            .Union(method.GetAttributeInstances(IsolatedWorker.FunctionAttributeTypeName, false))
            .ToList();

        if (functionAttributes.IsEmpty())
        {
            if (OurLogger.IsTraceEnabled())
            {
                OurLogger.Trace(
                    $"Unable to get a proper function name from a method '{method.ShortName}' that has more then one [Function] attribute.");
            }

            return null;
        }

        if (functionAttributes.Count > 1)
        {
            OurLogger.Info(
                $"Found more then one FunctionName attribute from a method '{method.ShortName}'. Return the first match.");
        }

        return functionAttributes.First();
    }
}