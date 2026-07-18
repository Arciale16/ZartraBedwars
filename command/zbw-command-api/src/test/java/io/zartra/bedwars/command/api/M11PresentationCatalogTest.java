package io.zartra.bedwars.command.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class M11PresentationCatalogTest {
    @Test
    void m11ActionsAreAdditiveUniqueAndMappedToCommandsPermissionsAndPages() {
        final List<PresentationActions.Definition> m11 = PresentationActions.Catalog.m11();
        assertEquals(25, m11.size());
        final Set<PresentationActions.ActionId> ids = new HashSet<PresentationActions.ActionId>();
        for (PresentationActions.Definition definition : m11) {
            assertTrue(ids.add(definition.id()));
            assertTrue(definition.id().value().toString().contains("m11/"));
            assertTrue(definition.commandPath().startsWith("/zbw "));
            assertTrue(definition.pageId().toString().contains("m11/"));
            assertTrue(definition.permission().value().startsWith("zartrabedwars."));
            assertFalseEmpty(definition.requirementIds());
        }
        assertEquals(PresentationActions.Catalog.throughM10().size() + m11.size(),
                PresentationActions.Catalog.throughM11().size());
    }
    private static void assertFalseEmpty(final Set<String> values) { assertTrue(!values.isEmpty()); }
}
