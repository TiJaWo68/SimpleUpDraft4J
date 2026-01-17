package de.in.updraft.source;

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
 * Fetches update information from a static JSON file.
 * Expects: { "version": "...", "url": "...", "changelog": "..." }
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class JsonMetadataSource implements UpdateSource {
    private final String metadataUrl;
    private final HttpClient httpClient;

    public JsonMetadataSource(String metadataUrl) {
        this.metadataUrl = metadataUrl;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public UpdateInfo fetchUpdate() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(metadataUrl))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Metadata URL returned status " + response.statusCode());
        }

        String json = response.body();
        String version = extractValue(json, "version");
        String downloadUrl = extractValue(json, "url");
        String changelog = extractValue(json, "changelog");

        if (version == null || downloadUrl == null) {
            throw new IOException("Failed to parse metadata JSON");
        }

        return new UpdateInfo(version, downloadUrl, changelog != null ? changelog : "");
    }

    protected String extractValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\r\\n", "\n").replace("\\n", "\n");
        }
        return null;
    }
}
