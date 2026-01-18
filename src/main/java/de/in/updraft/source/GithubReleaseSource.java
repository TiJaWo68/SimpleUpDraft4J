package de.in.updraft.source;

import de.in.updraft.UpdateChannel;
import de.in.updraft.UpdateInfo;
import de.in.updraft.UpdateSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches update information from GitHub Releases.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class GithubReleaseSource implements UpdateSource {
    private final String repoOwner;
    private final String repoName;
    private final UpdateChannel channel;
    private final HttpClient httpClient;

    public GithubReleaseSource(String repoOwner, String repoName, UpdateChannel channel) {
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.channel = channel;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
            .getLogger(GithubReleaseSource.class.getName());

    @Override
    public UpdateInfo fetchUpdate() throws IOException, InterruptedException {
        String url = channel == UpdateChannel.STABLE
                ? String.format("https://api.github.com/repos/%s/%s/releases/latest", repoOwner, repoName)
                : String.format("https://api.github.com/repos/%s/%s/releases", repoOwner, repoName);

        LOGGER.info("Fetching update from: " + url + " (Channel: " + channel + ")");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            LOGGER.warning("No releases found (404) at " + url);
            return null; // No releases found
        }
        if (response.statusCode() != 200) {
            LOGGER.severe("GitHub API returned status " + response.statusCode());
            throw new IOException("GitHub API returned status " + response.statusCode());
        }

        String json = response.body();
        UpdateInfo info;
        if (channel == UpdateChannel.STABLE) {
            info = parseOne(json);
        } else {
            // Take the first one from the list (array starts with [)
            info = parseFirst(json);
        }

        if (info != null) {
            LOGGER.info("Found version: " + info.version());
        } else {
            LOGGER.info("No version found in response.");
        }
        return info;
    }

    private UpdateInfo parseFirst(String jsonArray) throws IOException {
        // Very lazy array parsing: find the first object {}
        // Since we only need the first release, we can treat the whole array as
        // containing at least one object.
        // We look for the first occurrence of "tag_name", "body",
        // "browser_download_url"
        return parseOne(jsonArray);
    }

    protected UpdateInfo parseOne(String json) throws IOException {
        String version = extractValue(json, "tag_name");
        String body = extractValue(json, "body");
        String downloadUrl = extractDownloadUrl(json);

        if (version == null || downloadUrl == null) {
            throw new IOException("Failed to parse GitHub release info");
        }

        return new UpdateInfo(version, downloadUrl, body != null ? body : "");
    }

    protected String extractValue(String json, String key) {
        // Regex to find "key": "value"
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\r\\n", "\n").replace("\\n", "\n");
        }
        return null;
    }

    protected String extractDownloadUrl(String json) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        // Look for browser_download_url in assets
        // We look for tar.gz on Linux/Mac and .7z/.zip on Windows
        String regex;
        if (isWindows) {
            // Prioritize zip as it's the standard now
            regex = "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.zip)\"";
        } else {
            regex = "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.tar\\.gz)\"";
        }

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Fallback: if preferred format not found, look for any archive
        if (isWindows) {
            regex = "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.tar\\.gz)\"";
            pattern = Pattern.compile(regex);
            matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }
}
