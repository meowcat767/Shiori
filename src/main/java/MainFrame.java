import api.MangaDexClient;
import bookmark.BookmarkStore;
import bookmark.Bookmark;
import model.Manga;
import recent.RecentMangasStore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.net.*;
import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;

public class MainFrame extends JFrame {

    private static final Logger logger = LogManager.getLogger(MainFrame.class);

    private final MangaDexClient api = new MangaDexClient();
    private final ReaderPanel reader = new ReaderPanel();
    private Manga currentManga;
    private ChapterListPanel chapterList;
    private BookmarkStore bookmarkStore;
    private reading.ReadingProgressStore readingProgressStore;
    private RecentMangasStore recentMangasStore;
    private RecentMangasPanel recentMangasPanel;
    long CLIENT_ID = 1402751935466963214L;

    private Core discordCore;
    private Activity discordActivity;

    private void initDiscordPresence() {
        try {
            CreateParams params = new CreateParams();
            params.setClientID(1402751935466963214L);
            params.setFlags(CreateParams.getDefaultFlags());

            discordCore = new Core(params);

            discordActivity = new Activity();
            discordActivity.setDetails("Idle");
            discordActivity.setState("");



            discordActivity.assets().setLargeImage("/logo-trans.png");
            discordActivity.assets().setLargeText("Shiori");

            discordCore.activityManager().updateActivity(discordActivity);

            // Callback thread
            Thread discordThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    discordCore.runCallbacks();
                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }, "Discord-RPC");
            discordThread.setDaemon(true);
            discordThread.start();

        } catch (Exception e) {
            logger.error("Failed to init Discord Rich Presence", e);
        }
    }

    // Update manga/chapter dynamically
    public void updateDiscordManga(model.Manga manga, model.Chapter chapter) {
        if (discordActivity == null || discordCore == null || manga == null) return;

        discordActivity.setDetails("Reading: " + manga.title());
        if (chapter != null) {
            discordActivity.setState("Chapter: " + chapter.number());
        } else {
            discordActivity.setState("Browsing Shiori");
        }

        discordCore.activityManager().updateActivity(discordActivity);
    }

    private JPanel createBookmarksPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Toolbar with refresh button
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            reader.refreshBookmarksList();
        });
        toolbar.add(refreshButton);
        panel.add(toolbar, BorderLayout.NORTH);

        JList<String> bookmarksList = reader.getBookmarksList();

        // Add double-click listener to load bookmark
        bookmarksList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = bookmarksList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        Bookmark bookmark = bookmarkStore.all().get(index);
                        reader.loadBookmark(bookmark);
                    }
                }
            }
        });

        panel.add(new JScrollPane(bookmarksList), BorderLayout.CENTER);
        reader.refreshBookmarksList();
        return panel;
    }

    public MainFrame() {
        super("Shiori");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        ImageIcon icon = null;
        java.net.URL imgURL = MainFrame.class.getResource("/logo-trans.png");
        if (imgURL != null) {
            icon = new ImageIcon(imgURL);
            MainFrame.this.setIconImage(icon.getImage());
        } else {
            logger.error("Could not load logo");
        }

        chapterList = new ChapterListPanel(
                chapter -> reader.loadChapter(api, chapter, currentManga),
                chapter -> this.updateDiscordChapter(
                        currentManga.title(),
                        "Chapter " + chapter.number()
                )
        );





        // save bookmark location
        Path bookmarksPath = Paths.get(System.getProperty("user.home"), ".shiori", "bookmarks.json");
        this.bookmarkStore = new BookmarkStore(bookmarksPath);
        reader.setBookmarkStore(bookmarkStore);

        // Initialize reading progress store
        Path progressPath = Paths.get(System.getProperty("user.home"), ".shiori", "reading_progress.json");
        this.readingProgressStore = new reading.ReadingProgressStore(progressPath);
        reader.setReadingProgressStore(readingProgressStore);

        // Initialize recent mangas store
        Path recentMangasPath = Paths.get(System.getProperty("user.home"), ".shiori", "recent_mangas.json");
        this.recentMangasStore = new RecentMangasStore(recentMangasPath);

        // Create recent mangas panel
        this.recentMangasPanel = new RecentMangasPanel(recentMangasStore, mangaId -> {
            // Load manga when selected from recent list
            logger.info("Loading recent manga: {}", mangaId);
            try {
                api.getManga(mangaId).ifPresent(manga -> {
                    this.currentManga = manga;
                    chapterList.loadChapters(manga.id());
                });
            } catch (Exception e) {
                logger.error("Failed to load manga: {}", mangaId, e);
                JOptionPane.showMessageDialog(
                        MainFrame.this,
                        "Failed to load manga: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        MangaListPanel mangaList = new MangaListPanel(manga -> {
            this.currentManga = manga;
            logger.info("Selected manga: {} (ID: {})", manga.title(), manga.id());
            chapterList.loadChapters(manga.id());
            // Track this manga in recent list
            recentMangasStore.add(manga.id(), manga.title());
            if (recentMangasPanel != null) {
                recentMangasPanel.refreshList();
            }
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Manga", mangaList);
        tabs.add("Chapters", chapterList);
        tabs.add("Recent", recentMangasPanel);
        tabs.add("Bookmarks", createBookmarksPanel());

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                tabs,
                reader
        );
        split.setDividerLocation(350);
        add(split);

        setupZoomKeys();

        setupNavigationKeys();

        tabs.setMinimumSize(new Dimension(300, 100));
        reader.setMinimumSize(new Dimension(500, 100));

        setupMenu();
        initDiscordPresence();


    }



    private void setDiscordIdle() {
        if (discordCore == null) return; // make sure your Core instance exists

        try (Activity activity = new Activity()) {
            activity.setDetails("Idle in Shiori");   // main line
            activity.setState("");                    // optional second line
            discordCore.activityManager().updateActivity(activity);
        }
    }

    public void updateDiscordChapter(String mangaTitle, String chapterTitle) {
        if (discordActivity == null || discordCore == null) return;

        discordActivity.setDetails(mangaTitle);
        discordActivity.setState(chapterTitle);
        discordCore.activityManager().updateActivity(discordActivity);
    }



    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu helpMenu = new JMenu("Help");

        JMenuItem shortcutsItem = new JMenuItem("Keyboard Shortcuts");
        shortcutsItem.addActionListener(e -> showShortcuts());
        helpMenu.add(shortcutsItem);

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        JMenu advancedMenu = new JMenu("Advanced");
        JMenuItem clearCacheItem = new JMenuItem("Clear Cache");
        clearCacheItem.addActionListener(e -> reader.clearCache());
        advancedMenu.add(clearCacheItem);
        menuBar.add(advancedMenu);

        JMenu mangaMenu = new JMenu("Manga");
        JMenuItem bookmarksItem = new JMenuItem("Add Bookmark");
        bookmarksItem.addActionListener(e -> {addBookmark();});
        mangaMenu.add(bookmarksItem);
        menuBar.add(mangaMenu);

        setJMenuBar(menuBar);
    }

    private void addBookmark() {
        logger.info("Adding bookmark for current chapter");
        reader.addBookmark();
        reader.refreshBookmarksList();
    }



    private void showAbout() {
        // Load the icon
        java.net.URL imgURL = MainFrame.class.getResource("/logo-trans.png");
        ImageIcon icon = null;
        if (imgURL != null) {
            icon = new ImageIcon(imgURL);

            // Scale the image to 64x64 pixels (or whatever size you want)
            Image scaledImage = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaledImage);  // wrap it back into an ImageIcon
        } else {
            logger.error("Could not load logo");
        }
        UIManager.put("Panel.background", Color.BLACK);
        UIManager.put("OptionPane.background", Color.BLACK);
        UIManager.put("OptionPane.messageForeground", Color.WHITE); // text color

        JOptionPane.showMessageDialog(
                this,
                "Shiori - A Simple Manga Reader\n" +
                        "Powered by MangaDex API\n\n" +
                        "Logo by tevevision\n" +
                "Version 0.5a",
                "About Shiori",
                JOptionPane.INFORMATION_MESSAGE,
                icon
        );

// Reset colors to defaults if needed
        UIManager.put("Panel.background", null);
        UIManager.put("OptionPane.background", null);
        UIManager.put("OptionPane.messageForeground", null);
    }

    private void showShortcuts() {
        String shortcuts = """
                N / Right Arrow: Next Chapter
                P / Left Arrow: Previous Chapter
                Space: Page Down
                Shift + Space: Page Up
                Down Arrow: Scroll Down
                Up Arrow: Scroll Up
                + or = : Zoom In
                - : Zoom Out
                0 : Reset Zoom
                Ctrl + (+/-/0/Wheel): Zoom In/Out/Reset
                """;
        JOptionPane.showMessageDialog(this, shortcuts, "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setupNavigationKeys() {
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('n'), "nextChapter");
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("RIGHT"), "nextChapter");
        reader.getActionMap().put("nextChapter", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                chapterList.nextChapter();
            }
        });

        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('p'), "previousChapter");
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("LEFT"), "previousChapter");
        reader.getActionMap().put("previousChapter", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                chapterList.previousChapter();
            }
        });

        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(' '), "scrollDown");
        reader.getActionMap().put("scrollDown", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                reader.scrollPage(true);
            }
        });

        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("shift SPACE"), "scrollUp");
        reader.getActionMap().put("scrollUp", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                reader.scrollPage(false);
            }
        });

        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("DOWN"), "scrollLineDown");
        reader.getActionMap().put("scrollLineDown", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                reader.scrollLine(true);
            }
        });

        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("UP"), "scrollLineUp");
        reader.getActionMap().put("scrollLineUp", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                reader.scrollLine(false);
            }
        });
    }

    private void setupZoomKeys() {
        // Zoom In: '+' key, '=', and 'Ctrl +' or 'Ctrl ='
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('+'), "zoomIn");
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('='), "zoomIn");
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "zoomIn");

        reader.getActionMap().put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                reader.zoomIn();
            }
        });

        // Zoom Out: '-' key and 'Ctrl -'
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('-'), "zoomOut");
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "zoomOut");

        reader.getActionMap().put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                reader.zoomOut();
            }
        });

        // Reset Zoom: '0' and 'Ctrl 0'
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('0'), "resetZoom");
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), "resetZoom");
        reader.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK), "resetZoom");

        reader.getActionMap().put("resetZoom", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                reader.resetZoom();
            }
        });
    }


}