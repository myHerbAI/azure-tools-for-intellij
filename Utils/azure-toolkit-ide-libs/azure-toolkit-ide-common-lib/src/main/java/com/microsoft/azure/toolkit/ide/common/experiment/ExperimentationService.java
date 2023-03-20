/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.experiment;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class ExperimentationService {
    private static final String ASSIGNMENT_CONTEXT = "AssignmentContext";
    private final String NAME_SPACE = "IntelliJ";
    private CloseableHttpClient client;
    private HttpRequestBase request;
    private String endPoint;
    private final Map<String, String> featuresCache = new HashMap<>();
    private final Map<String, String> expParameters = new HashMap<>();
    private static final ObjectMapper JSON_MAPPER = new JsonMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ExperimentationService withAudienceFilters(Map<String, String> audienceFilters) {
        this.expParameters.putAll(audienceFilters);
        return this;
    }

    public ExperimentationService withAssignmentIds(Map<String, String> assignmentIds) {
        this.expParameters.putAll(assignmentIds);
        return this;
    }

    public ExperimentationService withEndPoint(String endPoint) {
        this.endPoint = endPoint;
        return this;
    }

    public ExperimentationService create() {
        final StringBuilder builder = new StringBuilder();
        expParameters.forEach((key, value) -> builder.append(String.format("%s=%s,", key, value)));
        this.client = HttpClients.custom().setSSLContext(Azure.az().config().getSslContext()).build();
        this.request = new HttpGet(endPoint);
        this.request.addHeader("x-exp-sdk-version", "Microsoft.VariantAssignment.Client/1.0.0");
        this.request.addHeader("x-exp-parameters", builder.toString());
        updateFeatures();
        return this;
    }

    private void updateFeatures() {
        try (final CloseableHttpResponse response = client.execute(request)) {
            final HttpEntity entity = response.getEntity();
            if (Objects.nonNull(entity)) {
                final AssignmentResponse assignmentResponse = JSON_MAPPER.readValue(entity.getContent(), AssignmentResponse.class);
                final List<AssignmentResponse.Config> configList = assignmentResponse.getConfigs();
                configList.forEach(config -> {
                    if (Objects.equals(config.getId(), NAME_SPACE)) {
                        featuresCache.putAll(config.getParameters());
                    }
                });
                featuresCache.put(ASSIGNMENT_CONTEXT, assignmentResponse.getAssignmentContext());
            }
            client.close();
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    @Nullable
    public String getFeatureVariable(String featureFlagName) {
        return featuresCache.get(featureFlagName);
    }

    @Nullable
    public String getAssignmentContext() {
        return featuresCache.get(ASSIGNMENT_CONTEXT);
    }

}
