package ui;

import model.RecentManga;
import recent.RecentMangasStore;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class RecentMangasPanel extends JPanel {

    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final RecentMangasStore store;
    private final Consumer<String> onSelect; // mangaId consumer
    private final showOptions options;

    private final JPanel contentPanel;
    private boolean showingEmpty = false;

    public RecentMangasPanel(RecentMangasStore store, Consumer<String> onSelect, showOptions options) {
        this.store = store;
        this.onSelect = onSelect;
        this.options = options;

        setLayout(new BorderLayout());

        // Toolbar with refresh and clear buttons
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear");

        refreshButton.addActionListener(e -> refreshList());
        clearButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Clear all recent manga?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                store.clear();
                refreshList();
            }
        });

        toolbar.add(refreshButton);
        toolbar.add(clearButton);
        add(toolbar, BorderLayout.NORTH);

        // Content panel using CardLayout for switching between list and empty state
        contentPanel = new JPanel(new CardLayout());
        JLabel emptyLabel = new JLabel("No recent manga yet", SwingConstants.CENTER);
        emptyLabel.setFont(emptyLabel.getFont().deriveFont(18f));
        contentPanel.add(emptyLabel, "empty");
        contentPanel.add(new JScrollPane(list), "list");
        add(contentPanel, BorderLayout.CENTER);

        // List selection triggers the consumer
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                String selected = list.getSelectedValue();
                // Extract manga ID from the list item (format: "Title (id)")
                String mangaId = extractMangaId(selected);
                if (mangaId != null) {
                    this.onSelect.accept(mangaId);
                }
            }
        });

        // Set initial enabled state based on caching
        updateEnabledState();

        // Initial load
        refreshList();
    }

    /**
     * Update the enabled state based on caching option
     */
    private void updateEnabledState() {
        boolean cachingEnabled = options != null && options.isCachingEnabled();
        setEnabled(cachingEnabled);
        
        // Update toolbar buttons
        for (Component c : getComponents()) {
            if (c instanceof JPanel toolbar) {
                for (Component button : toolbar.getComponents()) {
                    if (button instanceof JButton) {
                        button.setEnabled(cachingEnabled);
                    }
                }
            }
        }
        
        // Update list
        list.setEnabled(cachingEnabled);
        
        // Update message
        CardLayout cardLayout = (CardLayout) contentPanel.getLayout();
        if (!cachingEnabled) {
            cardLayout.show(contentPanel, "empty");
        }
    }

    /**
     * Refresh the list from the store
     */
    public void refreshList() {
        model.clear();

        List<RecentManga> recentMangas = store.getAll();

        if (recentMangas.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            for (RecentManga manga : recentMangas) {
                model.addElement(formatListItem(manga));
            }
        }

        revalidate();
        repaint();
    }

    private void showEmptyState(boolean empty) {
        CardLayout cardLayout = (CardLayout) contentPanel.getLayout();
        if (empty) {
            cardLayout.show(contentPanel, "empty");
        } else {
            cardLayout.show(contentPanel, "list");
        }
        showingEmpty = empty;
    }

    /**
     * Format a RecentManga for display in the list
     */
    private String formatListItem(RecentManga manga) {
        // Format: "Title (id)"
        return String.format("%s (%s)", manga.title(), manga.mangaId());
    }

    /**
     * Extract manga ID from a formatted list item
     */
    private String extractMangaId(String listItem) {
        if (listItem == null || !listItem.contains(" (")) {
            return null;
        }
        int start = listItem.lastIndexOf(" (") + 2;
        int end = listItem.lastIndexOf(')');
        if (start >= 0 && end >= 0 && end > start) {
            return listItem.substring(start, end);
        }
        return null;
    }

    /**
     * No-arg constructor for testing
     */
    public RecentMangasPanel() {
        this(null, id -> {}, null);
    }
}

