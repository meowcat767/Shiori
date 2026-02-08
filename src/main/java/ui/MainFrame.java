package ui;

import api.CacheManager;
import api.LocalPDFLoader;
import api.LocalPDFStore;
import api.MangaDexClient;
import bookmark.BookmarkStore;
import model.Bookmark;
import model.Manga;
import plugin.LibraryManager;
import plugin.PluginContext;
import plugin.PluginManager;
import plugin.ShioriPlugin;
import recent.RecentMangasStore;
import services.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainFrame extends JFrame {

    private static final Logger logger = LogManager.getLogger(MainFrame.class);

    private final MangaDexClient api = new MangaDexClient();
    private final ReaderPanel reader = new ReaderPanel();
    private final showOptions options = new showOptions();
    private Manga currentManga;
    private ChapterListPanel chapterList;
    private BookmarkStore bookmarkStore;
    private reading.ReadingProgressStore readingProgressStore;
    
    private RecentMangasStore recentMangasStore;
    private RecentMangasPanel recentMangasPanel;
    
    // Discord RPC service
    private DiscordRPCService discordRPCService;

    // Plugin system components
    private final PluginManager pluginManager;
    private final LibraryManager libraryManager;
    private PluginContext pluginContext;
    private JMenu pluginsMenu;

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

    /**
     * Constructor with plugin system.
     */
    public MainFrame(PluginManager pluginManager, LibraryManager libraryManager) {
        super("Shiori");
        this.pluginManager = pluginManager;
        this.libraryManager = libraryManager;
        
        initializePluginContext();
        initializeUI();
        initializeStores();
        setupPluginHooks();
    }

    /**
     * Initialize the plugin context for plugins to use.
     */
    private void initializePluginContext() {
        if (pluginManager != null) {
            CacheManager cacheManager = new CacheManager();
            cacheManager.setOptions(options);
            
            pluginContext = new PluginContext(
                    api,
                    bookmarkStore,
                    readingProgressStore,
                    recentMangasStore,
                    cacheManager,
                    pluginManager,
                    getJMenuBar()
            );
        }
    }

    /**
     * Initialize UI components.
     */
    private void initializeUI() {
        SplashScreen splash = new SplashScreen();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        // Add window close listener for cleanup
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (discordRPCService != null) {
                    discordRPCService.stop();
                    logger.info("Discord RPC service stopped on application close");
                }
            }
        });
        setSize(1200, 800);
        setLocationRelativeTo(null);

        ImageIcon icon;
        java.net.URL imgURL = MainFrame.class.getResource("/logo-trans.png");
        if (imgURL != null) {
            icon = new ImageIcon(imgURL);
            MainFrame.this.setIconImage(icon.getImage());
        } else {
            logger.error("Could not load logo for ui.MainFrame()");
        }

        chapterList = new ChapterListPanel(
                chapter -> reader.loadChapter(api, chapter, currentManga)
        );

        setupMenu();
        SwingUtilities.invokeLater(splash::hide);
    }

    /**
     * Initialize stores and panels.
     */
    private void initializeStores() {
        // save bookmark location
        Path bookmarksPath = Paths.get(System.getProperty("user.home"), ".shiori", "bookmarks.shiomark");
        this.bookmarkStore = new BookmarkStore(bookmarksPath);
        reader.setBookmarkStore(bookmarkStore);

        // Initialize reading progress store
        Path progressPath = Paths.get(System.getProperty("user.home"), ".shiori", "reading_progress.shioprogress");
        this.readingProgressStore = new reading.ReadingProgressStore(progressPath);
        reader.setReadingProgressStore(readingProgressStore);

        // Initialize recent mangas store
        Path recentMangasPath = Paths.get(System.getProperty("user.home"), ".shiori", "recent_mangas.shiorecents");
        this.recentMangasStore = new RecentMangasStore(recentMangasPath);

        // Initialize pdf store path
        Path pdfStorePath = Paths.get(
                System.getProperty("user.home"),
                ".shiori",
                "local_pdfs.json"
        );
        LocalPDFStore pdfStore = new LocalPDFStore(pdfStorePath);

        // Create recent mangas panel
        this.recentMangasPanel = new RecentMangasPanel(recentMangasStore, mangaId -> {
            // Load manga when selected from recent list
            logger.info("Loading recent manga: {}", mangaId);
            try {
                api.getManga(mangaId).ifPresent(manga -> {
                    this.currentManga = manga;
                    chapterList.loadChapters(manga.id());
                    notifyPluginsMangaLoaded(manga);
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
        }, options);

        MangaListPanel mangaList = new MangaListPanel(manga -> {
            this.currentManga = manga;
            logger.info("Selected manga: {} (ID: {})", manga.title(), manga.id());
            chapterList.loadChapters(manga.id());
            // Track this manga in recent list
            recentMangasStore.add(manga.id(), manga.title());
            if (recentMangasPanel != null) {
                recentMangasPanel.refreshList();
            }
            // Notify plugins
            notifyPluginsMangaLoaded(manga);
        });

        JPanel localPanel = new JPanel(new BorderLayout());
        JButton addPdf = new JButton("Add PDF…");
        addPdf.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setAcceptAllFileFilterUsed(false);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("PDF files", "pdf");
            chooser.addChoosableFileFilter(filter);
            chooser.setFileFilter(filter);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                LocalPDFLoader.loadIntoReader(
                        chooser.getSelectedFile(),
                        reader,
                        pdfStore
                );
            }
        });

        localPanel.add(addPdf, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Manga", mangaList);
        tabs.add("Chapters", chapterList);
        tabs.add("Local", localPanel);
        tabs.add("Recent", recentMangasPanel);
        tabs.add("Bookmarks", createBookmarksPanel());

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                tabs,
                reader
        );
        split.setDividerLocation(350);
        add(split);

        if (BuildInfo.isPreview()) {
            JOptionPane.showMessageDialog(
                    this,
                    "This is a preview build. Expect bugs",
                    "Preview Build Warning",
                    JOptionPane.WARNING_MESSAGE
            );
        }

        setupZoomKeys();
        setupNavigationKeys();
        
        // Initialize and start Discord RPC service
        discordRPCService = new DiscordRPCService();
        discordRPCService.start(1402751935466963214L);
        discordRPCService.initRpc();


        tabs.setMinimumSize(new Dimension(300, 100));
        reader.setMinimumSize(new Dimension(500, 100));
    }

    /**
     * Setup hooks for plugin lifecycle events.
     */
    private void setupPluginHooks() {
        // Initialize plugins if plugin manager exists
        if (pluginManager != null && pluginContext != null) {
            logger.info("Initializing {} plugin(s)", pluginManager.getPluginCount());
            
            for (ShioriPlugin plugin : pluginManager.getEnabledPlugins()) {
                try {
                    plugin.init(pluginContext);
                    logger.info("Initialized plugin: {}", plugin.getName());
                } catch (Exception e) {
                    logger.error("Failed to initialize plugin {}: {}", plugin.getName(), e.getMessage());
                }
            }
        }
    }

    /**
     * Notify plugins that a manga was loaded.
     */
    private void notifyPluginsMangaLoaded(Manga manga) {
        if (pluginManager != null) {
            pluginManager.notifyMangaLoaded(manga);
        }
        
        // Update Discord RPC with manga information
        updateDiscordRPC(manga, null);
    }
    
    /**
     * Update Discord Rich Presence with current manga and chapter information.
     * @param manga - The manga being read (null to clear)
     * @param chapter - The current chapter (null if no chapter selected)
     */
    private void updateDiscordRPC(Manga manga, model.Chapter chapter) {
        if (discordRPCService == null || !discordRPCService.isReady()) {
            return;
        }
        
        if (manga == null) {
            discordRPCService.clearActivity();
            return;
        }
        
        String mangaTitle = manga.title();
        String chapterInfo = "Chapters available";
        
        if (chapter != null) {
            chapterInfo = chapter.title() != null ? chapter.title() : "Chapter " + chapter.number();
        }
        
        discordRPCService.updateActivity(mangaTitle, chapterInfo, chapter != null);
        logger.debug("Updated Discord RPC: {} - {}", mangaTitle, chapterInfo);
    }

    /**
     * Notify plugins that a chapter was loaded.
     */
    private void notifyPluginsChapterLoaded(model.Chapter chapter, Manga manga) {
        if (pluginManager != null) {
            pluginManager.notifyChapterLoaded(chapter, manga);
        }
    }

    /**
     * Notify plugins that reading is complete.
     */
    private void notifyPluginsReadingComplete(model.Chapter chapter, Manga manga) {
        if (pluginManager != null) {
            pluginManager.notifyReadingComplete(chapter, manga);
        }
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem optionsItem = new JMenuItem("Options");
        optionsItem.addActionListener(e -> {
            showOptions options = new showOptions();
            options.showOptions();
            logger.info("Trying to fire ui.showOptions.ui.showOptions()...");});
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        JMenuItem statsItem = new JMenuItem("Statistics");
        statsItem.addActionListener(e -> {
            if (currentManga == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "No manga selected",
                        "Statistics",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            new MangaStatisticsWindow(
                    api,
                    currentManga.id(),
                    currentManga.title()
            );
        });
        fileMenu.add(statsItem);
        fileMenu.add(optionsItem);
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);



        // Plugins menu
        pluginsMenu = new JMenu("Plugins");
        JMenuItem pluginManagerItem = new JMenuItem("Plugin Manager...");
        pluginManagerItem.addActionListener(e -> showPluginManager());
        pluginsMenu.add(pluginManagerItem);
        
        JMenuItem librariesItem = new JMenuItem("Manage Libraries...");
        librariesItem.addActionListener(e -> showLibrariesManager());
        pluginsMenu.add(librariesItem);
        
        pluginsMenu.addSeparator();
        
        // List loaded plugins
        if (pluginManager != null) {
            int pluginCount = pluginManager.getPluginCount();
            JMenuItem pluginsInfoItem = new JMenuItem(
                    String.format("%d plugin(s) loaded", pluginCount)
            );
            pluginsInfoItem.setEnabled(false);
            pluginsMenu.add(pluginsInfoItem);
            
            if (pluginCount > 0) {
                pluginsMenu.addSeparator();
                for (ShioriPlugin plugin : pluginManager.getAllPlugins()) {
                    JMenuItem pluginItem = new JMenuItem(
                            plugin.getName() + " v" + plugin.getVersion()
                    );
                    pluginItem.setEnabled(false);
                    pluginsMenu.add(pluginItem);
                }
            }
        } else {
            JMenuItem noPluginsItem = new JMenuItem("No plugins loaded");
            noPluginsItem.setEnabled(false);
            pluginsMenu.add(noPluginsItem);
        }
        
        menuBar.add(pluginsMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem shortcutsItem = new JMenuItem("Keyboard Shortcuts");
        shortcutsItem.addActionListener(e -> showShortcuts());
        helpMenu.add(shortcutsItem);

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);


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
        menuBar.add(helpMenu);


        setJMenuBar(menuBar);
    }

    /**
     * Show the plugin manager dialog.
     */
    private void showPluginManager() {
        if (pluginManager != null && libraryManager != null) {
            plugin.ui.PluginManagerDialog dialog = new plugin.ui.PluginManagerDialog(
                    this, pluginManager, libraryManager
            );
            dialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Plugin system is not available.",
                    "Plugins Unavailable",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    /**
     * Show the libraries manager dialog.
     */
    private void showLibrariesManager() {
        if (libraryManager != null) {
            JOptionPane.showMessageDialog(
                    this,
                    libraryManager.getLibraryInfo(),
                    "Shared Libraries",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Plugin system is not available.",
                    "Libraries Unavailable",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
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

            // Scale the image to 64x64 pixels
            Image scaledImage = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaledImage);
        } else {
            logger.error("Could not load logo for showAbout()");
        }
        UIManager.put("Panel.background", Color.BLACK);
        UIManager.put("OptionPane.background", Color.BLACK);
        UIManager.put("OptionPane.messageForeground", Color.WHITE);

        JOptionPane.showMessageDialog(
                this,
                "Yomikomu Manga Reader\n" +
                        "Powered by MangaDex API\n\n" +
                        "Logo by tevevision\nWritten by meowcat767\n" +
                        "(C) Catbell Software 2026\n" +
                "Version 1.1",
                "About Yomikomiu",
                JOptionPane.INFORMATION_MESSAGE,
                icon
        );

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
        // Zoom In
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

        // Zoom Out
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

        // Reset Zoom
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

    /**
     * Get the plugin manager.
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * Get the library manager.
     */
    public LibraryManager getLibraryManager() {
        return libraryManager;
    }
}

