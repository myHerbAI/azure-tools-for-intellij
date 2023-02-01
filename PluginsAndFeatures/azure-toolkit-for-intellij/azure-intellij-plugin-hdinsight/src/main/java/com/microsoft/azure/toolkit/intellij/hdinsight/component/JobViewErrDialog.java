package com.microsoft.azure.toolkit.intellij.hdinsight.component;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class JobViewErrDialog extends DialogWrapper {

    private JPanel contentPane;

    public JobViewErrDialog(@Nullable Project project, boolean canBeParent) {
        super(project, canBeParent);
        setTitle("Cluster info error");
        init();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[] { getOKAction()};
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
