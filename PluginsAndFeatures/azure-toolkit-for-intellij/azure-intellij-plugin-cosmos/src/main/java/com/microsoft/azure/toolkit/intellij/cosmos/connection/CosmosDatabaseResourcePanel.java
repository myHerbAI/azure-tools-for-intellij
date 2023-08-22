package com.microsoft.azure.toolkit.intellij.cosmos.connection;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDatabase;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class CosmosDatabaseResourcePanel<T extends ICosmosDatabase, E extends CosmosDBAccount> implements AzureFormJPanel<Resource<T>> {
    private JPanel contentPanel;
    private CosmosDBAccountComboBox<E> cbAccount;
    private CosmosDatabaseComboBox<T, E> cbDatabase;
    private SubscriptionComboBox cbSubscription;
    private HyperlinkLabel lblCreate;

    private final Function<Subscription, ? extends List<? extends E>> accountLoader;
    private final Function<E, ? extends List<? extends T>> databaseLoader;
    private final ResourceDefinition<T> resourceDefinition;

    public CosmosDatabaseResourcePanel(@Nonnull final ResourceDefinition<T> resourceDefinition, @Nonnull final Function<Subscription, ? extends List<? extends E>> accountLoader,
                                       @Nonnull final Function<E, ? extends List<? extends T>> databaseLoader) {
        this.accountLoader = accountLoader;
        this.databaseLoader = databaseLoader;
        this.resourceDefinition = resourceDefinition;
        $$$setupUI$$$();
        this.cbSubscription.setRequired(true);
        this.cbAccount.setRequired(true);
        this.cbDatabase.setRequired(true);
        this.cbSubscription.addItemListener(this::onSubscriptionChanged);
        this.cbAccount.addItemListener(this::onAccountChanged);

        this.lblCreate.setHtmlText("<html><a href=\"\">Create new Cosmos DB account</a> in Azure.</html>");
        this.lblCreate.addHyperlinkListener(e -> {
            final DataContext context = DataManager.getInstance().getDataContext(this.lblCreate);
            final AnActionEvent event = AnActionEvent.createFromInputEvent(e.getInputEvent(), "CosmosDatabaseResourcePanel", new Presentation(), context);
            final DialogWrapper dialog = DialogWrapper.findInstance(this.contentPanel);
            if (dialog != null) {
                dialog.close(DialogWrapper.CLOSE_EXIT_CODE);
                final AzureCosmosService service = Azure.az(AzureCosmosService.class);
                AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.CREATE).bind(service).handle(service, event);
            }
        });
    }

    private void onSubscriptionChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.cbAccount.setSubscription(subscription);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.cbAccount.setSubscription(null);
        }
    }

    private void onAccountChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final E account = (E) e.getItem();
            this.cbDatabase.setAccount(account);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.cbDatabase.setAccount(null);
        }
    }

    @Override
    public JPanel getContentPanel() {
        return contentPanel;
    }

    @Override
    public Resource<T> getValue() {
        return Optional.ofNullable(cbDatabase.getValue()).map(resourceDefinition::define).orElse(null);
    }

    @Override
    public void setValue(Resource<T> data) {
        Optional.ofNullable(data).map(Resource::getData).ifPresent(database -> {
            final ResourceId resourceId = ResourceId.fromString(database.getId());
            cbSubscription.setValue(database.getSubscription());
            cbAccount.setValue(new AzureComboBox.ItemReference<>(resourceId.parent().name(), AzResource::getName));
            cbDatabase.setValue(new AzureComboBox.ItemReference<>(database.getName(), AzResource::getName));
        });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbAccount = new CosmosDBAccountComboBox<>(accountLoader);
        this.cbDatabase = new CosmosDatabaseComboBox<>(databaseLoader);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbDatabase, cbSubscription, cbAccount);
    }
}
