package io.zartra.bedwars.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.CaseId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.ContentPackId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.ExtensionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.GuiPageId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.IdentifierFormatException;
import io.zartra.bedwars.api.identity.MapId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.PrivateGameModifierId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.identity.ReplayId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.api.version.ApiVersions;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.api.version.VersionRange;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityAndVersionTest {
    private static final String UUID_TEXT = "123e4567-e89b-12d3-a456-426614174000";

    @Test
    void uuidIdentifiersRoundTripAndRemainTypeSafe() {
        final UUID uuid = UUID.fromString(UUID_TEXT);
        assertEquals(UUID_TEXT, ArenaId.parse(UUID_TEXT).toString());
        assertEquals(uuid, MapId.of(uuid).asUuid());
        assertEquals(MatchId.parse(UUID_TEXT), MatchId.of(uuid));
        assertEquals(ReplayId.parse(UUID_TEXT).hashCode(), ReplayId.of(uuid).hashCode());
        assertEquals(CaseId.parse(UUID_TEXT), CaseId.of(uuid));
        assertEquals(EventId.parse(UUID_TEXT), EventId.of(uuid));
        assertEquals(CorrelationId.parse(UUID_TEXT), CorrelationId.of(uuid));
        assertEquals(PlayerId.parse(UUID_TEXT), PlayerId.of(uuid));
        assertNotEquals(ArenaId.of(uuid), MapId.of(uuid));
        assertNotEquals(ArenaId.random(), ArenaId.random());
        assertThrows(IdentifierFormatException.class, () -> ArenaId.parse(null));
        assertThrows(IdentifierFormatException.class, () -> ArenaId.parse("not-a-uuid"));
        assertThrows(NullPointerException.class, () -> ArenaId.of(null));
    }

    @Test
    void namespacedIdentifiersRoundTripAndRejectMalformedValues() {
        assertEquals("demo:item/path", DefinitionId.parse("demo:item/path").toString());
        assertEquals(ExtensionId.of("demo", "extension"), ExtensionId.parse("demo:extension"));
        assertEquals("demo:gui", GuiPageId.parse("demo:gui").toString());
        assertEquals("demo:cap", CapabilityId.parse("demo:cap").toString());
        assertEquals("demo:generator", GeneratorTypeId.parse("demo:generator").toString());
        assertEquals("demo:modifier", PrivateGameModifierId.parse("demo:modifier").toString());
        assertEquals("demo:provider", ProviderId.parse("demo:provider").toString());
        assertEquals("demo:event", EventTypeId.parse("demo:event").toString());
        assertEquals("demo:resource", ResourceId.parse("demo:resource").toString());
        assertEquals("demo:pack", ContentPackId.parse("demo:pack").toString());
        assertEquals("demo:operation-1", IdempotencyKey.parse("demo:operation-1").toString());
        assertThrows(IdentifierFormatException.class, () -> DefinitionId.parse("missing-colon"));
        assertThrows(IdentifierFormatException.class, () -> DefinitionId.parse("UPPER:path"));
        assertThrows(IdentifierFormatException.class, () -> DefinitionId.parse("a:b:c"));
        assertNotEquals(DefinitionId.parse("demo:same"), CapabilityId.parse("demo:same"));
    }

    @Test
    void semanticVersionImplementsPrecedenceAndCanonicalRanges() {
        final SemanticVersion release = SemanticVersion.parse("1.2.3+build.7");
        assertEquals(1, release.major());
        assertEquals("build.7", release.build());
        assertFalse(release.isPreRelease());
        assertTrue(SemanticVersion.parse("1.2.3-alpha.1").compareTo(release) < 0);
        assertTrue(SemanticVersion.parse("1.2.3-alpha.2").compareTo(SemanticVersion.parse("1.2.3-alpha.10")) < 0);
        assertThrows(SemanticVersion.VersionFormatException.class, () -> SemanticVersion.parse("1.02.3"));
        assertThrows(SemanticVersion.VersionFormatException.class, () -> SemanticVersion.parse("1.0.0-01"));
        final VersionRange range = VersionRange.parse("[1.0.0,2.0.0)");
        assertTrue(range.contains(SemanticVersion.parse("1.9.9")));
        assertFalse(range.contains(SemanticVersion.parse("2.0.0")));
        assertEquals("[1.0.0,2.0.0)", range.toString());
        assertTrue(ApiVersions.supports(ApiVersions.CURRENT));
        assertFalse(ApiVersions.supports(SemanticVersion.parse("2.0.0")));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.between(
                SemanticVersion.parse("2.0.0"), SemanticVersion.parse("1.0.0")));
    }
}
