package de.in.updraft;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the physical update process: download, backup, script creation, and
 * restart.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class UpdateRunner {
    private final Path currentJar;
    private final HttpClient httpClient;

    public UpdateRunner() {
        try {
            this.currentJar = Paths.get(UpdateRunner.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            throw new RuntimeException("Failed to determine current JAR path", e);
        }
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void downloadAndUpdate(UpdateInfo info) throws IOException, InterruptedException {
        Path tempJar = Files.createTempFile("updraft-new-", ".jar");

        System.out.println("Downloading update from: " + info.downloadUrl());
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(info.downloadUrl())).GET().build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tempJar));

        if (response.statusCode() != 200) {
            throw new IOException("Failed to download update: HTTP " + response.statusCode());
        }

        Path backupPath = createBackup();
        applyUpdate(tempJar, backupPath);
    }

    private Path createBackup() throws IOException {
        String fileName = currentJar.getFileName().toString();
        String backupName = fileName.replace(".jar", "") + "-backup.jar";
        Path backupPath = currentJar.getParent().resolve(backupName);
        Files.copy(currentJar, backupPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Backup created at: " + backupPath);
        return backupPath;
    }

    public void revertToPreviousVersion() throws IOException {
        String fileName = currentJar.getFileName().toString();
        String backupName = fileName.replace(".jar", "") + "-backup.jar";
        Path backupPath = currentJar.getParent().resolve(backupName);

        if (!Files.exists(backupPath)) {
            throw new IOException("No backup found at " + backupPath);
        }

        // We use the same script logic to revert
        Path tempRevert = Files.createTempFile("updraft-revert-", ".jar");
        Files.copy(backupPath, tempRevert, StandardCopyOption.REPLACE_EXISTING);
        applyUpdate(tempRevert, null);
    }

    private void applyUpdate(Path newJar, Path backupPath) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        Path scriptPath;

        if (isWindows) {
            scriptPath = createWindowsScript(newJar);
        } else {
            scriptPath = createUnixScript(newJar);
        }

        System.out.println("Starting update script and exiting...");
        ProcessBuilder pb = new ProcessBuilder();
        if (isWindows) {
            pb.command("cmd.exe", "/c", scriptPath.toAbsolutePath().toString());
        } else {
            pb.command("sh", scriptPath.toAbsolutePath().toString());
        }
        pb.start();
        System.exit(0);
    }

    private Path createWindowsScript(Path newJar) throws IOException {
        Path script = Files.createTempFile("updraft-update-", ".bat");
        List<String> lines = new ArrayList<>();
        lines.add("@echo off");
        lines.add("timeout /t 2 /nobreak > nul"); // Wait for app to exit
        lines.add("copy /y \"" + newJar.toAbsolutePath() + "\" \"" + currentJar.toAbsolutePath() + "\"");
        lines.add("del \"" + newJar.toAbsolutePath() + "\"");
        lines.add("start \"\" javaw -jar \"" + currentJar.toAbsolutePath() + "\"");
        lines.add("del \"%~f0\""); // Delete itself
        Files.write(script, lines);
        return script;
    }

    private Path createUnixScript(Path newJar) throws IOException {
        Path script = Files.createTempFile("updraft-update-", ".sh");
        List<String> lines = new ArrayList<>();
        lines.add("#!/bin/sh");
        lines.add("sleep 2"); // Wait for app to exit
        lines.add("cp -f \"" + newJar.toAbsolutePath() + "\" \"" + currentJar.toAbsolutePath() + "\"");
        lines.add("rm \"" + newJar.toAbsolutePath() + "\"");
        lines.add("java -jar \"" + currentJar.toAbsolutePath() + "\" &");
        lines.add("rm -- \"$0\""); // Delete itself
        Files.write(script, lines);

        // Make executable
        script.toFile().setExecutable(true);
        return script;
    }
}
