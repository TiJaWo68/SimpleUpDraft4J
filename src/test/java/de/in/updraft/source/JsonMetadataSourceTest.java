package de.in.updraft.source;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link JsonMetadataSource}.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class JsonMetadataSourceTest {

    @Test
    public void testExtractValue() {
        JsonMetadataSource source = new JsonMetadataSource("http://example.com/update.json");
        String json = "{\"version\": \"2.0.0\", \"url\": \"http://dl.com/app.jar\", \"changelog\": \"Update!\"}";

        assertEquals("2.0.0", source.extractValue(json, "version"));
        assertEquals("http://dl.com/app.jar", source.extractValue(json, "url"));
        assertEquals("Update!", source.extractValue(json, "changelog"));
    }
}
