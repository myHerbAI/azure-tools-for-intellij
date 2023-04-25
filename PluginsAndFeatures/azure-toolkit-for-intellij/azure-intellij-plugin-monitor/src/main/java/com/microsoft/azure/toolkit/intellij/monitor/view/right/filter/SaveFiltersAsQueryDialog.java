package com.microsoft.azure.toolkit.intellij.monitor.view.right.filter;

import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.EditorTextField;
import com.microsoft.azure.toolkit.intellij.monitor.view.left.MonitorTreePanel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SaveFiltersAsQueryDialog extends DialogWrapper {
    private JPanel rootPanel;
    private JTextField queryNameField;
    private JTextField queryDescriptionField;
    private EditorTextField queryContent;
    @Getter
    private final MonitorTreePanel.QueryData queryDataToSave = new MonitorTreePanel.QueryData();

    public SaveFiltersAsQueryDialog(@Nullable Project project, String queryContent) {
        super(project, false);
        setTitle("Save as Query");
        $$$setupUI$$$();
        init();
        this.queryContent.setText(queryContent);
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
        this.queryDataToSave.setDisplayName(queryNameField.getText());
        this.queryDataToSave.setQueryString(queryContent.getText());
        super.doOKAction();
    }

    private EditorTextField createEditorTextField() {
        final Project project = ProjectManager.getInstance().getOpenProjects()[0];
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
