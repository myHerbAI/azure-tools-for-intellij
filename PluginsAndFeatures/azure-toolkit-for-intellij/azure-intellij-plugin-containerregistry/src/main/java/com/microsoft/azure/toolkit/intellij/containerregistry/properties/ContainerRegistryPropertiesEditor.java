/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.properties;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.intellij.common.action.AzureActionListenerWrapper;
import com.microsoft.azure.toolkit.intellij.common.component.UIUtils;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azuretools.core.mvp.model.container.ContainerRegistryMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.core.mvp.ui.containerregistry.ContainerRegistryProperty;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryPropertyMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryPropertyViewPresenter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ContainerRegistryPropertiesEditor extends BaseEditor implements ContainerRegistryPropertyMvpView {

    public static final String ID = ContainerRegistryPropertiesEditor.class.getName();
    private static final String INSIGHT_NAME = "AzurePlugin.IntelliJ.Editor.ContainerRegistryExplorer";
    private final ContainerRegistryPropertyViewPresenter<ContainerRegistryPropertiesEditor> containerPropertyPresenter;
    private final StatusBar statusBar;

    private static final String REFRESH = "Refresh";
    private static final String PREVIOUS_PAGE = "Previous page";
    private static final String NEXT_PAGE = "Next page";
    private static final String TAG = "Tag";
    private static final String REPOSITORY = "Repository";
    private static final String PROPERTY = "Properties";
    private static final String TABLE_LOADING_MESSAGE = "Loading...";
    private static final String TABLE_EMPTY_MESSAGE = "No available items.";
    private static final String ADMIN_NOT_ENABLED = "Admin user is not enabled.";
    private static final String PULL_IMAGE = "Pull Image";
    private static final String IMAGE_PULL_SUCCESS = "%s is successfully pulled.";
    private static final String REPO_TAG_NOT_AVAILABLE = "Cannot get Current repository and tag";

    private boolean isAdminEnabled;
    private String registryId = "";
    private String subscriptionId = "";
    private String password = "";
    private String password2 = "";
    private String currentRepo;
    private String currentTag;

    private JPanel pnlMain;
    private JTextField txtName;
    private JTextField txtResGrp;
    private JTextField txtSubscription;
    private JTextField txtRegion;
    private JTextField txtServerUrl;
    private JTextField txtUserName;
    private JButton btnPrimaryPassword;
    private JButton btnSecondaryPassword;
    private JTextField txtType;
    private JLabel lblSecondaryPwd;
    private JLabel lblPrimaryPwd;
    private JLabel lblUserName;
    private JButton btnEnable;
    private JButton btnDisable;
    private JPanel pnlPropertyHolder;
    private JPanel pnlProperty;
    private JPanel pnlRepoTable;
    private JPanel pnlTagTable;
    private JBTable tblRepo;
    private JBTable tblTag;
    private AnActionButton btnRepoRefresh;
    private AnActionButton btnRepoPrevious;
    private AnActionButton btnRepoNext;
    private AnActionButton btnTagRefresh;
    private AnActionButton btnTagPrevious;
    private AnActionButton btnTagNext;
    private final JPopupMenu menu;

    /**
     * Constructor of ACR property view.
     */
    public ContainerRegistryPropertiesEditor(@Nonnull Project project, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile);
        this.containerPropertyPresenter = new ContainerRegistryPropertyViewPresenter<>();
        this.containerPropertyPresenter.onAttachView(this);
        statusBar = WindowManager.getInstance().getStatusBar(project);

        disableTxtBoard();
        makeTxtOpaque();

        btnPrimaryPassword.addActionListener(event -> {
            try {
                copyToSystemClipboard(password);
            } catch (final Exception e) {
                onError(e.getMessage());
            }
        });

        btnSecondaryPassword.addActionListener(event -> {
            try {
                copyToSystemClipboard(password2);
            } catch (final Exception e) {
                onError(e.getMessage());
            }
        });

        btnEnable.addActionListener(actionEvent -> {
            disableWidgets(true, true);
            onAdminUserBtnClick();
        });
        btnDisable.addActionListener(actionEvent -> {
            disableWidgets(false, false);
            onAdminUserBtnClick();
        });

        final HideableDecorator propertyDecorator = new HideableDecorator(pnlPropertyHolder,
            PROPERTY, false /*adjustWindow*/);
        propertyDecorator.setContentComponent(pnlProperty);
        propertyDecorator.setOn(true);

        menu = new JPopupMenu();
        final JMenuItem menuItem = new JMenuItem(PULL_IMAGE);
        menuItem.addActionListener(new AzureActionListenerWrapper(INSIGHT_NAME, "menuItem", null) {
            @Override
            protected void actionPerformedFunc(ActionEvent e) {
                pullImage();
            }
        });
        menu.add(menuItem);
        disableWidgets(true, true);
        final ContainerRegistry registry = (ContainerRegistry) virtualFile.getUserData(AzureResourceEditorViewManager.AZURE_RESOURCE_KEY);
        this.onReadProperty(registry.getSubscriptionId(), registry.getId());
    }

    @Override
    public void onErrorWithException(String message, Exception ex) {
        ContainerRegistryPropertyMvpView.super.onErrorWithException(message, ex);
        this.enableWidgets();
    }

    private void createUIComponents() {
        final DefaultTableModel repoModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        repoModel.addColumn(REPOSITORY);

        tblRepo = new JBTable(repoModel);
        tblRepo.getEmptyText().setText(TABLE_LOADING_MESSAGE);
        tblRepo.setRowSelectionAllowed(true);
        tblRepo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblRepo.setStriped(true);
        tblRepo.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            final int selectedRow = tblRepo.getSelectedRow();
            if (selectedRow < 0 || selectedRow >= tblRepo.getRowCount()) {
                return;
            }
            final String selectedRepo = (String) tblRepo.getModel().getValueAt(selectedRow, 0);
            if (StringUtils.isEmpty(selectedRepo) || Objects.equals(selectedRepo, currentRepo)) {
                return;
            }
            currentRepo = selectedRepo;
            disableWidgets(false, true);
            tblTag.getEmptyText().setText(TABLE_LOADING_MESSAGE);
            containerPropertyPresenter.onListTags(subscriptionId, registryId, currentRepo, true /*isNextPage*/);
        });

        btnRepoRefresh = new AnActionButton(REFRESH, AllIcons.Actions.Refresh) {
            @Override
            public @Nonnull ActionUpdateThread getActionUpdateThread() {
                return super.getActionUpdateThread();
            }

            @Override
            public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
                disableWidgets(true, true);
                tblTag.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
                containerPropertyPresenter.onRefreshRepositories(subscriptionId, registryId, true /*isNextPage*/);
            }
        };

        btnRepoPrevious = new AnActionButton(PREVIOUS_PAGE, AllIcons.Actions.MoveUp) {
            @Override
            public @Nonnull ActionUpdateThread getActionUpdateThread() {
                return super.getActionUpdateThread();
            }

            @Override
            public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
                disableWidgets(true, true);
                tblTag.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
                containerPropertyPresenter.onListRepositories(subscriptionId, registryId, false /*isNextPage*/);
            }
        };

        btnRepoNext = new AnActionButton(NEXT_PAGE, AllIcons.Actions.MoveDown) {
            @Override
            public @Nonnull ActionUpdateThread getActionUpdateThread() {
                return super.getActionUpdateThread();
            }

            @Override
            public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
                disableWidgets(true, true);
                tblTag.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
                containerPropertyPresenter.onListRepositories(subscriptionId, registryId, true /*isNextPage*/);
            }
        };

        final ToolbarDecorator repoDecorator = ToolbarDecorator.createDecorator(tblRepo)
            .addExtraActions(btnRepoRefresh, btnRepoPrevious, btnRepoNext)
            .setToolbarPosition(ActionToolbarPosition.BOTTOM)
            .setToolbarBorder(JBUI.Borders.empty());

        pnlRepoTable = repoDecorator.createPanel();

        final DefaultTableModel tagModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tagModel.addColumn(TAG);

        tblTag = new JBTable(tagModel);
        tblTag.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
        tblTag.setRowSelectionAllowed(true);
        tblTag.setStriped(true);
        tblTag.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblTag.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    final int rowIndex = tblTag.getSelectedRow();
                    if (rowIndex < 0 || rowIndex >= tblTag.getRowCount()) {
                        return;
                    }
                    currentTag = (String) tblTag.getModel().getValueAt(rowIndex, 0);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        btnTagRefresh = new AnActionButton(REFRESH, AllIcons.Actions.Refresh) {
            @Override
            public @Nonnull ActionUpdateThread getActionUpdateThread() {
                return super.getActionUpdateThread();
            }

            @Override
            public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
                if (StringUtils.isEmpty(currentRepo)) {
                    return;
                }
                disableWidgets(false, true);
                containerPropertyPresenter.onListTags(subscriptionId, registryId, currentRepo, true /*isNextPage*/);
            }
        };

        btnTagPrevious = new AnActionButton(PREVIOUS_PAGE, AllIcons.Actions.MoveUp) {
            @Override
            public @Nonnull ActionUpdateThread getActionUpdateThread() {
                return super.getActionUpdateThread();
            }

            @Override
            public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
                if (StringUtils.isEmpty(currentRepo)) {
                    return;
                }
                disableWidgets(false, true);
                containerPropertyPresenter.onListTags(subscriptionId, registryId, currentRepo, false /*isNextPage*/);
            }
        };

        btnTagNext = new AnActionButton(NEXT_PAGE, AllIcons.Actions.MoveDown) {
            @Override
            public @Nonnull ActionUpdateThread getActionUpdateThread() {
                return super.getActionUpdateThread();
            }

            @Override
            public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
                if (StringUtils.isEmpty(currentRepo)) {
                    return;
                }
                disableWidgets(false, true);
                containerPropertyPresenter.onListTags(subscriptionId, registryId, currentRepo, true /*isNextPage*/);
            }
        };

        final ToolbarDecorator tagDecorator = ToolbarDecorator.createDecorator(tblTag)
            .addExtraActions(btnTagRefresh, btnTagPrevious, btnTagNext)
            .setToolbarPosition(ActionToolbarPosition.BOTTOM)
            .setToolbarBorder(JBUI.Borders.empty());

        pnlTagTable = tagDecorator.createPanel();
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return pnlMain;
    }

    @Nonnull
    @Override
    public String getName() {
        return ID;
    }

    @Override
    public void dispose() {
        containerPropertyPresenter.onDetachView();
    }

    @Override
    public void onReadProperty(String sid, String id) {
        containerPropertyPresenter.onGetRegistryProperty(sid, id);
    }

    @Override
    public void showProperty(ContainerRegistryProperty property) {
        registryId = property.getId();
        subscriptionId = property.getSubscriptionId();
        isAdminEnabled = property.isAdminEnabled();

        txtName.setText(property.getName());
        txtType.setText(property.getType());
        txtResGrp.setText(property.getGroupName());
        txtSubscription.setText(subscriptionId);
        txtRegion.setText(property.getRegionName());
        txtServerUrl.setText(property.getLoginServerUrl());

        lblUserName.setVisible(isAdminEnabled);
        txtUserName.setVisible(isAdminEnabled);
        lblPrimaryPwd.setVisible(isAdminEnabled);
        btnPrimaryPassword.setVisible(isAdminEnabled);
        lblSecondaryPwd.setVisible(isAdminEnabled);
        btnSecondaryPassword.setVisible(isAdminEnabled);
        disableWidgets(true, true);
        if (isAdminEnabled) {
            txtUserName.setText(property.getUserName());
            password = property.getPassword();
            password2 = property.getPassword2();
            tblRepo.getEmptyText().setText(TABLE_LOADING_MESSAGE);
            tblTag.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
            containerPropertyPresenter.onRefreshRepositories(subscriptionId, registryId, true /*isNextPage*/);
        } else {
            tblRepo.getEmptyText().setText(ADMIN_NOT_ENABLED);
            tblTag.getEmptyText().setText(ADMIN_NOT_ENABLED);
        }
        updateAdminUserBtn(isAdminEnabled);
        pnlProperty.revalidate();
    }

    @Override
    public void listRepo(List<String> repos) {
        fillTable(repos, tblRepo);
        enableWidgets();
    }

    @Override
    public void listTag(List<String> tags) {
        fillTable(tags, tblTag);
        enableWidgets();
    }

    private void onAdminUserBtnClick() {
        this.containerPropertyPresenter.onEnableAdminUser(subscriptionId, registryId, !isAdminEnabled);
    }

    private void updateAdminUserBtn(boolean isAdminEnabled) {
        btnEnable.setEnabled(!isAdminEnabled);
        btnDisable.setEnabled(isAdminEnabled);
    }

    private void disableTxtBoard() {
        txtName.setBorder(BorderFactory.createEmptyBorder());
        txtType.setBorder(BorderFactory.createEmptyBorder());
        txtResGrp.setBorder(BorderFactory.createEmptyBorder());
        txtSubscription.setBorder(BorderFactory.createEmptyBorder());
        txtRegion.setBorder(BorderFactory.createEmptyBorder());
        txtServerUrl.setBorder(BorderFactory.createEmptyBorder());
        txtUserName.setBorder(BorderFactory.createEmptyBorder());
    }

    private void makeTxtOpaque() {
        txtName.setBackground(null);
        txtType.setBackground(null);
        txtResGrp.setBackground(null);
        txtSubscription.setBackground(null);
        txtRegion.setBackground(null);
        txtServerUrl.setBackground(null);
        txtUserName.setBackground(null);
    }

    private void fillTable(List<String> list, @Nonnull JBTable table) {
        if (list != null && !list.isEmpty()) {
            final DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.getDataVector().clear();
            list.stream().sorted().forEach(item -> model.addRow(new String[]{item}));
        } else {
            table.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
        }
    }

    private void disableWidgets(boolean needResetRepo, boolean needResetTag) {
        btnEnable.setEnabled(false);
        btnDisable.setEnabled(false);
        tblRepo.setEnabled(false);
        btnRepoRefresh.setEnabled(false);
        btnRepoNext.setEnabled(false);
        btnRepoPrevious.setEnabled(false);
        tblTag.setEnabled(false);
        btnTagRefresh.setEnabled(false);
        btnTagNext.setEnabled(false);
        btnTagPrevious.setEnabled(false);
        if (needResetRepo) {
            currentRepo = null;
            cleanTableData((DefaultTableModel) tblRepo.getModel());
        }
        if (needResetTag) {
            currentTag = null;
            cleanTableData((DefaultTableModel) tblTag.getModel());
        }
    }

    private void enableWidgets() {
        updateAdminUserBtn(isAdminEnabled);
        tblRepo.setEnabled(true);
        btnRepoRefresh.setEnabled(true);
        if (containerPropertyPresenter.hasNextRepoPage()) {
            btnRepoNext.setEnabled(true);
        }
        if (containerPropertyPresenter.hasPreviousRepoPage()) {
            btnRepoPrevious.setEnabled(true);
        }
        tblTag.setEnabled(true);
        if (currentRepo == null) {
            return;
        }
        btnTagRefresh.setEnabled(true);
        if (containerPropertyPresenter.hasNextTagPage()) {
            btnTagNext.setEnabled(true);
        }
        if (containerPropertyPresenter.hasPreviousTagPage()) {
            btnTagPrevious.setEnabled(true);
        }
    }

    private void pullImage() {
        final AzureString title = OperationBundle.description("boundary/docker.pull_image.image", currentRepo);
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(null, title, false, () -> {
            try {
                if (StringUtils.isEmpty(currentRepo) || StringUtils.isEmpty(currentTag)) {
                    throw new Exception(REPO_TAG_NOT_AVAILABLE);
                }
                final ContainerRegistry registry = ContainerRegistryMvpModel.getInstance()
                    .getContainerRegistry(subscriptionId, registryId);
                final PrivateRegistryImageSetting setting = ContainerRegistryMvpModel.getInstance()
                    .createImageSettingWithRegistry(registry);
                final String image = String.format("%s:%s", currentRepo, currentTag);
                final String fullImageTagName = String.format("%s/%s", registry.getLoginServerUrl(), image);
                AzureDockerClient.getDefault().pullImage(Objects.requireNonNull(registry.getLoginServerUrl()), setting.getUsername(),
                    setting.getPassword(), currentRepo, currentTag);
                final String message = String.format(IMAGE_PULL_SUCCESS, fullImageTagName);
                UIUtils.showNotification(statusBar, message, MessageType.INFO);
                sendTelemetry(true, subscriptionId, null);
            } catch (final Exception e) {
                UIUtils.showNotification(statusBar, e.getMessage(), MessageType.ERROR);
                sendTelemetry(false, subscriptionId, e.getMessage());
            }
        }));
    }

    private void cleanTableData(DefaultTableModel model) {
        model.getDataVector().clear();
        model.fireTableDataChanged();
    }

    private void sendTelemetry(boolean success, @Nonnull String subscriptionId, @Nullable String errorMsg) {
        final Map<String, String> map = new HashMap<>();
        map.put("SubscriptionId", subscriptionId);
        map.put("Success", String.valueOf(success));
        map.put(AzureTelemeter.SERVICE_NAME, "ACR");
        map.put(AzureTelemeter.OPERATION_NAME, PULL_IMAGE);
        if (!success) {
            map.put("ErrorMsg", errorMsg);
        }
        AzureTelemeter.log(AzureTelemetry.Type.INFO, map);
    }

    private static void copyToSystemClipboard(String key) throws Exception {
        final StringSelection stringSelection = new StringSelection(key);
        final Toolkit toolKit = Toolkit.getDefaultToolkit();
        if (toolKit == null) {
            throw new Exception("Cannot copy to system clipboard.");
        }
        final Clipboard clipboard = toolKit.getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
}
