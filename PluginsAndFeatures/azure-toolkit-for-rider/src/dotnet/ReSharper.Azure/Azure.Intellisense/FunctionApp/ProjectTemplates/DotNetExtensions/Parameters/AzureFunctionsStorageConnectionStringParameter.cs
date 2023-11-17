// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.Rider.Backend.Features.ProjectModel.ProjectTemplates.DotNetExtensions;
using JetBrains.Rider.Backend.Features.ProjectModel.ProjectTemplates.DotNetTemplates;
using JetBrains.Rider.Model;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.ProjectTemplates.DotNetExtensions.Parameters;

internal class AzureFunctionsStorageConnectionStringParameter() : DotNetTemplateParameter(
    name: "StorageConnectionStringValue",
    presentableName: "Connection string",
    tooltip: "The connection string for your Azure WebJobs Storage.")
{
    public override RdProjectTemplateContent CreateContent(
        DotNetProjectTemplateExpander expander,
        IDotNetTemplateContentFactory factory,
        int index,
        IDictionary<string, string> context)
    {
        var parameter = expander.TemplateInfo.GetParameter(Name);
        if (parameter == null)
        {
            return factory.CreateNextParameters(new[] { expander }, index + 1, context);
        }

        var content = factory.CreateNextParameters(new[] { expander }, index + 1, context);
        // TODO: RdTextParameterStyle.FileChooser does not support numeric/text only input

        return new RdProjectTemplateTextParameter(
            Name,
            PresentableName,
            parameter.DefaultValue,
            Tooltip,
            RdTextParameterStyle.FileChooser,
            content
        );
    }
}