/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.dotazure;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule.RESOURCES_FILE;

@Slf4j
public class ResourceManager {
    private static final ExtensionPointName<ResourceDefinition<?>> exPoints =
        ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.connectorResourceType");
    private static Map<String, ResourceDefinition<?>> definitions;
    private static final String ATTR_DEFINITION = "type";
    private static final String ELEMENT_NAME_RESOURCES = "resources";
    private static final String ELEMENT_NAME_RESOURCE = "resource";
    private final Set<Resource<?>> resources = new LinkedHashSet<>();
    @Getter
    private final Profile profile;

    public ResourceManager(@Nonnull final Profile profile) {
        this.profile = profile;
        try {
            this.load();
        } catch (final Exception e) {
            final Throwable root = ExceptionUtils.getRootCause(e);
            if (!(root instanceof ProcessCanceledException)) {
                throw new AzureToolkitRuntimeException(e);
            }
        }
    }

    public static ResourceDefinition<?> getDefinition(String type) {
        return getDefinitionsMap().get(type);
    }

    public synchronized static Map<String, ResourceDefinition<?>> getDefinitionsMap() {
        if (MapUtils.isEmpty(definitions)) {
            definitions = exPoints.getExtensionList().stream()
                .collect(Collectors.toMap(ResourceDefinition::getName, r -> r));
        }
        return definitions;
    }

    public static List<ResourceDefinition<?>> getDefinitions() {
        return getDefinitionsMap().values().stream()
            .sorted(Comparator.comparing(ResourceDefinition::getTitle))
            .sorted(Comparator.comparing((ResourceDefinition<?> d) -> !d.isEnvPrefixSupported()))
            .collect(Collectors.toList());
    }

    public static List<ResourceDefinition<?>> getDefinitions(int role) {
        return getDefinitions().stream()
            .filter(d -> (d.getRole() & role) == role)
            .collect(Collectors.toList());
    }

    @AzureOperation("internal/connector.add_resource")
    public synchronized void addResource(Resource<?> resource) {
        resources.remove(resource);
        resources.add(resource);
    }

    @Nullable
    public Resource<?> getResourceById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        return resources.stream().filter(e -> StringUtils.equals(e.getId(), id)).findFirst().orElse(null);
    }

    @ExceptionNotification
    @AzureOperation("boundary/connector.save_resources")
    void save() throws IOException {
        final Element resourcesEle = new Element(ELEMENT_NAME_RESOURCES);
        // todo: whether to save invalid resources?
        this.resources.stream().filter(Resource::isValidResource).forEach(resource -> {
            final Element resourceEle = new Element(ELEMENT_NAME_RESOURCE);
            try {
                if (resource.writeTo(resourceEle)) {
                    resourceEle.setAttribute(ATTR_DEFINITION, resource.getDefinition().getName());
                    resourcesEle.addContent(resourceEle);
                }
            } catch (final Exception e) {
                log.warn(String.format("error occurs when persist resource of type '%s'", resource.getDefinition().getName()), e);
            }
        });
        final VirtualFile resourcesFile = this.profile.getProfileDir().findOrCreateChildData(this, RESOURCES_FILE);
        JDOMUtil.write(resourcesEle, resourcesFile.toNioPath());
    }

    @ExceptionNotification
    @AzureOperation(name = "boundary/connector.load_resources")
    void load() throws Exception {
        final VirtualFile resourcesFile = this.profile.getProfileDir().findChild(RESOURCES_FILE);
        if (Objects.isNull(resourcesFile) || resourcesFile.contentsToByteArray().length < 1) {
            return;
        }
        this.resources.clear();
        final Element resourcesEle = JDOMUtil.load(resourcesFile.toNioPath());
        for (final Element resourceEle : resourcesEle.getChildren()) {
            final String resDef = resourceEle.getAttributeValue(ATTR_DEFINITION);
            final ResourceDefinition<?> definition = ResourceManager.getDefinition(resDef);
            try {
                Optional.ofNullable(definition).map(d -> definition.read(resourceEle)).ifPresent(this::addResource);
            } catch (final Exception e) {
                log.warn(String.format("error occurs when load a resource of type '%s'", resDef), e);
            }
        }
    }
}
