package view;

import javax.swing.*;
import java.awt.*;

public class AnalysisPanel extends JPanel {
    private JTextArea resultArea;
    public AnalysisPanel() {
        setLayout(new BorderLayout());
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);
    }
    public void setResult(String result) {
        resultArea.setText(result);
    }
}
