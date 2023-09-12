/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.ToolbarDecorator;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.ProjectUtils;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.table.AppSettingsTable;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class ImportAppSettingsDialog extends JDialog implements ImportAppSettingsView {
    public static final String LOADING_TEXT = "Loading...";
    public static final String EMPTY_TEXT = "Empty";
    public static final String REFRESH_TEXT = "Refreshing...";

    private static final String LOCAL_SETTINGS_JSON = "local.settings.json";
    private JPanel contentPanel;
    private JButton buttonOK;
    private JButton buttonCancel;
    private ComboBox<Object> cbAppSettingsSource;
    private AppSettingsTable tblAppSettings;
    private JLabel lblAppSettingsSource;
    private JPanel pnlAppSettings;
    private JCheckBox chkErase;

    private boolean eraseExistingSettings;
    private Map<String, String> appSettings = null;
    private final Project project;
    private final AppSettingsDialogPresenter<ImportAppSettingsDialog> presenter = new AppSettingsDialogPresenter<>();

    public ImportAppSettingsDialog(@Nullable final Project project) {
        super();
        this.project = project;
        setContentPane(contentPanel);
        setModal(true);
        setTitle(message("function.appSettings.import.title"));
        setMinimumSize(new Dimension(-1, 250));
        setAlwaysOnTop(true);
        getRootPane().setDefaultButton(buttonOK);

        this.presenter.onAttachView(this);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        cbAppSettingsSource.setRenderer(new ListCellRendererWrapper<>() {
            @Override
            public void customize(JList list, Object object, int index, boolean isSelected, boolean cellHasFocus) {
                if (object instanceof FunctionApp) {
                    setIcon(IntelliJAzureIcons.getIcon(AzureIcons.FunctionApp.MODULE));
                    setText(((FunctionApp) object).getName());
                } else if (object instanceof VirtualFile) {
                    setText(((VirtualFile) object).getPath());
                    setIcon(AllIcons.FileTypes.Json);
                } else if (object instanceof String) {
                    setText(object.toString());
                }
            }
        });
        this.cbAppSettingsSource.setUsePreferredSizeAsMinimum(false);

        cbAppSettingsSource.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                final Object item = e.getItem();
                if (item instanceof FunctionApp) {
                    presenter.onLoadFunctionAppSettings((FunctionApp) item);
                } else if (item instanceof VirtualFile) {
                    presenter.onLoadLocalSettings(((VirtualFile) item).toNioPath());
                }
            }
        });

        chkErase.addActionListener(e -> eraseExistingSettings = chkErase.isSelected());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPanel.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        this.loadAppSettingSources();
        pack();
    }

    // todo: migrate to AzureComboBox framework
    private void loadAppSettingSources() {
        final Project project = Optional.ofNullable(this.project).orElseGet(ProjectUtils::getProject);
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final Collection<VirtualFile> files = ReadAction.compute(() -> FilenameIndex.getVirtualFilesByName("local.settings.json", GlobalSearchScope.projectScope(project)));
            AzureTaskManager.getInstance().runLater(() -> files.forEach(ImportAppSettingsDialog.this.cbAppSettingsSource::addItem), AzureTask.Modality.ANY);
        });
        presenter.onLoadFunctionApps();
    }

    @Override
    public void fillFunctionApps(List<FunctionApp> functionApps) {
        functionApps.forEach(functionAppResourceEx -> cbAppSettingsSource.addItem(functionAppResourceEx));
        pack();
    }

    @Override
    public void fillFunctionAppSettings(Map<String, String> appSettings) {
        tblAppSettings.setAppSettings(appSettings);
        if (appSettings.size() == 0) {
            tblAppSettings.getEmptyText().setText(EMPTY_TEXT);
        }
    }

    @Override
    public void beforeFillAppSettings() {
        tblAppSettings.getEmptyText().setText(LOADING_TEXT);
        tblAppSettings.clear();
    }

    public boolean shouldErase() {
        return eraseExistingSettings;
    }

    public Map<String, String> getAppSettings() {
        return this.appSettings;
    }

    private void createUIComponents() {
        tblAppSettings = new FunctionAppSettingsTable("");
        tblAppSettings.getEmptyText().setText(LOADING_TEXT);
        pnlAppSettings = ToolbarDecorator.createDecorator(tblAppSettings).createPanel();
    }

    @AzureOperation("user/function.do_import_app_settings")
    private void onOK() {
        this.appSettings = tblAppSettings.getAppSettings();
        dispose();
    }

    @AzureOperation("user/function.cancel_import_app_settings")
    private void onCancel() {
        this.appSettings = null;
        this.eraseExistingSettings = false;
        dispose();
    }
}
