package com.microsoft.azure.toolkit.ide.guidance.view;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBFont;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.guidance.Course;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceViewManager;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.phase.PhaseManager;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class CourseView {
    private JPanel contentPanel;
    private JLabel guidanceIcon;
    private JLabel titleLabel;
    private JPanel phasesPanel;
    private JLabel closeButton;
    private JPanel bodyPanel;
    private JPanel headPanel;

    private final Project project;
    @Getter
    private Course course;

    public CourseView(@Nonnull final Project project) {
        this.project = project;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        this.titleLabel.setFont(JBFont.h2().asBold());
        this.closeButton.setIcon(AllIcons.Actions.Exit);
        this.closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (AzureMessager.getMessager().confirm("Some steps might be lost, are you sure to abort current process?")) {
                    GuidanceViewManager.getInstance().showCoursesView(project);
                }
            }
        });
    }

    public void setCourse(@Nonnull Course course) {
        this.course = course;
        this.guidanceIcon.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
        this.titleLabel.setText(course.getTitle());
        fillPhase(course);
    }

    private void fillPhase(@Nonnull Course course) {
        this.phasesPanel.removeAll();
        final List<Phase> phases = course.getPhases();
        final GridLayoutManager layout = ((GridLayoutManager) this.phasesPanel.getLayout());
        final GridLayoutManager newLayout = new GridLayoutManager(phases.size() + 1, 1, layout.getMargin(), -1, -1);
        this.phasesPanel.setLayout(newLayout);
        for (int i = 0; i < phases.size(); i++) {
            final Phase phase = phases.get(i);
            final JPanel phasePanel = PhaseManager.createPhasePanel(phase);
            final GridConstraints c = new GridConstraints(i, 0, 1, 1, 1, 1, 3, 3, null, null, null, 0, false);
            this.phasesPanel.add(phasePanel, c);
        }
        final GridConstraints c = new GridConstraints(phases.size(), 0, 1, 1, 1, 2, 1, 6, null, null, null, 0, false);
        final Spacer spacer = new Spacer();
        this.phasesPanel.add(spacer, c);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    public void setVisible(boolean visible) {
        this.contentPanel.setVisible(visible);
    }

    private void createUIComponents() {
    }
}
