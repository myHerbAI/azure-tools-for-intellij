/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.appsettings;

import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class AppSettingsNode extends AzResourceNode<AppServiceAppBase<?, ?, ?>> {
    public AppSettingsNode(@Nonnull AppServiceAppBase<?, ?, ?> app) {
        super(app);
        this.withIcon(AzureIcons.Common.VARIABLE)
            .withLabel("App Settings")
            .withDescription("")
            .withTips("Variables passed as environment variables to the application code")
            .withActions(AppServiceActionsContributor.APP_SETTINGS_ACTIONS)
            .addChildren(
                a -> Optional.ofNullable(a.getAppSettings()).map(Map::entrySet).map(s -> s.stream().sorted(Map.Entry.comparingByKey()).toList()).orElse(Collections.emptyList()),
                (e, p) -> new AppSettingNode(e));
    }

    private static class AppSettingNode extends Node<Map.Entry<String, String>> {
        private boolean visible = false;

        public AppSettingNode(@Nonnull Map.Entry<String, String> data) {
            super(data);
            this.withIcon(AzureIcons.Common.VARIABLE)
                .withLabel(data.getKey())
                .withActions(createActionGroup())
                .withDescription(value -> visible ? " = " + data.getValue() : " = ***");
        }

        private IActionGroup createActionGroup() {
            final Action<Map.Entry<String, String>> toggleVisibleAction = new Action<Map.Entry<String, String>>(Action.Id.of("user/appservice.show_app_setting.key"))
                    .withLabel(ignore -> this.visible ? "Hide Value": "Show Value")
                    .withIdParam(Map.Entry::getKey)
                    .withHandler(e -> toggleVisible())
                    .withAuthRequired(false);
            return new ActionGroup(toggleVisibleAction, AppServiceActionsContributor.COPY_APP_SETTING, AppServiceActionsContributor.COPY_APP_SETTING_KEY);
        }

        private void toggleVisible() {
            this.visible = !this.visible;
            this.refreshViewLater(10);
        }
    }
}
