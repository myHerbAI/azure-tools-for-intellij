/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.samples.view;

import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.WrapLayout;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.samples.model.GithubRepository;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseListener;

public class GithubRepositoryPanel {
    private static final ColorKey TAG_BACKGROUND = ColorKey.createColorKey("Tag.background", JBUI.CurrentTheme.ActionButton.hoverBackground());
    @Getter
    private final GithubRepository repo;

    @Getter
    private JPanel contentPanel;
    private JLabel titleLabel;
    private JTextPane descriptionPane;
    private JPanel topicsPanel;

    public static final JBColor NOTIFICATION_BACKGROUND_COLOR =
        JBColor.namedColor("StatusBar.hoverBackground", new JBColor(15595004, 4606541));

    public GithubRepositoryPanel(@Nonnull final GithubRepository repo, @Nonnull final Project project) {
        super();
        this.repo = repo;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        // render course
        // this.lblIcon.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
        this.titleLabel.setText(repo.getName());
        final Icon icon = IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE);
        if (icon != null) {
            this.titleLabel.setIcon(icon);
        }

        this.descriptionPane.setFont(JBFont.medium());
        this.descriptionPane.setText(repo.getDescription());
        this.descriptionPane.setOpaque(false);
        this.descriptionPane.setVisible(StringUtils.isNotBlank(repo.getDescription()));

        this.repo.getTopics().forEach(tag -> this.topicsPanel.add(createTopicLabel(tag)));
    }

    public void toggleSelectedStatus(final boolean isSelected) {
        this.contentPanel.setBackground(isSelected ? NOTIFICATION_BACKGROUND_COLOR : UIUtil.getLabelBackground());
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    public void addMouseListener(@Nonnull final MouseListener coursePanelListener) {
        this.contentPanel.addMouseListener(coursePanelListener);
    }

    private JLabel createTopicLabel(String tag) {
        final JLabel label = new JLabel(tag);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        final Border border = new TopicLabelLineBorder(JBColor.namedColor("Tag.border", Gray.xC0), 1);
        final Border padding = BorderFactory.createEmptyBorder(0, 6, 2, 6);
        final Border margin = BorderFactory.createEmptyBorder(0, 0, 3, 0);
        label.setBackground(JBUI.CurrentTheme.ActionButton.hoverBackground());
        label.setBorder(BorderFactory.createCompoundBorder(margin, BorderFactory.createCompoundBorder(border, padding)));
        label.setFont(JBFont.regular().lessOn(1));
        label.setOpaque(false);
        return label;
    }

    private void createUIComponents() {
        this.contentPanel = new JPanel();
        this.topicsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 0));
        this.topicsPanel.setOpaque(false);
        this.topicsPanel.setBorder(JBUI.Borders.emptyLeft(-8));
    }
}
