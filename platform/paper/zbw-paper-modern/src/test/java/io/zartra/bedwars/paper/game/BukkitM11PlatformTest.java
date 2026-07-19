package io.zartra.bedwars.paper.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.shop.api.PurchaseContext;
import io.zartra.bedwars.shop.api.ShopCatalog;
import io.zartra.bedwars.shop.generator.GeneratorBatch;
import io.zartra.bedwars.shop.item.ItemActionRequest;
import io.zartra.bedwars.shop.item.UtilityItemDefinition;
import io.zartra.bedwars.shop.upgrade.TeamEffectIntent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

final class BukkitM11PlatformTest {
    @Test
    void validatesConstructionNullsBoundsAndRetryableDeliveries() {
        final Mapping mapping = new Mapping();
        assertThrows(NullPointerException.class, () -> new BukkitM11Platform(null));
        final BukkitM11Platform platform = new BukkitM11Platform(mapping);
        assertThrows(NullPointerException.class, () -> platform.deliver(null));
        assertThrows(IllegalArgumentException.class, () -> platform.deliver(batch("large", 4097)));
        assertThrows(IllegalArgumentException.class, () -> platform.deliver(batch("large", 4097)));
        assertEquals(0, mapping.generatorCalls);
        assertThrows(IllegalArgumentException.class,
                () -> platform.deliver(batch("missing-location", 1)));
        platform.deliver(batch("missing-location", 1));
        assertEquals(1, mapping.generatorCalls);
        mapping.location = Optional.of(new Object());
        assertThrows(IllegalArgumentException.class,
                () -> platform.deliver(batch("missing-resource", 1)));
        mapping.material = Optional.of("bad material");
        assertThrows(IllegalArgumentException.class,
                () -> platform.deliver(batch("invalid-material", 1)));
    }

    @Test
    void appliesTeamTargetsExactlyOnceAndCleansEmptyOwnership() {
        final Mapping mapping = new Mapping();
        final BukkitM11Platform platform = new BukkitM11Platform(mapping);
        assertThrows(NullPointerException.class, () -> platform.apply((TeamEffectIntent) null));
        platform.apply(effect("team", null));
        platform.apply(effect("team", null));
        assertEquals(1, mapping.teamCalls);
        platform.apply(effect("target", player()));
        platform.apply(effect("target", player()));
        assertEquals(1, mapping.playerCalls);
        assertThrows(NullPointerException.class, () -> platform.clear(null));
        platform.clear(player());
        assertEquals(1, mapping.clearCalls);
        assertThrows(NullPointerException.class, () -> platform.clearMatch(null));
        platform.clearMatch(match());
    }

    @Test
    void utilityProjectionRetriesFailuresAndDeduplicatesSuccess() {
        final Mapping mapping = new Mapping();
        final BukkitM11Platform platform = new BukkitM11Platform(mapping);
        final DefinitionId effect = DefinitionId.of("test", "effect/utility");
        assertThrows(NullPointerException.class,
                () -> platform.apply(null, definition(), request("null-effect")));
        assertThrows(NullPointerException.class,
                () -> platform.apply(effect, null, request("null-definition")));
        assertThrows(NullPointerException.class,
                () -> platform.apply(effect, definition(), null));
        assertFalse(platform.apply(effect, definition(), request("missing-player")));
        assertFalse(platform.apply(effect, definition(), request("missing-player")));
        assertEquals(2, mapping.playerCalls);
        mapping.player = Optional.of(new Actor());
        mapping.locations = Collections.nCopies(4097, new Object());
        assertFalse(platform.apply(effect, definition(), request("bounded")));
        mapping.locations = Collections.singletonList(new Object());
        assertFalse(platform.apply(effect, definition(), request("missing-material")));
        mapping.locations = Collections.emptyList();
        assertTrue(platform.apply(effect, definition(), request("success")));
        assertTrue(platform.apply(effect, definition(), request("success")));
    }

