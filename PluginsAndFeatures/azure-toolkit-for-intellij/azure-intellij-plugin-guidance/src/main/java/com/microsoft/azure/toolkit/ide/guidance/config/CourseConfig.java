/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guidance.config;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CourseConfig {
    private String uri;
    private String name;
    private String title;
    private String description;
    private String repository;
    private String icon;
    private Integer priority = 10;
    private List<String> tags;
    private List<PhaseConfig> phases;
    private Map<String, Object> context;
}
