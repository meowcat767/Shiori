import api.MangaDexClient;
import model.Chapter;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class ChapterListPanel extends JPanel {

    private final DefaultListModel<Chapter> model = new DefaultListModel<>();
    private final JList<Chapter> list = new JList<>(model);
    private final MangaDexClient api = new MangaDexClient();
    private final Consumer<Chapter> onSelect;

    public ChapterListPanel(Consumer<Chapter> onSelect) {
        this.onSelect = onSelect;

        setLayout(new BorderLayout());
        add(new JScrollPane(list), BorderLayout.CENTER);

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                if (onSelect != null) {
                    onSelect.accept(list.getSelectedValue());
                }
            }
        });
    }

    public void loadChapters(String mangaId) {
        model.clear();

        new SwingWorker<List<Chapter>, Void>() {
            @Override
            protected List<Chapter> doInBackground() throws Exception {
                return api.getChapters(mangaId);
            }

            @Override
            protected void done() {
                try {
                    List<Chapter> chapters = get();
                    for (Chapter c : chapters) model.addElement(c);

                    if (!chapters.isEmpty()) {
                        list.setSelectedIndex(0);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    public void nextChapter() {
        int index = list.getSelectedIndex();
        if (index != -1 && index < model.getSize() - 1) {
            list.setSelectedIndex(index + 1);
        }
    }

    public void previousChapter() {
        int index = list.getSelectedIndex();
        if (index > 0) {
            list.setSelectedIndex(index - 1);
        }
    }
}
