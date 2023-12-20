// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.Rider.Backend.Features.ProjectModel.ProjectTemplates.DotNetExtensions;
using JetBrains.Rider.Backend.Features.ProjectModel.ProjectTemplates.DotNetTemplates;
using JetBrains.Rider.Model;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.ProjectTemplates.DotNetExtensions.Parameters;

internal class AzureFunctionsVersionParameter() : DotNetTemplateParameter(
    name: "AzureFunctionsVersion",
    presentableName: "Functions host",
    tooltip: "The setting that determines the functions host target release")
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

        var options = new List<RdProjectTemplateGroupOption>();
        if (parameter.Choices != null && !parameter.Choices.IsEmpty())
        {
            foreach (var parameterOptionFromTemplate in parameter.Choices)
            {
                var presentation = !parameterOptionFromTemplate.Value.DisplayName.IsNullOrWhitespace()
                    ? parameterOptionFromTemplate.Value.DisplayName
                    : parameterOptionFromTemplate.Value.Description;

                var content = factory.CreateNextParameters(new[] { expander }, index + 1, context);
                options.Add(new RdProjectTemplateGroupOption(parameterOptionFromTemplate.Key,
                    presentation ?? parameterOptionFromTemplate.Key, null, content));
            }
        }
        else
        {
            var isNet6OrHigher = expander.Template.Sdk.Major is >= 6 or 0;
            var supportedAzureFunctionsVersions = isNet6OrHigher
                ? new[] { "V4" }
                : new[] { "V3", "V2" };

            foreach (var version in supportedAzureFunctionsVersions)
            {
                var content = factory.CreateNextParameters(new[] { expander }, index + 1, context);
                options.Add(new RdProjectTemplateGroupOption(version, version, null, content));
            }
        }

        return new RdProjectTemplateGroupParameter(
            Name,
            PresentableName,
            parameter.DefaultValue,
            Tooltip,
            options
        );
    }
}