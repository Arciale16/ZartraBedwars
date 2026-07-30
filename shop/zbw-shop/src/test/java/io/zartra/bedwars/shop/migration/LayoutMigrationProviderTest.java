package io.zartra.bedwars.shop.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.migration.MigrationApi;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LayoutMigrationProviderTest {
    private final LayoutMigrationProvider provider = new LayoutMigrationProvider();

    @Test
    void parsesAndMapsValidatedLayoutRecords() throws IOException {
        final String source = "weapons|external-layout-category|display-key=shop.weapons;slot=10\n"
                + "sword|external-layout-item|category=weapons;slot=11;"
                + "material=IRON_SWORD;price=4;action=purchase-item\n"
                + "selector|external-layout-hotbar|slot=0;action=team-selector\n";
        final List<MigrationApi.Record> records =
                new LawfulLayoutSourceAdapter().read(new StringReader(source));

        assertEquals(3, records.size());
        assertEquals(MigrationApi.ConversionState.MAPPED,
                provider.convert(records.get(0)).state());
        assertEquals(MigrationApi.ConversionState.MAPPED,
                provider.convert(records.get(1)).state());
        assertEquals(MigrationApi.ConversionState.MAPPED,
                provider.convert(records.get(2)).state());
    }

    @Test
    void reportsLossyInvalidAndUnsupportedRecordsWithoutExecution() {
        final Map<String, String> attributes = item();
        attributes.put("vendor-metadata", "ignored");
        assertEquals(MigrationApi.ConversionState.LOSSY, provider.convert(
                new MigrationApi.Record("sword", LayoutMigrationProvider.ITEM,
                        attributes)).state());

        attributes.put("price", "-1");
        assertEquals(MigrationApi.ConversionState.UNSUPPORTED, provider.convert(
                new MigrationApi.Record("bad", LayoutMigrationProvider.ITEM,
                        attributes)).state());
        assertEquals(MigrationApi.ConversionState.UNSUPPORTED, provider.convert(
                new MigrationApi.Record("unknown", "unknown-kind",
                        java.util.Collections.<String, String>emptyMap())).state());
    }

    @Test
    void rejectsMalformedAndDuplicateSourceAttributes() {
        final LawfulLayoutSourceAdapter adapter = new LawfulLayoutSourceAdapter();
        assertThrows(IOException.class,
                () -> adapter.read(new StringReader("missing-separators")));
        assertThrows(IOException.class, () -> adapter.read(new StringReader(
                "item|external-layout-item|slot=1;slot=2")));
        assertThrows(IOException.class, () -> adapter.read(new StringReader(
                "item|external-layout-item|broken")));
    }

    private static Map<String, String> item() {
        final Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("category", "weapons");
        attributes.put("slot", "11");
        attributes.put("material", "IRON_SWORD");
        attributes.put("price", "4");
        attributes.put("action", "purchase-item");
        return attributes;
    }
}
