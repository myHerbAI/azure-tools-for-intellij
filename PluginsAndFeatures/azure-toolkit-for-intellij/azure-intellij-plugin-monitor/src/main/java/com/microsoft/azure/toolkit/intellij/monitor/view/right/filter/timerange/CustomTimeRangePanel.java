package com.microsoft.azure.toolkit.intellij.monitor.view.right.filter.timerange;

import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.versionBrowser.StandardVersionFilterComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomTimeRangePanel {
    private JPanel rootPanel;
    private JCheckBox myUseDateAfterFilter;
    private JCheckBox myUseDateBeforeFilter;
    private TimePicker myDateAfter;
    private TimePicker myDateBefore;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public CustomTimeRangePanel() {
        $$$setupUI$$$();
        myDateAfter.setDateFormat(DATE_FORMAT);
        myDateBefore.setDateFormat(DATE_FORMAT);
        final ActionListener listener = this::updateAllEnabled;
        myUseDateAfterFilter.addActionListener(listener);
        myUseDateBeforeFilter.addActionListener(listener);
        updateAllEnabled(null);
    }

    @NotNull
    public JPanel getPanel() {
        return rootPanel;
    }

    public void setBefore(long beforeTs) {
        myUseDateBeforeFilter.setSelected(true);
        try {
            myDateBefore.updateDate(new Date(beforeTs));
            myDateBefore.setEnabled(true);
        }
        catch (final PropertyVetoException ignored) {
        }
    }

    public void setAfter(long afterTs) {
        myUseDateAfterFilter.setSelected(true);
        try {
            myDateAfter.updateDate(new Date(afterTs));
            myDateAfter.setEnabled(true);
        }
        catch (final PropertyVetoException ignored) {
        }
    }

    public long getBefore() {
        return myUseDateBeforeFilter.isSelected() ? myDateBefore.getTimeDate().getTime() : -1;
    }

    public long getAfter() {
        return myUseDateAfterFilter.isSelected() ? myDateAfter.getTimeDate().getTime() : -1;
    }

    @Nls
    @Nullable
    public String validateInput() {
        if (myUseDateAfterFilter.isSelected() && myDateAfter.getTimeDate() == null) {
            return VcsBundle.message("error.date.after.must.be.a.valid.date");
        }
        if (myUseDateBeforeFilter.isSelected() && myDateBefore.getTimeDate() == null) {
            return VcsBundle.message("error.date.before.must.be.a.valid.date");
        }
        return null;
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    private void updateAllEnabled(@Nullable ActionEvent e) {
        StandardVersionFilterComponent.updatePair(myUseDateBeforeFilter, myDateBefore, e);
        StandardVersionFilterComponent.updatePair(myUseDateAfterFilter, myDateAfter, e);
    }

    private void createUIComponents() {
        myDateBefore = new TimePicker();
        myDateAfter = new TimePicker();
    }
}
