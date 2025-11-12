import view.MainUI;

import javax.swing.SwingUtilities;

public class ProjectPlannerApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainUI().setVisible(true);
        });
    }
}

