package ui;

import api.MangaDexClient;
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;

public class MangaStatisticsWindow {

    private final JFrame frame;
    private final JPanel contentPanel;
    private final JLabel statusLabel;

    public MangaStatisticsWindow(MangaDexClient client, String mangaId, String mangaTitle) {
        frame = new JFrame("Statistics: " + mangaTitle);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        statusLabel = new JLabel("Hold On! Loading statistics...", SwingConstants.CENTER);
        contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayout(0, 1, 5, 5));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        frame.add(statusLabel, BorderLayout.NORTH);
        frame.add(contentPanel, BorderLayout.CENTER);

        frame.setSize(300, 250);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Fetch stats in background
        fetchStatistics(client, mangaId);
    }

    private void fetchStatistics(MangaDexClient client, String mangaId) {
        SwingWorker<JsonNode, Void> worker = new SwingWorker<>() {
            @Override
            protected JsonNode doInBackground() {
                try {
                    return client.getMangaStats(mangaId); // catch exceptions here
                } catch (Exception e) {
                    e.printStackTrace();
                    return null; // return null on failure
                }
            }

            @Override
            protected void done() {
                try {
                    JsonNode stats = get(); // no checked exception from get()
                    displayStats(stats);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void displayStats(JsonNode stats) {
        contentPanel.removeAll();

        if (stats == null || stats.isMissingNode()) {
            statusLabel.setText("No statistics available");
            return;
        }

        statusLabel.setText("Statistics loaded");

        JsonNode rating = stats.path("rating");

        double mean = rating.path("average").asDouble(-1);
        double bayesian = rating.path("bayesian").asDouble(-1);
        int follows = stats.path("follows").asInt(-1);

        contentPanel.add(new JLabel(
                mean >= 0 ? String.format("Mean Rating: %.2f / 10", mean)
                        : "Mean Rating: N/A"
        ));

        contentPanel.add(new JLabel(
                bayesian >= 0 ? String.format("Bayesian Rating: %.2f / 10", bayesian)
                        : "Bayesian Rating: N/A"
        ));

        contentPanel.add(new JLabel(
                follows >= 0 ? "Follows: " + follows
                        : "Follows: N/A"
        ));

        contentPanel.revalidate();
        contentPanel.repaint();
    }

}