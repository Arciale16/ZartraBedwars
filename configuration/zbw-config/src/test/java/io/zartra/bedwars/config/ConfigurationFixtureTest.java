package io.zartra.bedwars.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.configuration.ConfigurationKey;
import io.zartra.bedwars.api.configuration.ConfigurationVersion;
import io.zartra.bedwars.config.schema.ConfigurationModel.Document;
import io.zartra.bedwars.config.schema.ConfigurationModel.InitialCatalog;
import io.zartra.bedwars.config.schema.ConfigurationModel.LogicalFile;
import io.zartra.bedwars.config.schema.ConfigurationModel.Validator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigurationFixtureTest {
    private final Validator validator = new Validator();

    @Test void validFixturePassesStrictMessagesSchema() throws IOException {
        assertTrue(validator.validate(InitialCatalog.schema(LogicalFile.MESSAGES),
                fixture("valid/messages-v1.conf")).isValid());
    }

    @Test void invalidFixturesExposeUnknownAndDependencyFailures() throws IOException {
        assertFalse(validator.validate(InitialCatalog.schema(LogicalFile.MESSAGES),
                fixture("invalid/unknown-key.conf")).isValid());
        assertEquals("zartra:config/missing_dependency", validator.validate(
                InitialCatalog.schema(LogicalFile.DATABASE),
                fixture("invalid/missing-dependency.conf")).issues().get(0).code().toString());
    }

    @Test void migrationAndRollbackFixturesRemainStableInputs() throws IOException {
        final Document migration = fixture("migration/config-v1.conf");
        final Document rollback = fixture("rollback/messages-v1.conf");
        assertEquals(ConfigurationVersion.of(1), migration.version());
        assertEquals("preserved", migration.values().get(ConfigurationKey.of("legacy.option")));
        assertEquals("it-IT", rollback.values().get(ConfigurationKey.of("catalog.default-locale")));
    }

    private static Document fixture(final String name) throws IOException {
        final String path = "/configuration/" + name;
        final InputStream stream = ConfigurationFixtureTest.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalArgumentException("Missing fixture: " + path);
        }
        int version = 0;
        final Map<ConfigurationKey, String> values = new LinkedHashMap<ConfigurationKey, String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final int separator = line.indexOf('=');
                if (separator < 1) {
                    throw new IllegalArgumentException("Malformed fixture line");
                }
                final String key = line.substring(0, separator);
                final String value = line.substring(separator + 1);
                if ("version".equals(key)) {
                    version = Integer.parseInt(value);
                } else {
                    values.put(ConfigurationKey.of(key), value);
                }
            }
        }
        return Document.of(ConfigurationVersion.of(version), values);
    }
}
