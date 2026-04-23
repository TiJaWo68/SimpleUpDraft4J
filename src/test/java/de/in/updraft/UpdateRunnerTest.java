package de.in.updraft;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateRunnerTest {

    @Test
    public void testWindowsScriptGenerationForExe(@TempDir Path tempDir) throws Exception {
        Path fakeExe = tempDir.resolve("LogSyncPro.exe");
        Files.createFile(fakeExe);
        
        UpdateRunner runner = new UpdateRunner(fakeExe);
        Path updateFile = tempDir.resolve("update.zip");
        Files.createFile(updateFile);
        
        Path script = runner.createWindowsScript(updateFile);
        List<String> lines = Files.readAllLines(script);
        
        String content = String.join("\n", lines);
        
        assertTrue(content.contains(":wait_loop"), "Script should contain a wait loop");
        assertTrue(content.contains("move /y \"%target%\" \"%target%.old\""), "Script should check lock via move");
        assertTrue(content.contains("start \"\" \"%target%\""), "Script should restart via exe start command");
        assertTrue(content.contains("tar -xf"), "Script should use tar for extraction");
        
        Files.deleteIfExists(script);
    }

    @Test
    public void testWindowsScriptGenerationForJar(@TempDir Path tempDir) throws Exception {
        Path fakeJar = tempDir.resolve("LogSyncPro.jar");
        Files.createFile(fakeJar);
        
        UpdateRunner runner = new UpdateRunner(fakeJar);
        Path updateFile = tempDir.resolve("new-version.jar");
        Files.createFile(updateFile);
        
        Path script = runner.createWindowsScript(updateFile);
        List<String> lines = Files.readAllLines(script);
        
        String content = String.join("\n", lines);
        
        assertTrue(content.contains("copy /y"), "Script should use copy for jar files");
        assertTrue(content.contains("start \"\" javaw -jar \"%target%\""), "Script should restart via javaw -jar for jar files");
        
        Files.deleteIfExists(script);
    }
}
