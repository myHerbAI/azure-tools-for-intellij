// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.MSBuild;
using JetBrains.ProjectModel.Properties;
using JetBrains.Rd.Tasks;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Rider.Azure.Model;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Daemon.FunctionApp;

[SolutionComponent]
public class FunctionAppDaemonHost
{
    private readonly ISolution _solution;
    private readonly FunctionAppDaemonModel _model;

    public FunctionAppDaemonHost(ISolution solution)
    {
        _solution = solution;

        _model = solution.GetProtocolSolution().GetFunctionAppDaemonModel();
        _model.GetAzureFunctionsVersion.SetSync(GetAzureFunctionsVersionHandler);
    }

    public void RunFunctionApp(string projectFilePath) => _model.RunFunctionApp(
        new FunctionAppRequest(
            projectFilePath: projectFilePath,
            methodName: null,
            functionName: null));

    public void DebugFunctionApp(string projectFilePath) => _model.DebugFunctionApp(
        new FunctionAppRequest(
            projectFilePath: projectFilePath,
            methodName: null,
            functionName: null));

    public void RunFunctionApp(string projectFilePath, string methodName, string functionName) => _model.RunFunctionApp(
        new FunctionAppRequest(
            projectFilePath: projectFilePath,
            methodName: methodName,
            functionName: functionName));

    public void DebugFunctionApp(string projectFilePath, string methodName, string functionName) =>
        _model.DebugFunctionApp(
            new FunctionAppRequest(
                projectFilePath: projectFilePath,
                methodName: methodName,
                functionName: functionName));

    public void TriggerFunctionApp(string projectFilePath, string methodName, string functionName) =>
        _model.TriggerFunctionApp(
            new FunctionAppRequest(
                projectFilePath: projectFilePath,
                methodName: methodName,
                functionName: functionName));


    private string? GetAzureFunctionsVersionHandler(Lifetime lifetime, AzureFunctionsVersionRequest request)
    {
        var projectFilePath = VirtualFileSystemPath.Parse(request.ProjectFilePath, InteractionContext.SolutionContext);
        IProject? project;
        using (ReadLockCookie.Create())
        {
            project = _solution.FindProjectByProjectFilePath(projectFilePath);
        }

        if (project is null) return null;

        var version = project.GetUniqueRequestedProjectProperty(MSBuildProjectUtil.AzureFunctionsVersionProperty);

        return version;
    }
}