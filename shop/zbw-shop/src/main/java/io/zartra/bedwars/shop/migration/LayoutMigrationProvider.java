package io.zartra.bedwars.shop.migration;

import io.zartra.bedwars.api.migration.MigrationApi;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data-only converter for lawfully supplied external shop and hotbar layout projections.
 *
 * <p>ZBW-ADDON-283..290: this adapter validates scalar fields and never executes source actions,
 * opens files or copies source branding.</p>
 */
public final class LayoutMigrationProvider implements MigrationApi.Provider {
    /** Neutral category record kind. */
    public static final String CATEGORY = "external-layout-category";
    /** Neutral item record kind. */
    public static final String ITEM = "external-layout-item";
    /** Neutral hotbar record kind. */
    public static final String HOTBAR = "external-layout-hotbar";

    @Override public String id() { return "zartra:layout-migration"; }

    @Override public boolean supports(final String sourceKind) {
        return CATEGORY.equals(sourceKind) || ITEM.equals(sourceKind)
                || HOTBAR.equals(sourceKind);
    }

    @Override public MigrationApi.Conversion convert(final MigrationApi.Record source) {
        if (!supports(source.kind())) {
            return unsupported("unsupported-kind");
        }
        final Map<String, String> input = source.attributes();
        try {
            final Map<String, String> output = new TreeMap<String, String>();
            final String targetKind;
            if (CATEGORY.equals(source.kind())) {
                output.put("display-key", required(input, "display-key"));
                output.put("slot", slot(input, "slot"));
                targetKind = "native-shop-category";
            } else if (ITEM.equals(source.kind())) {
                output.put("category", required(input, "category"));
                output.put("slot", slot(input, "slot"));
                output.put("material", material(input));
                output.put("price", nonNegative(input, "price"));
                output.put("action", action(input));
                targetKind = "native-shop-item";
            } else {
                output.put("slot", hotbarSlot(input));
                output.put("action", hotbarAction(input));
                targetKind = "native-hotbar-entry";
            }
            final boolean lossy = input.size() > output.size();
            return new MigrationApi.Conversion(
                    lossy ? MigrationApi.ConversionState.LOSSY
                            : MigrationApi.ConversionState.MAPPED,
                    Collections.singletonList(new MigrationApi.Record(
                            "migrated/" + source.id(), targetKind, output)),
                    lossy ? "unmapped-metadata-omitted" : "validated");
        } catch (IllegalArgumentException invalid) {
            return unsupported("invalid-" + invalid.getMessage());
        }
    }

    private static MigrationApi.Conversion unsupported(final String reason) {
        return new MigrationApi.Conversion(MigrationApi.ConversionState.UNSUPPORTED,
                Collections.<MigrationApi.Record>emptyList(), reason);
    }

    private static String required(final Map<String, String> input, final String key) {
        final String value = input.get(key);
        if (value == null || !value.matches("[a-z0-9][a-z0-9_.:/-]{1,127}")) {
            throw new IllegalArgumentException(key);
        }
        return value;
    }

    private static String slot(final Map<String, String> input, final String key) {
        return integer(input, key, 0, 53);
    }

    private static String hotbarSlot(final Map<String, String> input) {
        return integer(input, "slot", 0, 8);
    }

    private static String nonNegative(final Map<String, String> input, final String key) {
        return integer(input, key, 0, 1000000);
    }

    private static String integer(final Map<String, String> input, final String key,
                                  final int minimum, final int maximum) {
        final String value = input.get(key);
        try {
            final int parsed = Integer.parseInt(value);
            if (parsed < minimum || parsed > maximum) {
                throw new IllegalArgumentException(key);
            }
            return Integer.toString(parsed);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException(key);
        }
    }

    private static String material(final Map<String, String> input) {
        final String material = input.get("material");
        if (material == null || !material.matches("[A-Z0-9_]{2,64}")) {
            throw new IllegalArgumentException("material");
        }
        return material;
    }

    private static String action(final Map<String, String> input) {
        final String action = input.get("action");
        if (!"purchase-item".equals(action) && !"open-category".equals(action)
                && !"quick-buy".equals(action)) {
            throw new IllegalArgumentException("action");
        }
        return action;
    }

    private static String hotbarAction(final Map<String, String> input) {
        final String action = input.get("action");
        if (!"join".equals(action) && !"leave".equals(action)
                && !"team-selector".equals(action) && !"spectator".equals(action)) {
            throw new IllegalArgumentException("action");
        }
        return action;
    }
}
