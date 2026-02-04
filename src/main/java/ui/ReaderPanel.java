package ui;

import api.CacheManager;
import api.MangaDexClient;
import model.Chapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReaderPanel extends JPanel {

    private static final Logger logger = LogManager.getLogger(ReaderPanel.class);

    private final JPanel pagesPanel;
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final CacheManager cacheManager = new CacheManager();
    private SwingWorker<Void, ImageIcon> currentWorker;
    private double zoomFactor = 1.0;
    private final Timer zoomTimer;
    private model.Chapter currentChapter;
    private model.Manga currentManga;
    private reading.ReadingProgressStore readingProgressStore;
    private bookmark.BookmarkStore bookmarkStore;
    private final api.MangaDexClient api = new api.MangaDexClient();
    private DefaultListModel<String> bookmarksListModel = new  DefaultListModel<>();
    private JList<String> bookmarksList = new JList<>(bookmarksListModel);
    private int currentPageIndex = 0;
    private final Timer pageTrackingTimer;
    private JScrollPane currentScrollPane;
    private boolean isLoading = true;

    public ReaderPanel() {
        setLayout(new BorderLayout());

        zoomTimer = new Timer(150, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshZoom();
            }
        });
        zoomTimer.setRepeats(false);

        // Initialize page tracking timer - runs every 2 seconds
        pageTrackingTimer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateCurrentPageIndex();
                savePageProgress();
            }
        });
        pageTrackingTimer.setRepeats(true);

        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBackground(Color.DARK_GRAY);
        statusLabel.setOpaque(true);
        add(statusLabel, BorderLayout.NORTH);

        pagesPanel = new JPanel();
        pagesPanel.setLayout(new BoxLayout(pagesPanel, BoxLayout.Y_AXIS));
        pagesPanel.setBackground(Color.BLACK);

        currentScrollPane = new JScrollPane(pagesPanel);
        currentScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        currentScrollPane.setBorder(null);

        currentScrollPane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    if (e.getWheelRotation() < 0) {
                        zoomIn();
                    } else {
                        zoomOut();
                    }
                    e.consume();
                }
            }
        });

        add(currentScrollPane, BorderLayout.CENTER);
    }

    public void setReadingProgressStore(reading.ReadingProgressStore store) {
        this.readingProgressStore = store;
    }

    public void setBookmarkStore(bookmark.BookmarkStore store) {
        this.bookmarkStore = store;
    }

    public void addBookmark() {
        logger.info("Attempting to add bookmark");
        if (bookmarkStore == null) {
            logger.error("FAULT 01: Bookmark system not initialized");
            JOptionPane.showMessageDialog(this, "Bookmark system not initialised!", "Fault 01", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (currentManga == null) {
            logger.warn("FAULT 02: No current manga to bookmark");
            JOptionPane.showMessageDialog(this, "Nothing to bookmark yet.", "Fault 02", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Update current page index before bookmarking
        updateCurrentPageIndex();

        model.Bookmark bookmark = new model.Bookmark(
                currentManga.id(),
                currentManga.title(),
                currentChapter.id(),
                currentChapter.title(),
                currentPageIndex,
                System.currentTimeMillis()
        );

        bookmarkStore.add(bookmark);
        logger.info("Bookmark added successfully for manga: {}, chapter: {}, page: {}", currentManga.title(), currentChapter.title(), currentPageIndex);
        JOptionPane.showMessageDialog(this, "Bookmark successfully added!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public void clearPages() {
        if (currentWorker != null) currentWorker.cancel(true);

        pagesPanel.removeAll();
        pagesPanel.revalidate();
        pagesPanel.repaint();
    }

    public void loadChapter(MangaDexClient api, Chapter chapter, model.Manga manga) {
        this.currentManga = manga;
        this.currentChapter = chapter;

        // Stop page tracking during load
        pageTrackingTimer.stop();
        isLoading = true;

        clearPages();
        scrollToTop();
        statusLabel.setText("Loading chapter: " + chapter.title() + "...");


        // Get saved page index before loading
        final int savedPageIndex = getSavedPageIndex(manga.id(), chapter.id());

        currentWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<String> pageUrls = api.getPageUrls(chapter.id());
                int total = pageUrls.size();
                int current = 0;
                for (String url : pageUrls) {
                    if (isCancelled()) break;

                    BufferedImage image;
                    if (cacheManager.isCached(url)) {
                        byte[] data = cacheManager.getFromCache(url);
                        try (InputStream in = new ByteArrayInputStream(data)) {
                            image = ImageIO.read(in);
                        }
                    } else {
                        try (InputStream in = new URL(url).openStream()) {
                            byte[] data = in.readAllBytes();
                            cacheManager.saveToCache(url, data);
                            try (InputStream imageIn = new ByteArrayInputStream(data)) {
                                image = ImageIO.read(imageIn);
                            }
                        }


                    }

                    current++;
                    if (image != null) {
                        publish(new ImageIcon(image));
                    }

                    final String progressText = String.format("Loading pages: %d / %d", current, total);
                    SwingUtilities.invokeLater(() -> statusLabel.setText(progressText));
                }
                return null;
            }

            @Override
            protected void process(List<ImageIcon> icons) {
                for (ImageIcon icon : icons) {
                    JLabel label = new JLabel(scaleIcon(icon));
                    label.putClientProperty("originalIcon", icon);
                    label.setAlignmentX(Component.CENTER_ALIGNMENT);
                    label.setBackground(Color.BLACK);
                    label.setOpaque(true);
                    pagesPanel.add(label);
                }
                pagesPanel.revalidate();
            }

            @Override
            protected void done() {
                if (!isCancelled()) {
                    isLoading = false;
                    statusLabel.setText("Chapter Loaded: " + chapter.title());

                    // Restore saved page position after loading
                    if (savedPageIndex > 0 && pagesPanel.getComponentCount() > savedPageIndex) {
                        SwingUtilities.invokeLater(() -> {
                            scrollToPage(savedPageIndex);
                        });
                    }

                    // Start page tracking
                    pageTrackingTimer.start();
                }
            }
        };

        currentWorker.execute();
    }

    private ImageIcon scaleIcon(ImageIcon icon) {
        int width = Math.max(1, (int) (icon.getIconWidth() * zoomFactor));
        int height = Math.max(1, (int) (icon.getIconHeight() * zoomFactor));

        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();

        // Use bilinear interpolation for a good balance between speed and quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        g2d.drawImage(icon.getImage(), 0, 0, width, height, null);
        g2d.dispose();

        return new ImageIcon(scaledImage);
    }

    public void zoomIn() {
        logger.debug("Zooming in from {} to {}", zoomFactor, zoomFactor * 1.2);
        zoomFactor *= 1.2;
        zoomTimer.restart();
    }

    public void zoomOut() {
        logger.debug("Zooming out from {} to {}", zoomFactor, zoomFactor / 1.2);
        zoomFactor /= 1.2;
        zoomTimer.restart();
    }

    public void resetZoom() {
        logger.debug("Resetting zoom from {} to 1.0", zoomFactor);
        zoomFactor = 1.0;
        zoomTimer.restart();
    }

    public void scrollPage(boolean down) {
        if (currentScrollPane != null) {
            JScrollBar vertical = currentScrollPane.getVerticalScrollBar();
            int amount = currentScrollPane.getViewport().getHeight() - 50;
            if (!down) amount = -amount;
            vertical.setValue(vertical.getValue() + amount);
        }
    }

    public void scrollLine(boolean down) {
        if (currentScrollPane != null) {
            JScrollBar vertical = currentScrollPane.getVerticalScrollBar();
            int amount = vertical.getUnitIncrement() * 3;
            if (!down) amount = -amount;
            vertical.setValue(vertical.getValue() + amount);
        }
    }

    public void scrollToTop() {
        SwingUtilities.invokeLater(() -> {
            if (currentScrollPane != null) {
                currentScrollPane.getVerticalScrollBar().setValue(0);
                currentScrollPane.getHorizontalScrollBar().setValue(0);
            }
        });
    }

    private void refreshZoom() {
        double relativeScroll = 0;
        int viewWidth = 0;
        if (currentScrollPane != null) {
            JScrollBar vertical = currentScrollPane.getVerticalScrollBar();
            int max = vertical.getMaximum() - vertical.getVisibleAmount();
            if (max > 0) {
                relativeScroll = (double) vertical.getValue() / max;
            }
            viewWidth = currentScrollPane.getViewport().getWidth();
        }

        for (Component comp : pagesPanel.getComponents()) {
            if (comp instanceof JLabel label && label.getClientProperty("originalIcon") instanceof ImageIcon originalIcon) {
                label.setIcon(scaleIcon(originalIcon));
            }
        }
        pagesPanel.revalidate();
        pagesPanel.repaint();

        if (currentScrollPane != null) {
            final double finalRelativeScroll = relativeScroll;
            final int finalViewWidth = viewWidth;
            SwingUtilities.invokeLater(() -> {
                // Adjust for horizontal scroll to keep it centered if it was centered
                JScrollBar horizontal = currentScrollPane.getHorizontalScrollBar();
                int newMaxH = horizontal.getMaximum() - horizontal.getVisibleAmount();
                if (newMaxH > 0) {
                    horizontal.setValue(newMaxH / 2);
                }

                JScrollBar vertical = currentScrollPane.getVerticalScrollBar();
                int max = vertical.getMaximum() - vertical.getVisibleAmount();
                vertical.setValue((int) (finalRelativeScroll * max));
            });
        }
    }

    public void clearCache() {
        logger.info("Clearing image cache");
        cacheManager.clearCache();
        logger.info("Cache cleared successfully");
        JOptionPane.showMessageDialog(this, "Cache cleared successfully.");
    }

    /**
     * Update the current page index based on scroll position
     */
    private void updateCurrentPageIndex() {
        if (currentScrollPane == null || isLoading || pagesPanel.getComponentCount() == 0) {
            return;
        }

        JScrollBar vertical = currentScrollPane.getVerticalScrollBar();
        int scrollValue = vertical.getValue();
        int viewportHeight = currentScrollPane.getViewport().getHeight();
        int middlePoint = scrollValue + viewportHeight / 2;

        int pageCount = pagesPanel.getComponentCount();
        int closestPageIndex = 0;
        int minDistance = Integer.MAX_VALUE;

        for (int i = 0; i < pageCount; i++) {
            Component comp = pagesPanel.getComponent(i);
            Rectangle bounds = comp.getBounds();
            int compMiddle = bounds.y + bounds.height / 2;
            int distance = Math.abs(compMiddle - middlePoint);

            if (distance < minDistance) {
                minDistance = distance;
                closestPageIndex = i;
            }
        }

        currentPageIndex = closestPageIndex;
    }

    /**
     * Save current page progress to reading progress store
     */
    private void savePageProgress() {
        if (readingProgressStore == null || currentManga == null || currentChapter == null || isLoading) {
            return;
        }

        readingProgressStore.saveProgress(
            currentManga.id(),
            currentChapter.id(),
            currentPageIndex
        );
    }

    /**
     * Scroll to a specific page index and update tracking
     */
    public void scrollToPage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pagesPanel.getComponentCount()) return;
        Component comp = pagesPanel.getComponent(pageIndex);
        if (comp instanceof JComponent jComp) {
            jComp.scrollRectToVisible(jComp.getBounds());
            currentPageIndex = pageIndex;
        }
    }

    /**
     * Get saved page position for a manga
     */
    public int getSavedPageIndex(String mangaId, String chapterId) {
        if (readingProgressStore == null) return 0;
        return readingProgressStore.getPageIndex(mangaId, chapterId);
    }

    public void refreshBookmarksList() {
        bookmarksListModel.clear();
        if (bookmarkStore != null) {
            for (model.Bookmark bookmark : bookmarkStore.all()) {
                String displayText = String.format("%s - %s (p.%d)",
                        bookmark.mangaTitle(),
                        bookmark.chapterTitle() != null ? bookmark.chapterTitle() : "Chapter",
                        bookmark.page() + 1);
                bookmarksListModel.addElement(displayText);
            }
        }
    }

    public JList<String> getBookmarksList() {
        return bookmarksList;
    }

    public void loadBookmark(model.Bookmark bookmark) {
        if (bookmark == null) return;

        // Get page from reading progress store if available
        final int progressPage = getSavedPageIndex(bookmark.mangaId(), bookmark.chapterId());
        final int bookmarkPage = progressPage > 0 ? progressPage : bookmark.page();

        // Create a minimal manga object for loading the chapter
        model.Manga manga = new model.Manga(
            bookmark.mangaId(),
            bookmark.mangaTitle()
        );

        // Create a chapter object
        model.Chapter chapter = new model.Chapter(
            bookmark.chapterId(),
            bookmark.chapterTitle() != null ? bookmark.chapterTitle() : "1",
            "1"
        );

        // Load the chapter - page will be restored in loadChapter's done() callback
        pageTrackingTimer.stop();
        isLoading = true;

        this.currentManga = manga;
        this.currentChapter = chapter;
        clearPages();
        scrollToTop();
        statusLabel.setText("Loading chapter: " + chapter.title() + "...");

        currentWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<String> pageUrls = api.getPageUrls(chapter.id());
                int total = pageUrls.size();
                int current = 0;
                for (String url : pageUrls) {
                    if (isCancelled()) break;

                    BufferedImage image;
                    if (cacheManager.isCached(url)) {
                        byte[] data = cacheManager.getFromCache(url);
                        try (InputStream in = new ByteArrayInputStream(data)) {
                            image = ImageIO.read(in);
                        }
                    } else {
                        try (InputStream in = new URL(url).openStream()) {
                            byte[] data = in.readAllBytes();
                            cacheManager.saveToCache(url, data);
                            try (InputStream imageIn = new ByteArrayInputStream(data)) {
                                image = ImageIO.read(imageIn);
                            }
                        }
                    }

                    current++;
                    if (image != null) {
                        publish(new ImageIcon(image));
                    }

                    final String progressText = String.format("Loading pages: %d / %d", current, total);
                    SwingUtilities.invokeLater(() -> statusLabel.setText(progressText));
                }
                return null;
            }

            @Override
            protected void process(List<ImageIcon> icons) {
                for (ImageIcon icon : icons) {
                    JLabel label = new JLabel(scaleIcon(icon));
                    label.putClientProperty("originalIcon", icon);
                    label.setAlignmentX(Component.CENTER_ALIGNMENT);
                    label.setBackground(Color.BLACK);
                    label.setOpaque(true);
                    pagesPanel.add(label);
                }
                pagesPanel.revalidate();
            }

            @Override
            protected void done() {
                if (!isCancelled()) {
                    isLoading = false;
                    statusLabel.setText("Chapter Loaded: " + chapter.title());

                    // Restore saved page position
                    if (bookmarkPage > 0 && pagesPanel.getComponentCount() > bookmarkPage) {
                        SwingUtilities.invokeLater(() -> {
                            scrollToPage(bookmarkPage);
                        });
                    }

                    // Start page tracking
                    pageTrackingTimer.start();
                }
            }
        };

        currentWorker.execute();
    }
}

