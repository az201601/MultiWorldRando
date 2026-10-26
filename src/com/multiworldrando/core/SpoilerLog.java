package com.multiworldrando.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects everything the randomizer changed so it can be written out as a
 * human-readable spoiler log alongside the randomized ROM.
 */
public final class SpoilerLog {

    private final long seed;
    private final String sourceName;
    private final String sourceMd5;
    private final List<String[]> changes = new ArrayList<String[]>();
    private final List<String[]> settings = new ArrayList<String[]>();

    public SpoilerLog(long seed, String sourceName, String sourceMd5) {
        this.seed = seed;
        this.sourceName = sourceName;
        this.sourceMd5 = sourceMd5;
    }

    public void addSetting(String name, String value) {
        settings.add(new String[] { name, value });
    }

    public void addChange(String name, String value) {
        changes.add(new String[] { name, value });
    }

    public void write(File file) throws IOException {
        Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        try {
            w.write(render());
        } finally {
            w.close();
        }
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        String bar = "===============================================";
        sb.append(bar).append('\n');
        sb.append(" Harvest Moon GB Randomizer - Spoiler Log\n");
        sb.append(bar).append("\n\n");
        sb.append("Seed:       ").append(SeedGenerator.format(seed)).append('\n');
        sb.append("Source ROM: ").append(sourceName).append('\n');
        sb.append("Source MD5: ").append(sourceMd5).append("\n\n");

        sb.append("Settings:\n");
        if (settings.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (String[] s : settings) {
                sb.append("  ").append(s[0]).append(": ").append(s[1]).append('\n');
            }
        }

        sb.append("\nChanges:\n");
        if (changes.isEmpty()) {
            sb.append("  (no changes)\n");
        } else {
            for (String[] c : changes) {
                sb.append("  ").append(c[0]).append(": ").append(c[1]).append('\n');
            }
        }
        return sb.toString();
    }
}
