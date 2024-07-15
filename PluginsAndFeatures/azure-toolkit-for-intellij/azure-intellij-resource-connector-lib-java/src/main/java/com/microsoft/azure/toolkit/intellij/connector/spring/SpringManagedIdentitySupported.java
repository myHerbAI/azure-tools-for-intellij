package com.microsoft.azure.toolkit.intellij.connector.spring;

import com.microsoft.azure.toolkit.intellij.connector.IManagedIdentitySupported;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface SpringManagedIdentitySupported<T extends AzResource> extends SpringSupported<T>, IManagedIdentitySupported<T> {
    List<Pair<String, String>> getSpringPropertiesForManagedIdentity(String key);
}
