package io.zartra.bedwars.shop.migration;

import io.zartra.bedwars.api.migration.MigrationApi;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Bounded parser for an operator-authorized neutral layout export.
 *
 * <p>The format is one record per line:
 * {@code id|kind|key=value;key=value}. Blank lines and lines beginning with {@code #} are ignored.
 * The adapter receives a reader and therefore cannot escape an operator-confined source path.</p>
 */
public final class LawfulLayoutSourceAdapter {
    /** Maximum accepted source characters. */
    public static final int MAX_CHARACTERS = 1048576;
    /** Maximum accepted records. */
    public static final int MAX_RECORDS = 10000;

    /** Parses and validates the neutral export without executing any content. */
    public List<MigrationApi.Record> read(final Reader source) throws IOException {
        final BufferedReader reader = new BufferedReader(Objects.requireNonNull(source, "source"));
        final List<MigrationApi.Record> records = new ArrayList<MigrationApi.Record>();
        int characters = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            characters += line.length() + 1;
            if (characters > MAX_CHARACTERS) {
                throw new IOException("layout source exceeds one MiB");
            }
            final String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            records.add(parse(trimmed));
            if (records.size() > MAX_RECORDS) {
                throw new IOException("layout source exceeds record limit");
            }
        }
        return java.util.Collections.unmodifiableList(records);
    }

    private static MigrationApi.Record parse(final String line) throws IOException {
        final String[] parts = line.split("\\|", -1);
        if (parts.length != 3) {
            throw new IOException("invalid layout record");
        }
        final Map<String, String> attributes = new TreeMap<String, String>();
        if (!parts[2].isEmpty()) {
            for (String attribute : parts[2].split(";")) {
                final int separator = attribute.indexOf('=');
                if (separator < 1 || separator == attribute.length() - 1) {
                    throw new IOException("invalid layout attribute");
                }
                final String key = attribute.substring(0, separator).trim();
                final String value = attribute.substring(separator + 1).trim();
                if (attributes.put(key, value) != null) {
                    throw new IOException("duplicate layout attribute");
                }
            }
        }
        try {
            return new MigrationApi.Record(parts[0].trim(), parts[1].trim(), attributes);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("invalid layout record", invalid);
        }
    }
}
