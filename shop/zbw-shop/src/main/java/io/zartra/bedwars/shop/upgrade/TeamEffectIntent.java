package io.zartra.bedwars.shop.upgrade;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ResourceId;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Immutable platform-neutral request to apply a team effect or deliver forge resources. */
public final class TeamEffectIntent {
    /** Effect class translated later by the primary Paper adapter. */
    public enum Kind { UPGRADE_APPLIED, TRAP_ACTIVATED, HEAL_POOL, DRAGON_BUFF, FORGE_RESOURCES }
    private final IdempotencyKey key;
    private final Kind kind;
    private final DefinitionId teamId;
    private final DefinitionId effect;
    private final PlayerId target;
    private final Map<ResourceId, Integer> resources;
    /** Creates a validated effect intent. */
    public TeamEffectIntent(final IdempotencyKey key, final Kind kind, final DefinitionId teamId,
                            final DefinitionId effect, final PlayerId target,
                            final Map<ResourceId, Integer> resources) {
        this.key = Objects.requireNonNull(key, "key");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.teamId = Objects.requireNonNull(teamId, "teamId");
        this.effect = Objects.requireNonNull(effect, "effect");
        this.target = target;
        final Map<ResourceId, Integer> copy = new TreeMap<ResourceId, Integer>();
        for (Map.Entry<ResourceId, Integer> entry : Objects.requireNonNull(resources, "resources").entrySet()) {
            final int amount = Objects.requireNonNull(entry.getValue(), "resource amount");
            if (amount < 1) { throw new IllegalArgumentException("resource amount must be positive"); }
            copy.put(Objects.requireNonNull(entry.getKey(), "resource"), amount);
        }
        if (kind == Kind.FORGE_RESOURCES != !copy.isEmpty()) {
            throw new IllegalArgumentException("only forge intents carry resources");
        }
        this.resources = Collections.unmodifiableMap(copy);
    }
    /** @return idempotency key */ public IdempotencyKey key() { return key; }
    /** @return kind */ public Kind kind() { return kind; }
    /** @return owning team */ public DefinitionId teamId() { return teamId; }
    /** @return semantic effect */ public DefinitionId effect() { return effect; }
    /** @return optional player target */ public Optional<PlayerId> target() { return Optional.ofNullable(target); }
    /** @return forge resources or empty map */ public Map<ResourceId, Integer> resources() { return resources; }
}
