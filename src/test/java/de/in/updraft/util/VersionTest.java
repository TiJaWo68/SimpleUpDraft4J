package de.in.updraft.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Version}.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class VersionTest {

    @Test
    public void testComparison() {
        Version v2 = new Version("1.0.1");
        Version v3 = new Version("1.1.0");
        Version v4 = new Version("2.0.0");

        assertTrue(v2.isNewerThan("1.0.0"));
        assertTrue(v3.isNewerThan("1.0.1"));
        assertTrue(v4.isNewerThan("1.9.9"));
    }

    @Test
    public void testPrefixV() {
        Version v1 = new Version("v1.2.3");
        assertEquals("1.2.3", v1.toString());
    }

    @Test
    public void testSuffix() {
        Version v2 = new Version("1.0.0");
        assertTrue(v2.isNewerThan("1.0.0-beta"));
    }
}
