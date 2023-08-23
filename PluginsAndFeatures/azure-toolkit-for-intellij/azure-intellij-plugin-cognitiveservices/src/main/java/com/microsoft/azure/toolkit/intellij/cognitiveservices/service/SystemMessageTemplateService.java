package com.microsoft.azure.toolkit.intellij.cognitiveservices.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.SystemMessage;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SystemMessageTemplateService {
    private static final ObjectMapper JSON_MAPPER = new JsonMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String TEMPLATE_JSON = "/template.json";

    // todo: get latest template from github
    @Cacheable(value = "openai-system-message-template")
    @AzureOperation(name = "boundary/openai.load_system_message_template")
    public static List<SystemMessage> loadTemplates() {
        try (final InputStream stream = SystemMessageTemplateService.class.getResourceAsStream(TEMPLATE_JSON)) {
            final ObjectReader reader = JSON_MAPPER.readerFor(SystemMessage.class);
            final MappingIterator<SystemMessage> data = reader.readValues(stream);
            return data.readAll();
        } catch (final IOException e) {
            final String message = String.format("failed to load system message template from \"%s\"", TEMPLATE_JSON);
            throw new AzureToolkitRuntimeException(message, e);
        }
    }
}
