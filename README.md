# SimpleUpDraft4J

Lightweight Java 17 auto-update library for GitHub and Generic JSON sources.

## Architecture

SimpleUpDraft4J is designed with a strict separation of concerns to support both headless (server/CLI) and GUI environments.

- **Core Logic**: `GithubUpdater` and `UpdateRunner` manage the update flow, semantic versioning, file operations, and application restarts without any UI dependencies.
- **Update Sources**: The `UpdateSource` interface allows plugging in different providers (GitHub API, static JSON via URL).
- **Safety**: Every update creates a local backup. A rollback mechanism is available if the new version fails.
- **UI Interaction**: A separate `SimpleSwingUpdater` helper provides standard Swing-based update dialogs for desktop applications.

## Interfaces & Models

### `UpdateSource`
The core interface for update providers.
```java
public interface UpdateSource {
    UpdateInfo fetchUpdate() throws IOException, InterruptedException;
}
```

### `UpdateInfo` (Record)
Contains the metadata for an available update.
- `version`: The version string (e.g., "1.2.3").
- `downloadUrl`: Direct link to the JAR file.
- `changelog`: Description of changes (Markdown supported from GitHub).

### `UpdateChannel`
Enum to distinguish between release types:
- `STABLE`: Fetches the `/latest` release from GitHub.
- `NIGHTLY`: Fetches the first item in the release list (includes pre-releases).

---

## Integration Example

### GitHub Integration
To integrate SimpleUpDraft4J using GitHub as the update source:

```java
import de.in.updraft.GithubUpdater;
import de.in.updraft.UpdateChannel;
import de.in.updraft.UpdateInfo;
import de.in.updraft.source.GithubReleaseSource;
import de.in.updraft.ui.SimpleSwingUpdater;

public class MyMainApp {
    public static void main(String[] args) {
        String currentVersion = "1.0.0";
        
        // 1. Initialize Source and Updater
        GithubReleaseSource source = new GithubReleaseSource("owner", "repo", UpdateChannel.STABLE);
        GithubUpdater updater = new GithubUpdater(currentVersion, source);
        
        try {
            // 2. Check for updates
            UpdateInfo info = updater.checkForUpdates();
            
            if (info != null) {
                // 3. Option A: Show Swing Dialog (for Desktop Apps)
                SimpleSwingUpdater ui = new SimpleSwingUpdater(updater);
                ui.showUpdateDialog(info);
                
                // OR Option B: Perform update immediately (Headless)
                // updater.performUpdate(info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Generic JSON Source (NextCloud / Custom Server)
If you host a static `update.json` file:
```java
String url = "https://your-cloud.com/s/abcdef/download/update.json";
JsonMetadataSource source = new JsonMetadataSource(url);
GithubUpdater updater = new GithubUpdater(currentVersion, source);
```
Expected JSON format:
```json
{
  "version": "1.1.0",
  "url": "https://your-cloud.com/s/xyz/download/app.jar",
  "changelog": "Added new features and fixed bugs."
}
```
