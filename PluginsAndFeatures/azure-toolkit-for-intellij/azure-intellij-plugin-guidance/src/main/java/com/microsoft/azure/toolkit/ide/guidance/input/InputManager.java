package com.microsoft.azure.toolkit.ide.guidance.input;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.microsoft.azure.toolkit.ide.guidance.Context;
import com.microsoft.azure.toolkit.ide.guidance.config.InputConfig;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InputManager {
    private static final ExtensionPointName<GuidanceInputProvider> exPoints =
            ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.guidanceInputProvider");
    private static List<GuidanceInputProvider> providers;

    public synchronized static List<GuidanceInputProvider> getInputProviders() {
        if (CollectionUtils.isEmpty(providers)) {
            providers = exPoints.extensions().collect(Collectors.toList());
        }
        return providers;
    }

    @Nonnull
    public static GuidanceInput<?> createInputComponent(@Nonnull final InputConfig config, @Nonnull final Context context) {
        return getInputProviders().stream()
                .map(provider -> provider.createInputComponent(config, context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new AzureToolkitRuntimeException(AzureString.format("Unsupported input context %s", config.getName()).toString()));
    }
}
