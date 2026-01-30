import javax.swing.*;
import java.awt.*;

public class ReaderPanel extends JPanel {

    private final JPanel pagesPanel;

    public ReaderPanel() {
        setLayout(new BorderLayout());

        pagesPanel = new JPanel();
        pagesPanel.setLayout(new BoxLayout(pagesPanel, BoxLayout.Y_AXIS));
        pagesPanel.setBackground(Color.BLACK);

        JScrollPane scrollPane = new JScrollPane(pagesPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    // Temporary helper for testing
    public void addDummyPage() {
        JLabel label = new JLabel("Page");
        label.setForeground(Color.WHITE);
        label.setPreferredSize(new Dimension(600, 800));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        pagesPanel.add(label);
        pagesPanel.revalidate();
    }

    public void clearPages() {
        pagesPanel.removeAll();
        pagesPanel.revalidate();
        pagesPanel.repaint();
    }
}