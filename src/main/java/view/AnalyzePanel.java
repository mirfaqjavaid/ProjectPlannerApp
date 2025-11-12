package view;

import javax.swing.*;
import java.awt.*;
import controller.AnalysisController;
import model.*;
import java.util.*;

public class AnalyzePanel extends JPanel {
    private Project project;
    private JTextArea output;

    public AnalyzePanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new GridLayout(1, 4, 10, 10));
        JButton comp = new JButton("Completion");
        JButton overlap = new JButton("Overlaps");
        JButton team = new JButton("Teams");
        JButton effort = new JButton("Effort");

        top.add(comp); top.add(overlap); top.add(team); top.add(effort);
        add(top, BorderLayout.NORTH);

        output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font("Consolas", Font.PLAIN, 14));
        add(new JScrollPane(output), BorderLayout.CENTER);

        comp.addActionListener(e -> showCompletion());
        overlap.addActionListener(e -> showOverlaps());
        team.addActionListener(e -> showTeams());
        effort.addActionListener(e -> showEffort());
    }

    private void showCompletion() {
        output.setText("Project Completion: " +
            AnalysisController.getProjectCompletionDate(project.getTasks()));
    }

    private void showOverlaps() {
        output.setText("Overlapping Tasks:\n");
        for (String s : AnalysisController.getOverlappingTasks(project.getTasks()))
            output.append(" - " + s + "\n");
    }

    private  void showTeams() {
        output.setText("Teams by Task:\n");
        for (Task t : project.getTasks()) {
          java.util.List<String> team = AnalysisController.getTeamForTask(t.getId(), project.getResources());

            output.append(t.getTitle() + " → " + team + "\n");
        }
    }

    private void showEffort() {
        output.setText("Effort Breakdown (hours):\n");
        Map<String, Long> map = AnalysisController.getEffortHoursPerResource(project.getResources(), project.getTasks());
        for (String r : map.keySet()) output.append(r + " → " + map.get(r) + " hrs\n");
    }
}
