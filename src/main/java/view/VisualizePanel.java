package view;

import javax.swing.*;
import java.awt.*;
import model.Project;

/**
 * VisualizePanel now delegates drawing to GanttChartPanel which renders a
 * time-based Gantt chart. The panel recreates the chart when Refresh is
 * clicked so it picks up the latest tasks from the Project.
 */
public class VisualizePanel extends JPanel {
    private Project project;
    private JScrollPane scrollPane;
    private GanttChartPanel ganttPanel;

    public VisualizePanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());

        JButton refresh = new JButton("Refresh Chart");
        add(refresh, BorderLayout.NORTH);

        // initial chart
        ganttPanel = new GanttChartPanel(project == null ? null : project.getTasks());
        scrollPane = new JScrollPane(ganttPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        refresh.addActionListener(e -> {
            // recreate the gantt panel so it reads current tasks
            ganttPanel = new GanttChartPanel(project == null ? null : project.getTasks());
            scrollPane.setViewportView(ganttPanel);
            revalidate();
            repaint();
        });
    }
}
