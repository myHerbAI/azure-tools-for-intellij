package com.microsoft.azure.toolkit.intellij.cognitiveservices.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode
public class Configuration {
    public static final Configuration DEFAULT = Configuration.builder().maxResponse(1000).temperature(0.7)
        .topP(0.95).stopSequences(Collections.emptyList()).frequencyPenalty(0d).presencePenalty(0d).build();

    private Integer maxResponse;
    private Double temperature;
    private Double topP;
    private List<String> stopSequences;
    private Double frequencyPenalty;
    private Double presencePenalty;
}
