package io.zartra.bedwars.command.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class M12PresentationCatalogTest {
    @Test
    void m12ActionsAreCompleteUniqueAndUseCentralPermissions() {
        final List<PresentationActions.Definition> definitions =
                PresentationActions.Catalog.m12();
        assertEquals(17, definitions.size());
        final Set<PresentationActions.ActionId> ids = new HashSet<>();
        int destructive = 0;
        for (PresentationActions.Definition definition : definitions) {
            assertTrue(ids.add(definition.id()));
            assertTrue(definition.id().toString().contains("m12/"));
            assertTrue(definition.commandPath().startsWith("/zbw "));
            assertTrue(definition.pageId().toString().contains("m12/"));
            assertTrue(definition.permission().value().startsWith("zartrabedwars."));
            assertTrue(!definition.requirementIds().isEmpty());
            if (definition.destructive()) { destructive++; }
        }
        assertEquals(5, destructive);
        assertEquals(PresentationActions.Catalog.throughM11().size() + definitions.size(),
                PresentationActions.Catalog.throughM12().size());
    }
}
