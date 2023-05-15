package com.microsoft.azure.toolkit.intellij.monitor.view.right.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.EditorTextField;
import com.microsoft.azure.toolkit.intellij.monitor.AzureMonitorManager;
import com.microsoft.azure.toolkit.intellij.monitor.view.left.MonitorTreePanel;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaveFiltersAsQueryDialog extends DialogWrapper {
    private JPanel rootPanel;
    private JTextField queryNameField;
    private JTextField queryDescriptionField;
    private EditorTextField queryContent;
    @Getter
    private final MonitorTreePanel.QueryData queryDataToSave = new MonitorTreePanel.QueryData();
    private final List<MonitorTreePanel.QueryData> originData;
    private final Project project;

    public SaveFiltersAsQueryDialog(@Nullable Project project, String queryContent, List<MonitorTreePanel.QueryData> originData) {
        super(project, false);
        setTitle("Save as Query");
        $$$setupUI$$$();
        init();
        this.queryContent.setText(queryContent);
        this.originData = originData;
        this.project = project;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return rootPanel;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (StringUtils.isBlank(queryNameField.getText())) {
            return new ValidationInfo("Name is required.", queryNameField);
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        final String queryName = queryNameField.getText();
        if (isExistingQuery(queryName) && !isOverride(queryName)) {
            return;
        }
        this.queryDataToSave.setDisplayName(queryNameField.getText());
        this.queryDataToSave.setQueryString(queryContent.getText());
        super.doOKAction();
    }

    private boolean isExistingQuery(String queryName) {
        return this.originData.stream().anyMatch(q -> queryName.equals(q.getDisplayName()));
    }

    private boolean isOverride(String queryName) {
        return AzureMessager.getMessager().confirm(AzureString.format("A query with the same name {0} already exists, do you want to override?", queryName));
    }

    private EditorTextField createEditorTextField() {
        final DocumentImpl document = new DocumentImpl("", true);
        final EditorTextField result = new EditorTextField(document, project, PlainTextFileType.INSTANCE, true, false);
        result.addSettingsProvider(editor -> { // add scrolling/line number features/soft wrap
            editor.setHorizontalScrollbarVisible(true);
            editor.setVerticalScrollbarVisible(true);
            editor.getSettings().setLineNumbersShown(true);
            editor.getSettings().setUseSoftWraps(true);
        });
        return result;
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        this.queryContent = createEditorTextField();
    }

}
