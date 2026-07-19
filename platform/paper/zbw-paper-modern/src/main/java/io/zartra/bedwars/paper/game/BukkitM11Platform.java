package io.zartra.bedwars.paper.game;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.shop.generator.GeneratorBatch;
import io.zartra.bedwars.shop.item.ItemActionRequest;
import io.zartra.bedwars.shop.item.UtilityItemDefinition;
import io.zartra.bedwars.shop.upgrade.TeamEffectIntent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Exact Paper 1.21.1 mutation adapter for already validated M11 intents.
 *
 * <p>The approved Paper mirror is intentionally non-transitive, so Bukkit values remain opaque
 * and are invoked through the same exact-version reflection boundary as the established M08
 * projections. All semantic mappings come from validated configuration; this class only performs
 * owner-thread translation, exactly-once projection and ownership cleanup.</p>
 */
public final class BukkitM11Platform implements M11PaperProjection.Platform {
    private static final int MAX_PROJECTED_UNITS = 4096;
    private static final int MAX_KEYS = 8192;
    private final Mappings mappings;
    private final Set<IdempotencyKey> completed = new LinkedHashSet<>();
    private final Map<MatchId, List<Object>> matchEntities = new LinkedHashMap<>();
    private final Map<PlayerId, List<BlockSnapshot>> playerBlocks = new LinkedHashMap<>();
    private final Map<MatchId, List<BlockSnapshot>> matchBlocks = new LinkedHashMap<>();

    /** Creates an exact adapter from a complete semantic mapping provider. */
    public BukkitM11Platform(final Mappings mappings) {
        this.mappings = Objects.requireNonNull(mappings, "mappings");
    }

    @Override public void deliver(final GeneratorBatch batch) {
        Objects.requireNonNull(batch, "batch");
        if (!begin(batch.key())) { return; }
        if (batch.amount() > MAX_PROJECTED_UNITS) {
            completed.remove(batch.key());
            throw new IllegalArgumentException("generator batch exceeds Paper projection bound");
        }
        final Object location = mappings.generatorLocation(batch.generatorId()).orElseThrow(
                () -> new IllegalArgumentException("unmapped generator location"));
        final Object material = material(mappings.material(batch.resource()).orElseThrow(
                () -> new IllegalArgumentException("unmapped generator resource")));
        final Object world = PaperReflection.invoke(location, "getWorld", new Class<?>[0]);
        final List<Object> created = matchEntities.computeIfAbsent(batch.matchId(),
                ignored -> new ArrayList<>());
        int remaining = batch.amount();
        while (remaining > 0) {
            final int amount = Math.min(remaining, 64);
            final Object stack = PaperReflection.construct(PaperReflection.ITEM_STACK,
                    new Class<?>[] {PaperReflection.MATERIAL, int.class}, material, amount);
            final Object dropped = PaperReflection.invoke(world, "dropItemNaturally",
                    new Class<?>[] {PaperReflection.LOCATION, PaperReflection.ITEM_STACK},
                    location, stack);
            created.add(dropped);
            remaining -= amount;
        }
    }

    @Override public void apply(final TeamEffectIntent intent) {
        Objects.requireNonNull(intent, "intent");
        if (!begin(intent.key())) { return; }
        final Collection<Object> targets = targets(intent);
        if (intent.kind() == TeamEffectIntent.Kind.FORGE_RESOURCES) {
            for (Object player : targets) {
                for (Map.Entry<ResourceId, Integer> resource : intent.resources().entrySet()) {
                    grant(player, mappings.material(resource.getKey()).orElseThrow(
                            () -> new IllegalArgumentException("unmapped forge resource")),
                            resource.getValue(), mappings.matchOf(player));
                }
            }
        }
        for (Object player : targets) { applyEffect(player, mappings.effect(intent.effect())); }
    }

