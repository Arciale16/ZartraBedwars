package io.zartra.bedwars.shop.upgrade;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Immutable recoverable upgrade/trap state isolated to one match team. */
public final class TeamUpgradeState {
    private final MatchId matchId;
    private final DefinitionId teamId;
    private final long revision;
    private final Map<DefinitionId, Integer> levels;
    private final List<TrapCharge> traps;
    private final Set<IdempotencyKey> completed;
    private final boolean cleaned;
    private TeamUpgradeState(final MatchId matchId, final DefinitionId teamId, final long revision,
                             final Map<DefinitionId, Integer> levels, final List<TrapCharge> traps,
                             final Set<IdempotencyKey> completed, final boolean cleaned) {
        if (revision < 0L) { throw new IllegalArgumentException("revision must be non-negative"); }
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.teamId = Objects.requireNonNull(teamId, "teamId");
        this.revision = revision;
        this.levels = Collections.unmodifiableMap(new TreeMap<DefinitionId, Integer>(levels));
        this.traps = Collections.unmodifiableList(new ArrayList<TrapCharge>(traps));
        this.completed = Collections.unmodifiableSet(new TreeSet<IdempotencyKey>(completed));
        this.cleaned = cleaned;
    }
    /** @return empty team state */ public static TeamUpgradeState empty(final MatchId matchId, final DefinitionId teamId) { return new TeamUpgradeState(matchId, teamId, 0L, Collections.<DefinitionId, Integer>emptyMap(), Collections.<TrapCharge>emptyList(), Collections.<IdempotencyKey>emptySet(), false); }
    /** Restores a validated immutable snapshot for recovery/rejoin. */
    public static TeamUpgradeState restore(final MatchId matchId, final DefinitionId teamId,
                                           final long revision, final Map<DefinitionId, Integer> levels,
                                           final List<TrapCharge> traps,
                                           final Set<IdempotencyKey> completed) {
        return new TeamUpgradeState(matchId, teamId, revision, levels, traps, completed, false);
    }
    /** @return match */ public MatchId matchId() { return matchId; }
    /** @return team */ public DefinitionId teamId() { return teamId; }
    /** @return revision */ public long revision() { return revision; }
    /** @return level, zero when not purchased */ public int level(final DefinitionId id) { return levels.containsKey(id) ? levels.get(id) : 0; }
    /** @return immutable levels */ public Map<DefinitionId, Integer> levels() { return levels; }
    /** @return immutable queued traps */ public List<TrapCharge> traps() { return traps; }
    /** @return whether operation already committed */ public boolean completed(final IdempotencyKey key) { return completed.contains(key); }
    /** @return terminal cleanup state */ public boolean cleaned() { return cleaned; }
    TeamUpgradeState upgraded(final UpgradeDefinition definition, final int level,
                              final IdempotencyKey key) {
        final Map<DefinitionId, Integer> changed = new TreeMap<DefinitionId, Integer>(levels);
        changed.put(definition.id(), level);
        final List<TrapCharge> changedTraps = new ArrayList<TrapCharge>(traps);
        if (definition.kind() == UpgradeDefinition.Kind.TRAP) {
            changedTraps.add(new TrapCharge(definition.id(), definition.level(level).effect()));
        }
        final Set<IdempotencyKey> keys = new TreeSet<IdempotencyKey>(completed);
        keys.add(key);
        return new TeamUpgradeState(matchId, teamId, revision + 1L, changed, changedTraps, keys, false);
    }
    TeamUpgradeState activatedTrap(final IdempotencyKey key) {
        final List<TrapCharge> changed = new ArrayList<TrapCharge>(traps);
        changed.remove(0);
        final Set<IdempotencyKey> keys = new TreeSet<IdempotencyKey>(completed);
        keys.add(key);
        return new TeamUpgradeState(matchId, teamId, revision + 1L, levels, changed, keys, false);
    }
    TeamUpgradeState cleanup() { return new TeamUpgradeState(matchId, teamId, revision + 1L, Collections.<DefinitionId, Integer>emptyMap(), Collections.<TrapCharge>emptyList(), completed, true); }

    /** Immutable queued trap charge. */
    public static final class TrapCharge {
        private final DefinitionId upgradeId;
        private final DefinitionId effect;
        /** Creates a validated charge, including during recovery. */
        public TrapCharge(final DefinitionId upgradeId, final DefinitionId effect) {
            this.upgradeId = Objects.requireNonNull(upgradeId, "upgradeId");
            this.effect = Objects.requireNonNull(effect, "effect");
        }
        /** @return source upgrade */ public DefinitionId upgradeId() { return upgradeId; }
        /** @return activation effect */ public DefinitionId effect() { return effect; }
    }
}
