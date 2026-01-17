package de.in.updraft;

import de.in.updraft.util.Version;

import java.io.IOException;

/**
 * Main class for managing updates.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class GithubUpdater {
    private final String currentVersion;
    private final UpdateSource source;
    private final UpdateRunner runner;

    public GithubUpdater(String currentVersion, UpdateSource source) {
        this.currentVersion = currentVersion;
        this.source = source;
        this.runner = new UpdateRunner();
    }

    /**
     * Checks for updates and returns UpdateInfo if a newer version is available.
     * 
     * @return UpdateInfo or null if no update available.
     * @throws IOException          If fetching fails.
     * @throws InterruptedException If interrupted.
     */
    public UpdateInfo checkForUpdates() throws IOException, InterruptedException {
        UpdateInfo info = source.fetchUpdate();
        if (info == null || info.version() == null) {
            return null;
        }
        Version latest = new Version(info.version());
        if (latest.isNewerThan(currentVersion)) {
            return info;
        }
        return null;
    }

    /**
     * Performs the update.
     * 
     * @param info The update info to apply.
     * @throws IOException          If update fails.
     * @throws InterruptedException If interrupted.
     */
    public void performUpdate(UpdateInfo info) throws IOException, InterruptedException {
        runner.downloadAndUpdate(info);
    }

    /**
     * Reverts to the previous version if a backup exists.
     * 
     * @throws IOException If revert fails.
     */
    public void revert() throws IOException {
        runner.revertToPreviousVersion();
    }
}
