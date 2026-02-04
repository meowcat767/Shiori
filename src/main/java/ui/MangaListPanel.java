package ui;

import api.MangaDexClient;
import model.Manga;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class MangaListPanel extends JPanel {

    private final DefaultListModel<Manga> model = new DefaultListModel<>();
    private final JList<Manga> list = new JList<>(model);
    private final MangaDexClient api = new MangaDexClient();
    private final Consumer<Manga> onSelect;

    private final JLabel loadingLabel = new JLabel("Searching...", SwingConstants.CENTER);
    private final JTextField searchField = new JTextField("Enter manga name and press ENTER");

    public MangaListPanel(Consumer<Manga> onSelect) {
        this.onSelect = onSelect;

        setLayout(new BorderLayout());

        // Search box
        add(searchField, BorderLayout.NORTH);

        // Manga list scroll pane
        add(new JScrollPane(list), BorderLayout.CENTER);

        // List selection triggers the consumer
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                this.onSelect.accept(list.getSelectedValue());
            }
        });

        // Search action
        searchField.addActionListener(e -> {
            String query = searchField.getText().trim();
            if (query.isEmpty()) return;

            // Show loading label while searching
            showLoading();

            // Clear previous results
            model.clear();

            new SwingWorker<List<Manga>, Void>() {
                @Override
                protected List<Manga> doInBackground() throws Exception {
                    return api.searchManga(query);
                }

                @Override
                protected void done() {
                    hideLoading();
                    try {
                        List<Manga> results = get();
                        for (Manga m : results) model.addElement(m);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                                MangaListPanel.this,
                                "Failed to search manga: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }.execute();
        });
    }

    // Optional no-arg constructor for testing
    public MangaListPanel() {
        this(manga -> {});
    }

    // --- Loading label helpers ---
    private void showLoading() {
        // Remove current center component (the list scroll pane)
        Component center = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (center != null) remove(center);

        loadingLabel.setFont(loadingLabel.getFont().deriveFont(18f));
        add(loadingLabel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void hideLoading() {
        Component center = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (center != null) remove(center);

        add(new JScrollPane(list), BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}