    @Override public boolean apply(final DefinitionId effect,
                                   final UtilityItemDefinition definition,
                                   final ItemActionRequest request) {
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(request, "request");
        if (!begin(request.key())) { return true; }
        final Object actor = mappings.player(request.context().playerId()).orElse(null);
        if (actor == null) {
            completed.remove(request.key());
            return false;
        }
        final List<Object> locations = mappings.utilityBlocks(effect, definition, request);
        if (locations.size() > MAX_PROJECTED_UNITS) {
            completed.remove(request.key());
            return false;
        }
        if (!locations.isEmpty()) {
            final Optional<String> configured = mappings.utilityMaterial(effect, definition);
            if (!configured.isPresent()) {
                completed.remove(request.key());
                return false;
            }
            final Object material = material(configured.get());
            final List<BlockSnapshot> owned = playerBlocks.computeIfAbsent(
                    request.context().playerId(), ignored -> new ArrayList<>());
            final List<BlockSnapshot> matchOwned = matchBlocks.computeIfAbsent(
                    request.context().matchId(), ignored -> new ArrayList<>());
            for (Object location : locations) {
                final Object block = PaperReflection.invoke(location, "getBlock", new Class<?>[0]);
                final Object original = PaperReflection.invoke(block, "getBlockData", new Class<?>[0]);
                PaperReflection.invoke(block, "setType",
                        new Class<?>[] {PaperReflection.MATERIAL, boolean.class}, material, false);
                final BlockSnapshot snapshot = new BlockSnapshot(block, original);
                owned.add(snapshot);
                matchOwned.add(snapshot);
            }
        }
        applyEffect(actor, mappings.effect(effect));
        return true;
    }

    @Override public void clear(final PlayerId playerId) {
        restore(playerBlocks.remove(Objects.requireNonNull(playerId, "playerId")));
        mappings.clearOwnedEffects(playerId);
    }

    @Override public void clearMatch(final MatchId matchId) {
        Objects.requireNonNull(matchId, "matchId");
        final List<Object> entities = matchEntities.remove(matchId);
        if (entities != null) {
            for (Object entity : entities) {
                final boolean valid = (Boolean) PaperReflection.invoke(entity, "isValid",
                        new Class<?>[0]);
                if (valid) { PaperReflection.invoke(entity, "remove", new Class<?>[0]); }
            }
        }
        restore(matchBlocks.remove(matchId));
    }

    private void grant(final Object player, final String materialName, final int amount,
                       final MatchId matchId) {
        final Object inventory = PaperReflection.invoke(player, "getInventory", new Class<?>[0]);
        int remaining = amount;
        while (remaining > 0) {
            final int stackAmount = Math.min(remaining, 64);
            final Object stack = PaperReflection.construct(PaperReflection.ITEM_STACK,
                    new Class<?>[] {PaperReflection.MATERIAL, int.class},
                    material(materialName), stackAmount);
            final Object array = java.lang.reflect.Array.newInstance(PaperReflection.ITEM_STACK, 1);
            java.lang.reflect.Array.set(array, 0, stack);
            @SuppressWarnings("unchecked")
            final Map<Integer, Object> overflow = (Map<Integer, Object>) PaperReflection.invoke(
                    inventory, "addItem", new Class<?>[] {array.getClass()}, array);
            for (Object rejected : overflow.values()) {
                final Object location = PaperReflection.invoke(player, "getLocation", new Class<?>[0]);
                final Object world = PaperReflection.invoke(player, "getWorld", new Class<?>[0]);
                final Object dropped = PaperReflection.invoke(world, "dropItemNaturally",
                        new Class<?>[] {PaperReflection.LOCATION, PaperReflection.ITEM_STACK},
                        location, rejected);
                matchEntities.computeIfAbsent(matchId, ignored -> new ArrayList<>()).add(dropped);
            }
            remaining -= stackAmount;
        }
    }

    private Collection<Object> targets(final TeamEffectIntent intent) {
        if (intent.target().isPresent()) {
            return mappings.player(intent.target().get()).<Collection<Object>>map(
                    Collections::singletonList).orElseGet(Collections::emptyList);
        }
        return mappings.teamPlayers(intent.teamId());
    }

    private static void applyEffect(final Object player, final EffectMapping effect) {
        if (effect == null) { return; }
        final Object location = PaperReflection.invoke(player, "getLocation", new Class<?>[0]);
        if (effect.sound().isPresent()) {
            PaperReflection.invoke(player, "playSound",
                    new Class<?>[] {PaperReflection.LOCATION, String.class, float.class, float.class},
                    location, effect.sound().get(), effect.volume(), effect.pitch());
        }
        if (effect.particle().isPresent() && effect.particleCount() > 0) {
            final Object particle = PaperReflection.constant(
                    PaperReflection.type("org.bukkit.Particle"), effect.particle().get());
            PaperReflection.invoke(player, "spawnParticle",
                    new Class<?>[] {PaperReflection.type("org.bukkit.Particle"),
                            PaperReflection.LOCATION, int.class},
                    particle, location, effect.particleCount());
        }
    }

