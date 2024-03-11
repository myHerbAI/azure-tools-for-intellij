/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.ide.appservice.webapp.model;

import com.microsoft.azure.toolkit.ide.appservice.model.AzureArtifactConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode()
@SuperBuilder(toBuilder = true)
public class WebAppDeployRunConfigurationModel {
    private AppServiceConfig webAppConfig;
    private AzureArtifactConfig artifactConfig;
    private boolean slotPanelVisible = false;
    private boolean deployToRoot = true;
    private boolean openBrowserAfterDeployment = true;
    // todo: add config for before run tasks
}
