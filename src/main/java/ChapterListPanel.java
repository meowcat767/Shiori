import javax.swing.*;
import java.awt.*;

public class ChapterListPanel extends JPanel {

    public ChapterListPanel() {
        setLayout(new BorderLayout());

        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Chapter 1");
        model.addElement("Chapter 2");

        JList<String> list = new JList<>(model);
        add(new JScrollPane(list), BorderLayout.CENTER);
    }
}