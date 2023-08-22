/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode
public class SystemMessage {
    private String name;
    private String systemMessage;
    @Builder.Default
    private List<Example> examples = new ArrayList<>();

    @Data
    @Builder
    @EqualsAndHashCode
    public static class Example {
        private String user;
        private String assistant;
    }
}
