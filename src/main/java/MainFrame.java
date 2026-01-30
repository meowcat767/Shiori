import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    public MainFrame() {
        super("Shiori");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setLocationRelativeTo(null);
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                createSidebar(),
                new ReaderPanel()
        );
        splitPane.setDividerLocation(150);
        splitPane.setResizeWeight(0);
        add(splitPane);
        UIManager.put("ScrollBar.thumb", Color.DARK_GRAY);
        UIManager.put("ScrollBar.track", Color.BLACK);
    }
    private JPanel createSidebar() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Libary");
        panel.add(title, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Manga", new MangaListPanel());
        tabs.add("Chapters", new ChapterListPanel());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}