    @Test
    void validatesEffectMappingsAtEveryBoundary() {
        final BukkitM11Platform.EffectMapping empty = mapping(
                Optional.empty(), Optional.empty(), 0F, 0.5F, 0);
        assertFalse(empty.sound().isPresent());
        assertFalse(empty.particle().isPresent());
        assertEquals(0F, empty.volume());
        assertEquals(0.5F, empty.pitch());
        assertEquals(0, empty.particleCount());
        final BukkitM11Platform.EffectMapping full = mapping(
                Optional.of("BLOCK_NOTE_BLOCK_PLING"), Optional.of("HAPPY_VILLAGER"),
                4F, 2F, 512);
        assertEquals("BLOCK_NOTE_BLOCK_PLING", full.sound().get());
        assertEquals("HAPPY_VILLAGER", full.particle().get());
        assertThrows(NullPointerException.class,
                () -> mapping(null, Optional.empty(), 1F, 1F, 0));
        assertThrows(NullPointerException.class,
                () -> mapping(Optional.empty(), null, 1F, 1F, 0));
        invalid(Optional.of("invalid"), Optional.empty(), 1F, 1F, 0);
        invalid(Optional.empty(), Optional.of("invalid"), 1F, 1F, 0);
        invalid(Optional.empty(), Optional.empty(), -1F, 1F, 0);
        invalid(Optional.empty(), Optional.empty(), 5F, 1F, 0);
        invalid(Optional.empty(), Optional.empty(), Float.NaN, 1F, 0);
        invalid(Optional.empty(), Optional.empty(), 1F, 0.4F, 0);
        invalid(Optional.empty(), Optional.empty(), 1F, 2.1F, 0);
        invalid(Optional.empty(), Optional.empty(), 1F, Float.NaN, 0);
        invalid(Optional.empty(), Optional.empty(), 1F, 1F, -1);
        invalid(Optional.empty(), Optional.empty(), 1F, 1F, 513);
    }

    @Test
    void projectsFeedbackEvictsKeysAndRestoresOwnedCleanup() throws ReflectiveOperationException {
        final Actor actor = new Actor();
        final Mapping mapping = new Mapping();
        mapping.player = Optional.of(actor);
        mapping.feedback = new BukkitM11Platform.EffectMapping(
                Optional.empty(), Optional.empty(), 1F, 1F, 0);
        final BukkitM11Platform platform = new BukkitM11Platform(mapping);
        platform.apply(new TeamEffectIntent(
                IdempotencyKey.of("test", "forge"), TeamEffectIntent.Kind.FORGE_RESOURCES,
                DefinitionId.of("test", "team/red"), DefinitionId.of("test", "effect/forge"),
                null, Collections.singletonMap(ResourceId.of("minecraft", "iron_ingot"), 1)));
        assertTrue(platform.apply(DefinitionId.of("test", "effect/feedback"), definition(),
                request("feedback")));

        final RestorableBlock block = new RestorableBlock();
        final Object snapshot = snapshot(block);
        blocks(platform, "playerBlocks").put(player(), Collections.singletonList(snapshot));
        blocks(platform, "matchBlocks").put(match(), Collections.singletonList(snapshot));
        final Entity valid = new Entity(true);
        final Entity invalid = new Entity(false);
        entities(platform).put(match(), java.util.Arrays.asList(valid, invalid));
        platform.clear(player());
        platform.clearMatch(match());
        assertEquals(2, block.restores);
        assertEquals(1, valid.removals);
        assertEquals(0, invalid.removals);

        final Set<IdempotencyKey> completed = completed(platform);
        completed.clear();
        for (int index = 0; index < 8192; index++) {
            completed.add(IdempotencyKey.of("test", "retained-" + index));
        }
        assertTrue(platform.apply(DefinitionId.of("test", "effect/evict"), definition(),
                request("evict")));
        assertEquals(8192, completed.size());
    }

    private static void invalid(final Optional<String> sound, final Optional<String> particle,
                                final float volume, final float pitch, final int count) {
        assertThrows(IllegalArgumentException.class,
                () -> mapping(sound, particle, volume, pitch, count));
    }

    private static BukkitM11Platform.EffectMapping mapping(final Optional<String> sound,
            final Optional<String> particle, final float volume, final float pitch,
            final int count) {
        return new BukkitM11Platform.EffectMapping(sound, particle, volume, pitch, count);
    }

