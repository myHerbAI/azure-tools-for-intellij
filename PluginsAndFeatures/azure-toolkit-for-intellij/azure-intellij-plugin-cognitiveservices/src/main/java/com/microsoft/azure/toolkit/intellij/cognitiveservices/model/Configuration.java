package com.microsoft.azure.toolkit.intellij.cognitiveservices.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class Configuration {
    private Integer maxResponse;
    private Double temperature;
    private Double topP;
    private String stopSequences;
    private Double frequencyPenalty;
    private Double presencePenalty;
}
