package view;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    public DashboardPanel() {
        setLayout(new BorderLayout());
        JLabel title = new JLabel("Project Planner Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        JTextArea info = new JTextArea(
            "Welcome to Project Planner!\n\n" +
            "→ Use 'Project Form' to load or create tasks/resources.\n" +
            "→ Use 'Analyze' to find overlaps or project duration.\n" +
            "→ Use 'Visualize' to view Gantt chart.\n"
        );
        info.setEditable(false);
        info.setFont(new Font("Consolas", Font.PLAIN, 15));
        add(info, BorderLayout.CENTER);
    }
}
