/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function.coretools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.annotation.Nullable;
import java.io.IOException;


public class ReleaseService {
    private final static String FUNCTIONS_CORE_TOOLS_FEED_URL = "https://aka.ms/func-core-tools-feed";
    private final String FAILED_TO_GET_FEED_INFO = "failed to get release feed info";
    private final OkHttpClient client = new OkHttpClient();
    private final static ReleaseService instance = new ReleaseService();

    public static ReleaseService getInstance() {
        return instance;
    }

    @Nullable
    public ReleaseFeedData getReleaseFeedData() {
        final Request request = new Request.Builder()
                .url(FUNCTIONS_CORE_TOOLS_FEED_URL)
                .build();
        final ObjectMapper jsonMapper = new JsonMapper()
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (final Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return jsonMapper.readValue(response.body().string(), ReleaseFeedData.class);
            }
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException(FAILED_TO_GET_FEED_INFO, e);
        }
        return null;
    }
}
