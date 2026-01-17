package de.in.updraft.util;

/**
 * Utility for SemVer comparison.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class Version implements Comparable<Version> {
    private final int[] parts;
    private final String suffix;

    public Version(String version) {
        if (version == null)
            throw new IllegalArgumentException("Version cannot be null");

        // Remove 'v' prefix if present
        if (version.startsWith("v")) {
            version = version.substring(1);
        }

        String[] mainParts = version.split("-", 2);
        String[] numericParts = mainParts[0].split("\\.");

        this.parts = new int[numericParts.length];
        for (int i = 0; i < numericParts.length; i++) {
            try {
                this.parts[i] = Integer.parseInt(numericParts[i]);
            } catch (NumberFormatException e) {
                this.parts[i] = 0;
            }
        }

        this.suffix = mainParts.length > 1 ? mainParts[1] : "";
    }

    @Override
    public int compareTo(Version o) {
        int length = Math.max(this.parts.length, o.parts.length);
        for (int i = 0; i < length; i++) {
            int thisPart = i < this.parts.length ? this.parts[i] : 0;
            int thatPart = i < o.parts.length ? o.parts[i] : 0;
            if (thisPart < thatPart)
                return -1;
            if (thisPart > thatPart)
                return 1;
        }

        // Suffix comparison (simplified: actual version > pre-release version)
        if (this.suffix.isEmpty() && !o.suffix.isEmpty())
            return 1;
        if (!this.suffix.isEmpty() && o.suffix.isEmpty())
            return -1;

        return this.suffix.compareTo(o.suffix);
    }

    public boolean isNewerThan(String other) {
        return compareTo(new Version(other)) > 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]);
            if (i < parts.length - 1)
                sb.append(".");
        }
        if (!suffix.isEmpty()) {
            sb.append("-").append(suffix);
        }
        return sb.toString();
    }
}
