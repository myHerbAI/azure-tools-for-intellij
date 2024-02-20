package com.microsoft.azure.toolkit.intellij.azuresdk;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.MachineTaggingService;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.WorkspaceTaggingService;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessage;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeatureAdvertisementService {

    private static final Map<String, String> MACHINE_SERVICES = new LinkedHashMap<>() {{
        put("docker", "Microsoft.ContainerRegistry");
        put("kubectl", "Microsoft.ContainerService");
        put("podman", "Microsoft.ContainerService");

        put("mysql", "Microsoft.DBforMySQL");
        put("psql", "Microsoft.DBforPostgreSQL");
        put("sqlservr", "Microsoft.Sql");
        put("redis-server", "Microsoft.Cache");
        put("redis-cli", "Microsoft.Cache");
        put("azurite", "Microsoft.Storage");
        put("Microsoft.Azure.Cosmos.Emulator", "Microsoft.DocumentDB");
    }};


    private static final Map<String, String> PROJECT_SERVICES = new LinkedHashMap<>() {{
        put("openai", "Microsoft.CognitiveServices");
        put("azure_openai", "Microsoft.CognitiveServices");
        put("redis", "Microsoft.Cache");
        put("eventhubs", "Microsoft.EventHub");
        put("servicebus", "Microsoft.ServiceBus");
        put("azure_storage", "Microsoft.Storage");
        put("cosmos", "Microsoft.DocumentDB");
    }};
    public static final String MSG_TEMPLATE = "Current project is detected as possibly using %s. You can managed them in %s after signing-in";

    public static void advertiseProjectService(@Nonnull final Project project) {
        final String service = getNextProjectAdService(project);
        if (service != null) {
            doAdvertise(project, service);
        }
    }

    @AzureOperation("auto/sdk.advertise_service")
    private static void doAdvertise(@Nonnull Project project, String service) {
        final IntellijAzureMessage message = (IntellijAzureMessage) buildMessage(service);
        if (message != null) {
            message.setProject(project).setPriority(IntellijAzureMessage.PRIORITY_HIGH).show(AzureMessager.getMessager());
            PropertiesComponent.getInstance().setValue("azure.advertisement.project.service", service);
        }
    }

    @Nullable
    private static IAzureMessage buildMessage(@Nonnull String service) {
        final Action<Object> signInAction = AzureActionManager.getInstance().getAction(Action.AUTHENTICATE);

        final Action<Object> focusService = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.SELECT_RESOURCE_IN_EXPLORER);
        final Action<Object> openExplorer = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_AZURE_EXPLORER);
        final List<AzService> services = Azure.getServices(service);
        final Action<Object> openExplorerAction = services.isEmpty() ? openExplorer : focusService.bind(services.get(0)).withLabel("Open in Azure Explorer");

        return switch (service) {
            case "Microsoft.CognitiveServices" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + ", and try your own \"%s\" with \"%s\" model deployments in the integrated %s.", "Azure OpenAI", "Azure Explorer",
                    "Copilot", "GPT*", "AI playground"), signInAction, openExplorerAction);
            case "Microsoft.Cache" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + ", and explore or manage cached data in the integrated %s.", "Azure Redis Cache", "Azure Explorer",
                    "Redis Explorer"), signInAction, openExplorerAction);
            case "Microsoft.EventHub" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + ", and send messages to or monitor messages with the integrated %s.", "Azure EventHubs", "Azure Explorer",
                    "Event Hub Explorer"), signInAction, openExplorerAction);
            case "Microsoft.ServiceBus" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + ", and send messages to or monitor messages with the integrated %s.", "Azure ServiceBus", "Azure Explorer",
                    "Service Bus Explorer"), signInAction, openExplorerAction);
            case "Microsoft.Storage" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + " with rich functions for browsing and management of blobs and files.", "Azure Storage Accounts", "Azure Explorer"
                ), signInAction, openExplorerAction);
            case "Microsoft.DocumentDB" -> AzureMessager.getMessager().buildInfoMessage(
                AzureString.format(MSG_TEMPLATE + " with rich functions for browsing and management of documents.", "Azure CosmosDB", "Azure Explorer"
                ), signInAction, openExplorerAction);
            default -> null;
        };
    }

    @Nonnull
    public static List<String> getProjectServices(Project project) {
        final Set<String> tags = WorkspaceTaggingService.getWorkspaceTags(project);
        return PROJECT_SERVICES.entrySet().stream()
            .filter(e -> tags.contains(e.getKey()))
            .map(Map.Entry::getValue).toList();
    }

    @Nonnull
    public static List<String> getMachineServices() {
        final Set<String> tags = MachineTaggingService.getMachineTags();
        return MACHINE_SERVICES.entrySet().stream()
            .filter(e -> tags.contains(e.getKey()))
            .map(Map.Entry::getValue).toList();
    }

    @Nullable
    public static String getNextProjectAdService(@Nonnull final Project project) {
        final List<String> services = getProjectServices(project);
        if (!services.isEmpty()) {
            final String lastService = PropertiesComponent.getInstance().getValue("azure.advertisement.project.service", "");
            final int index = services.indexOf(lastService);
            return services.get((index + 1) % services.size());
        }
        return null;
    }

    @Nullable
    public static String getNextMachineAdService() {
        final List<String> services = getMachineServices();
        if (!services.isEmpty()) {
            final String lastService = PropertiesComponent.getInstance().getValue("azure.advertisement.machine.service", "");
            final int index = services.indexOf(lastService);
            return services.get((index + 1) % services.size());
        }
        return null;
    }
}