    static GeneratorBatch batch(final String key, final int amount) {
        try {
            final Constructor<GeneratorBatch> constructor = GeneratorBatch.class
                    .getDeclaredConstructor(IdempotencyKey.class, MatchId.class,
                            DefinitionId.class, ResourceId.class, long.class, int.class,
                            Instant.class);
            constructor.setAccessible(true);
            return constructor.newInstance(IdempotencyKey.of("test", key), match(),
                    DefinitionId.of("test", "generator/iron"),
                    ResourceId.of("minecraft", "iron_ingot"), 1L, amount, Instant.EPOCH);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    static TeamEffectIntent effect(final String key, final PlayerId target) {
        return new TeamEffectIntent(IdempotencyKey.of("test", key),
                TeamEffectIntent.Kind.HEAL_POOL, DefinitionId.of("test", "team/red"),
                DefinitionId.of("test", "effect/heal"), target, Collections.emptyMap());
    }

    static UtilityItemDefinition definition() {
        return new UtilityItemDefinition(DefinitionId.of("test", "utility/tower"),
                DefinitionId.of("test", "item/tower"), UtilityItemDefinition.Kind.POPUP_TOWER,
                PermissionNode.of("zartrabedwars.item.popup_tower"),
                new ShopCatalog.Price(Collections.singletonList(new ShopCatalog.ResourceAmount(
                        ResourceId.of("minecraft", "iron_ingot"), 24))), Duration.ZERO,
                UtilityItemDefinition.TargetRule.LOCATION, 4, Collections.emptyMap());
    }

    static ItemActionRequest request(final String key) {
        return new ItemActionRequest(context(), definition().id(), IdempotencyKey.of("test", key),
                Instant.EPOCH, Optional.empty());
    }

    private static PurchaseContext context() {
        return new PurchaseContext(AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER,
                DefinitionId.of("test", "player/one")), player(), match(),
                ArenaId.of(new UUID(3L, 3L)), DefinitionId.of("test", "mode/standard"),
                DefinitionId.of("test", "team/red"), Optional.empty());
    }

    private static PlayerId player() { return PlayerId.of(new UUID(1L, 1L)); }
    private static MatchId match() { return MatchId.of(new UUID(2L, 2L)); }

    private static Object snapshot(final RestorableBlock block) throws ReflectiveOperationException {
        final Class<?> type = Class.forName(BukkitM11Platform.class.getName() + "$BlockSnapshot");
        final Constructor<?> constructor = type.getDeclaredConstructor(Object.class, Object.class);
        constructor.setAccessible(true);
        return constructor.newInstance(block, null);
    }

    @SuppressWarnings("unchecked")
    private static <K> Map<K, List<Object>> blocks(final BukkitM11Platform platform,
                                                   final String name)
            throws ReflectiveOperationException {
        final Field field = BukkitM11Platform.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<K, List<Object>>) field.get(platform);
    }

    @SuppressWarnings("unchecked")
    private static Map<MatchId, List<Object>> entities(final BukkitM11Platform platform)
            throws ReflectiveOperationException {
        final Field field = BukkitM11Platform.class.getDeclaredField("matchEntities");
        field.setAccessible(true);
        return (Map<MatchId, List<Object>>) field.get(platform);
    }

    @SuppressWarnings("unchecked")
    private static Set<IdempotencyKey> completed(final BukkitM11Platform platform)
            throws ReflectiveOperationException {
        final Field field = BukkitM11Platform.class.getDeclaredField("completed");
        field.setAccessible(true);
        return (Set<IdempotencyKey>) field.get(platform);
    }

    public static final class Actor {
        public Object getLocation() { return new Object(); }
    }

    public static final class RestorableBlock {
        private int restores;
        public void setBlockData(final BlockData data, final boolean physics) { restores++; }
    }

    public static final class Entity {
        private final boolean valid;
        private int removals;
        Entity(final boolean valid) { this.valid = valid; }
        public boolean isValid() { return valid; }
        public void remove() { removals++; }
    }

    private static final class Mapping implements BukkitM11Platform.Mappings {
        private Optional<Object> location = Optional.empty();
        private Optional<String> material = Optional.empty();
        private Optional<Object> player = Optional.empty();
        private List<Object> locations = Collections.emptyList();
        private BukkitM11Platform.EffectMapping feedback;
        private int generatorCalls;
        private int playerCalls;
        private int teamCalls;
        private int clearCalls;
        @Override public Optional<Object> generatorLocation(final DefinitionId id) {
            generatorCalls++;
            return location;
        }
        @Override public Optional<String> material(final ResourceId id) { return material; }
        @Override public Optional<Object> player(final PlayerId id) {
            playerCalls++;
            return player;
        }
        @Override public Collection<Object> teamPlayers(final DefinitionId teamId) {
            teamCalls++;
            return Collections.emptyList();
        }
        @Override public MatchId matchOf(final Object ignored) { return match(); }
        @Override public List<Object> utilityBlocks(final DefinitionId effect,
                final UtilityItemDefinition definition, final ItemActionRequest request) {
            return locations;
        }
        @Override public Optional<String> utilityMaterial(final DefinitionId effect,
                final UtilityItemDefinition definition) { return Optional.empty(); }
        @Override public BukkitM11Platform.EffectMapping effect(final DefinitionId id) {
            return feedback;
        }
        @Override public void clearOwnedEffects(final PlayerId playerId) { clearCalls++; }
    }
}
