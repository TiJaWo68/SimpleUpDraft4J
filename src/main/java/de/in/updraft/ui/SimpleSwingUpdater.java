package de.in.updraft.ui;

import de.in.updraft.GithubUpdater;
import de.in.updraft.UpdateInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Swing-based helper for displaying update dialogs.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class SimpleSwingUpdater {
    private static final Logger LOGGER = LogManager.getLogger(SimpleSwingUpdater.class);

    private final GithubUpdater updater;

    public SimpleSwingUpdater(GithubUpdater updater) {
        this.updater = updater;
    }

    public void showUpdateDialog(UpdateInfo info) {
        if (info == null)
            return;

        JDialog dialog = new JDialog((Frame) null, "Update Available", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("A new version is available: " + info.version());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        JTextArea changelogArea = new JTextArea(info.changelog());
        changelogArea.setEditable(false);
        changelogArea.setLineWrap(true);
        changelogArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(changelogArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton updateButton = new JButton("Update Now");
        JButton laterButton = new JButton("Later");

        updateButton.addActionListener(e -> {
            dialog.dispose();
            new Thread(() -> {
                try {
                    updater.performUpdate(info);
                } catch (Exception ex) {
                    LOGGER.error("Update failed", ex);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                            "Update failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                }
            }).start();
        });

        laterButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(updateButton);
        buttonPanel.add(laterButton);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
