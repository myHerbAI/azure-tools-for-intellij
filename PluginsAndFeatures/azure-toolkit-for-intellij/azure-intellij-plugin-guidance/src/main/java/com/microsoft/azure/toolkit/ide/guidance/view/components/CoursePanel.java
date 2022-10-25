package com.microsoft.azure.toolkit.ide.guidance.view.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceViewManager;
import com.microsoft.azure.toolkit.ide.guidance.config.CourseConfig;
import com.microsoft.azure.toolkit.ide.guidance.view.ViewUtils;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseListener;
import java.util.Objects;
import java.util.Optional;

public class CoursePanel {
    private final CourseConfig course;
    @Getter
    private JPanel rootPanel;
    private JLabel lblTitle;
    private JTextPane areaDescription;
    private JButton startButton;
    private JPanel tagsPanel;

    private final Project project;

    public CoursePanel(@Nonnull final CourseConfig course, @Nonnull final Project project) {
        super();
        this.course = course;
        this.project = project;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        this.lblTitle.setFont(JBFont.h4());
        // render course
        // this.lblIcon.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
        this.lblTitle.setText(course.getTitle());
        this.lblTitle.setPreferredSize(new Dimension(-1, startButton.getPreferredSize().height));
        this.startButton.setVisible(false);
        this.startButton.addActionListener(e -> openGuidance());
        this.areaDescription.setFont(JBFont.medium());
        this.areaDescription.setText(course.getDescription());
        course.getTags().forEach(tag -> this.tagsPanel.add(decorateTagLabel(tag)));
    }

    public void toggleSelectedStatus(final boolean isSelected) {
        if (Objects.equals(isSelected, startButton.isVisible())) {
            return;
        }
        this.startButton.setVisible(isSelected);
        ViewUtils.setBackgroundColor(this.rootPanel, isSelected ? ViewUtils.NOTIFICATION_BACKGROUND_COLOR : UIUtil.getLabelBackground());
        if (isSelected) {
            Optional.ofNullable(this.getRootPanel().getRootPane()).ifPresent(pane -> pane.setDefaultButton(this.startButton));
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    public void addMouseListener(@Nonnull final MouseListener coursePanelListener) {
        this.rootPanel.addMouseListener(coursePanelListener);
    }

    @AzureOperation(name = "guidance.open_course.course", params = {"this.course.getTitle()"}, type = AzureOperation.Type.ACTION)
    public void openGuidance() {
        GuidanceViewManager.getInstance().openCourseView(project, course);
    }

    private JLabel decorateTagLabel(String tag) {
        final JLabel label = new JLabel(tag);
        final Border borderLine = new RoundedLineBorder(new JBColor(12895428, 6185056), 2);
        final Border margin = JBUI.Borders.empty(0, 6);
        label.setBorder(new CompoundBorder(borderLine, margin));
        label.setOpaque(true);
        label.setBackground(new JBColor(16777215, 5001298));
        label.setFont(JBFont.regular().lessOn(2));
        return label;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.rootPanel = new RoundedPanel(5);
        this.tagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        this.tagsPanel.setBorder(JBUI.Borders.emptyLeft(-8));
    }
}
