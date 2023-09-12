/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class SystemMessage {
    @EqualsAndHashCode.Exclude
    private String name;
    private String systemMessage;
    @EqualsAndHashCode.Exclude
    private Boolean isDefault;
    @Builder.Default
    private List<Example> examples = new ArrayList<>();

    @Data
    @Builder
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Example {
        private String user;
        private String assistant;
    }
}
