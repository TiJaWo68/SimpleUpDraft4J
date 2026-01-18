package de.in.updraft.source;

import de.in.updraft.UpdateChannel;
import de.in.updraft.UpdateInfo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GithubReleaseSourceTest {

    @Test
    public void testParseOne() throws Exception {
        String json = "{\n" +
                "  \"tag_name\": \"v1.2.3\",\n" +
                "  \"body\": \"New features!\\n- Bugfixes\",\n" +
                "  \"assets\": [\n" +
                "    {\n" +
                "      \"browser_download_url\": \"https://github.com/test/test/releases/download/v1.2.3/test.tar.gz\"\n"
                +
                "    }\n" +
                "  ]\n" +
                "}";

        GithubReleaseSource source = new GithubReleaseSource("owner", "repo", UpdateChannel.STABLE);
        UpdateInfo info = source.parseOne(json);

        assertEquals("v1.2.3", info.version());
        assertEquals("New features!\n- Bugfixes", info.changelog());
        assertEquals("https://github.com/test/test/releases/download/v1.2.3/test.tar.gz", info.downloadUrl());
    }

    @Test
    public void testExtractValue() {
        GithubReleaseSource source = new GithubReleaseSource("owner", "repo", UpdateChannel.STABLE);
        String json = "{\"foo\": \"bar\", \"nested\": {\"key\": \"value\"}}";
        assertEquals("bar", source.extractValue(json, "foo"));
        assertEquals("value", source.extractValue(json, "key"));
    }
}
