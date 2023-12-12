package com.microsoft.azure.toolkit.intellij.samples.view;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.intellij.samples.model.GithubOrganization;
import com.microsoft.azure.toolkit.intellij.samples.model.GithubRepository;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GithubRepositoriesView {
    private final Project project;
    private final GithubOrganization organization;

    @Getter
    private JPanel contentPanel;
    private JPanel repositoriesPanel;
    private ActionLink nextButton;
    private ActionLink prevButton;
    private SearchTextField searchBox;
    private JPanel loadingPanel;
    private JLabel loadingIcon;
    private JBScrollPane scrollPane;
    private JPanel searchBoxPanel;

    private int page = 1;
    private boolean last = false;
    private boolean first = false;

    private final List<GithubRepositoryPanel> repositoryPanels = new ArrayList<>();

    public GithubRepositoriesView(@Nonnull Project project) {
        this.project = project;
        this.organization = new GithubOrganization("Azure-Samples");
        $$$setupUI$$$();
        init();
    }

    private void init() {
        final JBTextField searchField = this.searchBox.getTextEditor();
        final Dimension size = searchField.getSize();
        final Dimension newSize = new Dimension(size.width, size.height + 4);
        searchField.setPreferredSize(newSize);
        searchField.getEmptyText().setText("Search Azure code samples...");
        searchField.setSize(newSize);
        searchField.setBorder(BorderFactory.createEmptyBorder());
        searchField.setBackground(JBUI.CurrentTheme.EditorTabs.background());
        this.searchBoxPanel.setBackground(JBUI.CurrentTheme.EditorTabs.background());
        this.searchBoxPanel.setBorder(BorderFactory.createCompoundBorder(
            new SideBorder(JBColor.border(), SideBorder.ALL),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));

        this.scrollPane.setBorder(new SideBorder(JBColor.border(), SideBorder.ALL));
        this.loadingIcon.setIcon(IconUtil.scale(IntelliJAzureIcons.getIcon(AzureIcons.Common.REFRESH_ICON), this.loadingIcon, 1.5f));
        this.loadPage(1);
        this.searchBox.requestFocusInWindow();
        this.searchBox.requestFocus();
        final TailingDebouncer debouncer = new TailingDebouncer(() -> this.loadPage(1), 500);
        this.searchBox.addDocumentListener((TextDocumentListenerAdapter) debouncer::debounce);
    }

    public void loadPrevPage(ActionEvent e) {
        if (!first) {
            loadPage(--page);
        }
    }

    public void loadNextPage(ActionEvent e) {
        if (!last) {
            loadPage(++page);
        }
    }

    private void loadPage(int page) {
        this.page = page;
        this.repositoriesPanel.removeAll();
        this.repositoriesPanel.setVisible(false);
        this.loadingPanel.setVisible(true);
        this.prevButton.setEnabled(false);
        this.nextButton.setEnabled(false);
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        tm.runInBackground("load github.com/Azure-Samples repositories", () -> organization.search(this.searchBox.getText(), page)).thenAccept(result -> tm.runLater(() -> {
            final int pages = result.count() / GithubOrganization.DEFAULT_PAGE_SIZE + ((result.count() % GithubOrganization.DEFAULT_PAGE_SIZE == 0) ? 0 : 1);
            last = page >= pages;
            first = page <= 1; // pages can be 0
            this.setRepositories(result.items());
            this.loadingPanel.setVisible(false);
            this.repositoriesPanel.setVisible(true);
            this.scrollPane.revalidate();
            this.scrollPane.repaint();
            this.nextButton.setEnabled(!last);
            this.prevButton.setEnabled(!first);
        }));
    }

    private void setRepositories(final List<GithubRepository> repositories) {
        if (CollectionUtils.isEmpty(repositories)) {
            return;
        }
        this.repositoryPanels.clear();
        for (int i = 0; i < repositories.size(); i++) {
            final GithubRepository repository = repositories.get(i);
            final GithubRepositoryPanel repositoryPanel = new GithubRepositoryPanel(repository, this.project);
            addSelectionListener(repositoryPanel.getContentPanel(), new RepositorySelectionListener(repositoryPanel));
            if (i != 0) {
                this.repositoriesPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
            }
            this.repositoriesPanel.add(repositoryPanel.getContentPanel());
            this.repositoryPanels.add(repositoryPanel);
        }
        this.repositoryPanels.get(0).toggleSelectedStatus(true);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    public void setVisible(boolean visible) {
        this.contentPanel.setVisible(visible);
    }

    private void addSelectionListener(@Nonnull final JComponent component, @Nonnull MouseListener mouseListener) {
        component.addMouseListener(mouseListener);
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Arrays.stream(component.getComponents()).filter(c -> c instanceof JComponent).forEach(child -> addSelectionListener((JComponent) child, mouseListener));
    }

    class RepositorySelectionListener extends MouseAdapter {
        private final GithubRepositoryPanel panel;

        RepositorySelectionListener(@Nonnull final GithubRepositoryPanel panel) {
            super();
            this.panel = panel;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            repositoryPanels.forEach(panel -> panel.toggleSelectedStatus(false));
            panel.toggleSelectedStatus(true);
        }
    }

    private void createUIComponents() {
        this.repositoriesPanel = new JPanel();
        this.repositoriesPanel.setLayout(new BoxLayout(this.repositoriesPanel, BoxLayout.Y_AXIS));
        this.prevButton = new ActionLink("Previous page", this::loadPrevPage);
        this.nextButton = new ActionLink("Next page", this::loadNextPage);
        this.prevButton.setAutoHideOnDisable(false);
        this.nextButton.setAutoHideOnDisable(false);
        this.prevButton.setIcon(AllIcons.General.ChevronLeft);
        this.nextButton.setIcon(AllIcons.General.ChevronRight, true);
    }
}