    private static Object material(final String name) {
        if (name == null || !name.matches("[A-Z][A-Z0-9_]{1,63}")) {
            throw new IllegalArgumentException("invalid Paper material mapping");
        }
        return PaperReflection.constant(PaperReflection.MATERIAL, name);
    }

    private boolean begin(final IdempotencyKey key) {
        if (!completed.add(key)) { return false; }
        if (completed.size() > MAX_KEYS) {
            final Iterator<IdempotencyKey> iterator = completed.iterator();
            iterator.next();
            iterator.remove();
        }
        return true;
    }

    private static void restore(final List<BlockSnapshot> blocks) {
        if (blocks == null) { return; }
        for (int index = blocks.size() - 1; index >= 0; index--) { blocks.get(index).restore(); }
    }

    /** Validated configuration and live-player lookup boundary. Bukkit objects remain opaque. */
    public interface Mappings {
        /** Resolves one configured Bukkit Location. */ Optional<Object> generatorLocation(DefinitionId id);
        /** Resolves a native/custom resource to a Paper Material enum name. */ Optional<String> material(ResourceId id);
        /** Resolves one online Bukkit Player. */ Optional<Object> player(PlayerId id);
        /** Resolves current online Bukkit Players on a team. */ Collection<Object> teamPlayers(DefinitionId teamId);
        /** Resolves the owning match for overflow cleanup. */ MatchId matchOf(Object player);
        /** Resolves bounded reversible Bukkit Locations for a utility action. */
        List<Object> utilityBlocks(DefinitionId effect, UtilityItemDefinition definition,
                                   ItemActionRequest request);
        /** Resolves the Paper Material enum name for utility blocks. */
        Optional<String> utilityMaterial(DefinitionId effect, UtilityItemDefinition definition);
        /** Resolves optional configured sound and particle feedback. */ EffectMapping effect(DefinitionId id);
        /** Clears only effects previously owned by M11 for one player. */ void clearOwnedEffects(PlayerId playerId);
    }

    /** Immutable configured Paper feedback mapping. */
    public static final class EffectMapping {
        private final String sound;
        private final String particle;
        private final float volume;
        private final float pitch;
        private final int particleCount;
        /** Creates a bounded optional feedback mapping. */
        public EffectMapping(final Optional<String> sound, final Optional<String> particle,
                             final float volume, final float pitch, final int particleCount) {
            this.sound = enumName(sound, "sound");
            this.particle = enumName(particle, "particle");
            if (!Float.isFinite(volume) || !Float.isFinite(pitch) || volume < 0F || volume > 4F
                    || pitch < 0.5F || pitch > 2F || particleCount < 0 || particleCount > 512) {
                throw new IllegalArgumentException("effect mapping is outside bounds");
            }
            this.volume = volume;
            this.pitch = pitch;
            this.particleCount = particleCount;
        }
        private static String enumName(final Optional<String> value, final String label) {
            final String result = Objects.requireNonNull(value, label).orElse(null);
            if (result != null && !result.matches("[A-Z][A-Z0-9_]{1,95}")) {
                throw new IllegalArgumentException("invalid " + label + " mapping");
            }
            return result;
        }
        /** @return optional sound enum name */ public Optional<String> sound() { return Optional.ofNullable(sound); }
        /** @return optional particle enum name */ public Optional<String> particle() { return Optional.ofNullable(particle); }
        /** @return sound volume */ public float volume() { return volume; }
        /** @return sound pitch */ public float pitch() { return pitch; }
        /** @return particle count */ public int particleCount() { return particleCount; }
    }

    private static final class BlockSnapshot {
        private final Object block;
        private final Object original;
        private BlockSnapshot(final Object block, final Object original) {
            this.block = block;
            this.original = original;
        }
        private void restore() {
            PaperReflection.invoke(block, "setBlockData",
                    new Class<?>[] {PaperReflection.BLOCK_DATA, boolean.class}, original, false);
        }
    }
}
