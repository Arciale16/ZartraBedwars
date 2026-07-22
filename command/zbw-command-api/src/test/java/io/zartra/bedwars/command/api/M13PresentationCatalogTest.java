package io.zartra.bedwars.command.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class M13PresentationCatalogTest {
    @Test void catalogHasExactPermissionAndConfirmationSurface() {
        final List<PresentationActions.Definition> definitions = PresentationActions.Catalog.m13();
        assertEquals(17, definitions.size());
        final Set<String> permissions = new HashSet<String>();
        int mutations = 0;
        for (PresentationActions.Definition definition : definitions) {
            permissions.add(definition.permission().toString());
            if (definition.destructive()) { mutations++; }
            assertTrue(definition.commandPath().startsWith("/zbw "));
            assertTrue(definition.pageId().toString().startsWith("zartra:m13/"));
        }
        assertEquals(12, permissions.size());
        assertEquals(6, mutations);
        assertEquals(PresentationActions.Catalog.throughM12().size() + 17,
                PresentationActions.Catalog.throughM13().size());
    }
}
