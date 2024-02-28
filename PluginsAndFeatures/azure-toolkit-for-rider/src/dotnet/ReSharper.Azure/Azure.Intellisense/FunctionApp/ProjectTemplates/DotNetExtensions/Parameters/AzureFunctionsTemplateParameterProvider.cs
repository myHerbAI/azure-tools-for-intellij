// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.Application;
using JetBrains.Rider.Backend.Features.ProjectModel.ProjectTemplates.DotNetExtensions;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.ProjectTemplates.DotNetExtensions.Parameters;

[ShellComponent]
public class AzureFunctionsTemplateParameterProvider : IDotNetTemplateParameterProvider
{
    public int Priority => 20;

    public IReadOnlyCollection<DotNetTemplateParameter> Get()
    {
        return new DotNetTemplateParameter[]
        {
            new AzureFunctionsVersionParameter(),
            // TODO: When RdTextParameterStyle supports numeric/text only input, these can be enabled
            //new AzureFunctionsHttpPortParameter(),
            //new AzureFunctionsStorageConnectionStringParameter()
        };
    }
}