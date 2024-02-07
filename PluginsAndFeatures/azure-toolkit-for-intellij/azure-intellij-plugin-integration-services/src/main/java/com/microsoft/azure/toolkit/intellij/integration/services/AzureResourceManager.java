package com.microsoft.azure.toolkit.intellij.integration.services;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.XCollection;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

@State(
    name = "ServiceViewAzureResources",
    storages = {@Storage("service.azure-resource.xml")}
)
public class AzureResourceManager implements PersistentStateComponent<AzureResourceManager.State> {

    private final List<ResourceId> myResources = new ArrayList<>();

    public AzureResourceManager() {

    }

    public static AzureResourceManager getInstance() {
        return ApplicationManager.getApplication().getService(AzureResourceManager.class);
    }

    public @Unmodifiable List<ResourceId> getResources() {
        return List.copyOf(this.myResources);
    }

    @Override
    public @Nullable AzureResourceManager.State getState() {
        final State state = new State();
        state.myResources.addAll(this.myResources.stream().map(ResourceId::id).toList());
        return state;
    }

    public void loadState(@NotNull State state) {
        this.myResources.clear();
        state.myResources.stream().map(ResourceId::fromString).forEach(this.myResources::add);
    }

    public void addResource(@NotNull AbstractAzResource<?, ?, ?> resource) {
        if (this.myResources.stream().anyMatch(r -> r.id().equalsIgnoreCase(resource.getId()))) { // ResourceId doesn't override equals
            return;
        }
        this.myResources.add(ResourceId.fromString(resource.getId()));
        ApplicationManager.getApplication().getMessageBus().syncPublisher(Listener.TOPIC).resourceAdded(resource);
    }

    public void removeResource(@NotNull AbstractAzResource<?, ?, ?> resource) {
        this.myResources.removeIf(r -> r.id().equalsIgnoreCase(resource.getId()));
        ApplicationManager.getApplication().getMessageBus().syncPublisher(Listener.TOPIC).resourceRemoved(resource);
    }

    public interface Listener {
        Topic<Listener> TOPIC = Topic.create("ServiceViewAzureResourceManager.topic", Listener.class);

        void resourceAdded(AbstractAzResource<?, ?, ?> resource);

        void resourceRemoved(AbstractAzResource<?, ?, ?> resource);
    }

    public static class State {
        @Property(
            surroundWithTag = false
        )
        @XCollection
        public List<String> myResources = new ArrayList<>();
    }
}
