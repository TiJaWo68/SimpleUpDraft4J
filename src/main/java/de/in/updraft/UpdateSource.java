package de.in.updraft;

import java.io.IOException;

/**
 * Interface for update sources.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public interface UpdateSource {
    /**
     * Fetches the latest update information from this source.
     * 
     * @return UpdateInfo containing version, URL and changelog.
     * @throws IOException          If fetching fails.
     * @throws InterruptedException If the request is interrupted.
     */
    UpdateInfo fetchUpdate() throws IOException, InterruptedException;
}
