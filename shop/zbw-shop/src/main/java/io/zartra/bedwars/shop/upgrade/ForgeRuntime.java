package io.zartra.bedwars.shop.upgrade;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.game.model.MatchSnapshot;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Deterministic bounded forge scheduler driven by M08 snapshots and recovered team state. */
public final class ForgeRuntime {
    /** Maximum catch-up emissions per invocation. */ public static final int MAX_EMISSIONS = 128;
    private final ForgePolicy policy;
    private long sequence;
    private int currentLevel;
    private Instant next;
    private boolean cleaned;
    /** Creates an idle forge runtime. */
    public ForgeRuntime(final ForgePolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }
    /** Emits all due resource intents up to the strict bound. */
    public synchronized List<TeamEffectIntent> tick(final MatchSnapshot match,
                                                     final TeamUpgradeState state,
                                                     final Instant now) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(now, "now");
        if (!match.matchId().equals(state.matchId()) || !match.team(state.teamId()).isPresent()) {
            throw new IllegalArgumentException("forge state does not belong to match team");
        }
        if (match.state() != MatchSnapshot.State.PLAYING || state.cleaned()) {
            cleanup();
            return Collections.emptyList();
        }
        final int level = state.level(policy.upgradeId());
        if (level == 0 || cleaned) { return Collections.emptyList(); }
        final ForgePolicy.Level selected = policy.level(level);
        if (next == null || level != currentLevel) {
            currentLevel = level;
            next = now.plus(selected.interval());
            return Collections.emptyList();
        }
        final List<TeamEffectIntent> intents = new ArrayList<TeamEffectIntent>();
        while (intents.size() < MAX_EMISSIONS && !now.isBefore(next)) {
            sequence++;
            final String teamDigest = UUID.nameUUIDFromBytes(state.teamId().toString()
                    .getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
            final IdempotencyKey key = IdempotencyKey.of("zartra", "forge/"
                    + match.matchId().toString().replace("-", "") + "/" + teamDigest + "/" + sequence);
            intents.add(new TeamEffectIntent(key, TeamEffectIntent.Kind.FORGE_RESOURCES,
                    state.teamId(), DefinitionId.of("zartra", "effect/forge"), null,
                    selected.resources()));
            next = next.plus(selected.interval());
        }
        return Collections.unmodifiableList(intents);
    }
    /** Permanently clears match-local timing. */
    public synchronized void cleanup() {
        next = null;
        cleaned = true;
    }
    /** @return monotonic emission sequence */ public synchronized long sequence() { return sequence; }
    /** @return whether terminal cleanup occurred */ public synchronized boolean cleaned() { return cleaned; }
}
