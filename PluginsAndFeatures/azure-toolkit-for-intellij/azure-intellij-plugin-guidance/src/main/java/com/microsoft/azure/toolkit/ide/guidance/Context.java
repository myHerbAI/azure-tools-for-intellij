/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guidance;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Getter
public class Context {

    private static final SimpleTemplateEngine engine = new SimpleTemplateEngine();

    private final Course course;
    private final Project project;
    private final Map<String, Object> parameters = new HashMap<>();

    private final Map<String, List<Consumer<Object>>> propertyChangeListenerMap = new ConcurrentHashMap<>();
    private final List<Consumer<Context>> contextListenerList = new CopyOnWriteArrayList<>();

    public Context(@Nonnull final Course course, @Nullable Map<String, Object> context) {
        this.course = course;
        this.project = course.getProject();
        Optional.ofNullable(context).ifPresent(parameters::putAll);
    }

    public Object getProperty(String key) {
        return parameters.get(key);
    }

    public void setProperty(String key, Object value) {
        final Object origin = parameters.get(key);
        if (!Objects.equals(origin, value)) {
            parameters.put(key, value);
            Optional.ofNullable(propertyChangeListenerMap.get(key))
                    .ifPresent(list -> list.forEach(c -> AzureTaskManager.getInstance().runOnPooledThread(() -> c.accept(value))));
            contextListenerList.forEach(listener -> AzureTaskManager.getInstance().runOnPooledThread(() -> listener.accept(this)));
        }
    }

    public void addPropertyListener(String key, Consumer<Object> listener) {
        final List<Consumer<Object>> consumers = propertyChangeListenerMap.computeIfAbsent(key, ignore -> new ArrayList<>());
        consumers.add(listener);
    }

    public void removePropertyListener(String key, Consumer<Object> listener) {
        Optional.ofNullable(propertyChangeListenerMap.get(key)).ifPresent(list -> list.remove(listener));
    }

    public void addContextListener(Consumer<Context> listener) {
        contextListenerList.add(listener);
    }

    public void removeContextChangeListener(Consumer<Context> listener) {
        contextListenerList.remove(listener);
    }

    public String render(final String template) {
        try {
            final Map<String, Object> bindings = new HashMap<>();
            bindings.put("context", this.parameters);
            final Template tpl = engine.createTemplate(template);
            return tpl.make(bindings).toString()
                    .replace("null", "") // for not exists values, engine will render them as null, remove them in the final result
                    .replace("\\s+", StringUtils.SPACE); // remove extra spaces
        } catch (final Exception e) {
            return StringUtils.EMPTY;
        }
    }
}
