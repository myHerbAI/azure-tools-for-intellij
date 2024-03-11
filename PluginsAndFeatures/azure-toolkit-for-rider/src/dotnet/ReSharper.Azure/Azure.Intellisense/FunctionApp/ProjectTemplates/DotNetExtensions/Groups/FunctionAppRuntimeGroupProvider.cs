// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.Application;
using JetBrains.Rider.Backend.Features.ProjectModel.ProjectTemplates.DotNetExtensions;
using Microsoft.TemplateEngine.Abstractions;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.ProjectTemplates.DotNetExtensions.Groups;

[ShellComponent]
public class FunctionAppRuntimeGroupProvider : IDotNetTemplateGroupProvider
{
    public int Priority => 40;
        
    public IReadOnlyCollection<DotNetTemplateGroup> Get()
    {
        return new[] { new FunctionAppRuntimeGroup() };
    }

    private class FunctionAppRuntimeGroup() : DotNetTemplateGroup("Functions runtime", null)
    {
        protected override bool ShowIfSingleTemplate => false;

        protected override string? GetOption(ITemplateInfo info)
        {
            if (info.GroupIdentity is "Microsoft.AzureFunctions.ProjectTemplates")
            {
                // Isolated worker known template identities:
                if (info.Identity == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.Isolated.3.x" ||
                    info.Identity == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.Isolated.3.x" ||
                    info.Identity == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.Isolated.4.x" ||
                    info.Identity == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.Isolated.4.x")
                {
                    return "Isolated worker";
                }
                    
                // Default worker known template identities:
                if (info.Identity == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.3.x" ||
                    info.Identity == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.3.x" ||
                    info.Identity == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.4.x" ||
                    info.Identity == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.4.x")
                {
                    return "Default worker";
                }
            }
                
            return null;
        }
    }
}