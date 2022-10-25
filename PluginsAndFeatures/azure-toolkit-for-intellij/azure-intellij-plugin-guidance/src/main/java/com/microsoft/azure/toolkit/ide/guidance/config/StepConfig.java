/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guidance.config;

import lombok.Data;

import java.util.List;

@Data
public class StepConfig {
    private String name;
    private String title;
    private String description;
    private TaskConfig task;
    private boolean continueOnError = false;
    private List<InputConfig> inputs;
}
