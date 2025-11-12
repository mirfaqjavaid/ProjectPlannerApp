package view;

import javax.swing.*;
import java.awt.*;
import controller.ProjectController;
import model.Project;
import model.storage.DatabaseInitializer;
import model.storage.FileProjectStorage;

public class MainUI extends JFrame {
    public static Project globalProject = new Project("Untitled Project");
    private ProjectController controller = new ProjectController(new FileProjectStorage());

    private JPanel mainPanel;

    public MainUI() {
        // Initialize database
        DatabaseInitializer.initializeDatabase();
        
        setTitle("Project Planner");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Navigation bar
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton dashboardBtn = new JButton("Dashboard");
        JButton formBtn = new JButton("Project Form");
        JButton analyzeBtn = new JButton("Analyze");
        JButton visualizeBtn = new JButton("Visualize");
        navBar.add(dashboardBtn);
        navBar.add(formBtn);
        navBar.add(analyzeBtn);
        navBar.add(visualizeBtn);
        add(navBar, BorderLayout.NORTH);

        // Main panel to swap views
        mainPanel = new JPanel(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        // Panels
        DashboardPanel dashboardPanel = new DashboardPanel();
        ProjectFormPanel formPanel = new ProjectFormPanel(globalProject);
        AnalyzePanel analyzePanel = new AnalyzePanel(globalProject);
        VisualizePanel visualizePanel = new VisualizePanel(globalProject);

        // Show dashboard by default
        showPanel(dashboardPanel);

        // Button actions
        dashboardBtn.addActionListener(e -> showPanel(dashboardPanel));
        formBtn.addActionListener(e -> showPanel(formPanel));
        analyzeBtn.addActionListener(e -> showPanel(analyzePanel));
        visualizeBtn.addActionListener(e -> showPanel(visualizePanel));
    }

    private void showPanel(JPanel panel) {
        mainPanel.removeAll();
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainUI().setVisible(true);
        });
    }
}