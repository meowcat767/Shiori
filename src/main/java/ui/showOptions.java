package ui;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

public class showOptions {

    private static final String KEY_CACHE = "cachingEnabled";

    private final Preferences prefs =
            Preferences.userNodeForPackage(showOptions.class);

    private boolean iWantCaching;

    public showOptions() {
        // Load persisted value (default = true)
        iWantCaching = prefs.getBoolean(KEY_CACHE, true);
    }

    public void showOptions() {
        initOptionsUI();
    }

    private void initOptionsUI() {
        JFrame frame = new JFrame("Options");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new FlowLayout());

        JCheckBox c1 = new JCheckBox("Enable Caching", iWantCaching);

        c1.addActionListener(e -> {
            iWantCaching = c1.isSelected();
            prefs.putBoolean(KEY_CACHE, iWantCaching);
        });

        frame.add(c1);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public boolean isCachingEnabled() {
        return iWantCaching;
    }
}