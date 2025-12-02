package ch02_pit_01;

import javax.swing.*;
import java.awt.*;

final class PlannerViewContext {
    private final ChatbotPlannerClientGUI owner;
    private final JLabel statusLabel;
    private final CardLayout resultCardLayout;
    private final JPanel resultCardPanel;
    private final JTextArea resultArea;
    private JFrame resultFrame;

    PlannerViewContext(
        ChatbotPlannerClientGUI owner,
        JLabel statusLabel,
        CardLayout resultCardLayout,
        JPanel resultCardPanel,
        JTextArea resultArea
    ) {
        this.owner = owner;
        this.statusLabel = statusLabel;
        this.resultCardLayout = resultCardLayout;
        this.resultCardPanel = resultCardPanel;
        this.resultArea = resultArea;
    }

    JLabel getStatusLabel() {
        return statusLabel;
    }

    JTextArea getResultArea() {
        return resultArea;
    }

    void setResultFrame(JFrame resultFrame) {
        this.resultFrame = resultFrame;
    }

    void ensureResultFrameVisible() {
        if (resultFrame == null) {
            return;
        }
        if (!resultFrame.isVisible()) {
            resultFrame.setVisible(true);
        }
        resultFrame.toFront();
    }

    void showPlansCard() {
        resultCardLayout.show(resultCardPanel, "plans");
    }

    void showJsonCard() {
        resultCardLayout.show(resultCardPanel, "json");
    }

    ChatbotPlannerClientGUI getOwner() {
        return owner;
    }
}

