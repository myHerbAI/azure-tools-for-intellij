package com.microsoft.azure.toolkit.intellij.cognitiveservices.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;

@Data
@Builder
@EqualsAndHashCode
public class Configuration {
    public static final Configuration DEFAULT = Configuration.builder().maxResponse(1000).temperature(0.7)
            .topP(0.95).stopSequences(StringUtils.EMPTY).frequencyPenalty(0d).presencePenalty(0d).build();

    private Integer maxResponse;
    private Double temperature;
    private Double topP;
    private String stopSequences;
    private Double frequencyPenalty;
    private Double presencePenalty;
}
