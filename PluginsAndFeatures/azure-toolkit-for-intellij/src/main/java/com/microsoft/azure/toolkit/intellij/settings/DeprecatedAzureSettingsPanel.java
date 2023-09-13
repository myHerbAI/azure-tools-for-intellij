/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.settings;

import com.intellij.openapi.project.Project;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.navigation.Place;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

import static com.microsoft.azure.toolkit.intellij.settings.AzureSettingsConfigurable.AZURE_CONFIGURABLE_ID;

// todo: deprecate this panel
public class DeprecatedAzureSettingsPanel {
    public static final String LINK_CONTENT = "<html>Configuration panel here has been deprecated, please switch to <a href=\"\">Tools -> Azure</a> for Azure configuration</html>";
    private HyperlinkLabel lblRedirectLink;
    @Getter
    private JPanel pnlRoot;

    private final Project project;

    public DeprecatedAzureSettingsPanel(Project project) {
        this.project = project;
    }

    private void createUIComponents() {
        lblRedirectLink = new HyperlinkLabel();
        lblRedirectLink.setHtmlText(LINK_CONTENT);
        lblRedirectLink.addHyperlinkListener(e -> {
            final Place place = new Place();
            // refers SettingsEditor.SELECTED_CONFIGURABLE
            place.putPath("settings.editor.selected.configurable", AZURE_CONFIGURABLE_ID);
            getSettingsEditor().navigateTo(place, false);
        });
    }

    private Place.Navigator getSettingsEditor() {
        Container container = this.pnlRoot;
        while (container != null && !(container instanceof Place.Navigator)) {
            container = container.getParent();
        }
        return (Place.Navigator) container;
    }
}
