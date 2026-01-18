package de.in.updraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static final Logger LOGGER = LogManager.getLogger(UpdateRunner.class);

    private final Path currentJar;
    private final HttpClient httpClient;

    public UpdateRunner(Path applicationJar) {
        this.currentJar = applicationJar;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void downloadAndUpdate(UpdateInfo info) throws IOException, InterruptedException {
        Path tempJar = Files.createTempFile("updraft-new-", ".jar");

        LOGGER.info("Downloading update from: {}", info.downloadUrl());
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(info.downloadUrl())).GET().build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tempJar));

        if (response.statusCode() != 200) {
            throw new IOException("Failed to download update: HTTP " + response.statusCode());
        }

        String fileName = info.downloadUrl().substring(info.downloadUrl().lastIndexOf('/') + 1);
        Path updateFile = Files.createTempFile("updraft-update-", fileName);
        Files.move(tempJar, updateFile, StandardCopyOption.REPLACE_EXISTING);

        Path backupPath = createBackup();
        applyUpdate(updateFile, backupPath);
    }

    private Path createBackup() throws IOException {
        String fileName = currentJar.getFileName().toString();
        String backupName = fileName.replace(".jar", "") + "-backup.jar";
        Path backupPath = currentJar.getParent().resolve(backupName);
        Files.copy(currentJar, backupPath, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Backup created at: {}", backupPath);
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

    private void applyUpdate(Path updateFile, Path backupPath) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        Path scriptPath;

        if (isWindows) {
            scriptPath = createWindowsScript(updateFile);
        } else {
            scriptPath = createUnixScript(updateFile);
        }

        LOGGER.info("Starting update script and exiting...");
        ProcessBuilder pb = new ProcessBuilder();
        if (isWindows) {
            pb.command("cmd.exe", "/c", scriptPath.toAbsolutePath().toString());
        } else {
            pb.command("sh", scriptPath.toAbsolutePath().toString());
        }
        pb.start();
        System.exit(0);
    }

    private Path createWindowsScript(Path updateFile) throws IOException {
        Path script = Files.createTempFile("updraft-update-", ".bat");
        List<String> lines = new ArrayList<>();
        lines.add("@echo off");
        lines.add("timeout /t 2 /nobreak > nul"); // Wait for app to exit

        // If it's a jar, just copy. If it's an archive (zip), use tar.
        // We use .zip for Windows to avoid 7z dependency. Modern Windows has tar.
        String lower = updateFile.toString().toLowerCase();
        if (lower.endsWith(".jar")) {
            lines.add("copy /y \"" + updateFile.toAbsolutePath() + "\" \"" + currentJar.toAbsolutePath() + "\"");
            lines.add("del \"" + updateFile.toAbsolutePath() + "\"");
        } else {
            // Attempt to extract to current directory (parent of currentJar)
            // tar -xf archive -C destination
            Path dest = currentJar.getParent();
            lines.add("tar -xf \"" + updateFile.toAbsolutePath() + "\" -C \"" + dest.toAbsolutePath() + "\"");
            lines.add("del \"" + updateFile.toAbsolutePath() + "\"");
        }

        lines.add("start \"\" javaw -jar \"" + currentJar.toAbsolutePath() + "\"");
        lines.add("del \"%~f0\""); // Delete itself
        Files.write(script, lines);
        return script;
    }

    private Path createUnixScript(Path updateFile) throws IOException {
        Path script = Files.createTempFile("updraft-update-", ".sh");
        List<String> lines = new ArrayList<>();
        lines.add("#!/bin/sh");
        lines.add("sleep 2"); // Wait for app to exit

        String lower = updateFile.toString().toLowerCase();
        if (lower.endsWith(".jar")) {
            lines.add("cp -f \"" + updateFile.toAbsolutePath() + "\" \"" + currentJar.toAbsolutePath() + "\"");
            lines.add("rm \"" + updateFile.toAbsolutePath() + "\"");
        } else if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
            // Extract tarball to parent dir of currentJar
            // We assume the formatting of the archive is flat or has correct structure.
            // Typically releases might be in a subdir. We act as if they extract over.
            // tar -xzf archive -C dest
            Path dest = currentJar.getParent();
            lines.add("tar -xzf \"" + updateFile.toAbsolutePath() + "\" -C \"" + dest.toAbsolutePath() + "\"");
            lines.add("rm \"" + updateFile.toAbsolutePath() + "\"");
        }

        lines.add("java -jar \"" + currentJar.toAbsolutePath() + "\" &");
        lines.add("rm -- \"$0\""); // Delete itself
        Files.write(script, lines);

        // Make executable
        script.toFile().setExecutable(true);
        return script;
    }
}
