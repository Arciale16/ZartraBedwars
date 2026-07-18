package io.zartra.bedwars.shop.api;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.util.Objects;
import java.util.Optional;

/** Immutable authenticated match context used by shop policies. */
public final class PurchaseContext {
    private final AuthorizationSubject subject;
    private final PlayerId playerId;
    private final MatchId matchId;
    private final ArenaId arenaId;
    private final DefinitionId modeId;
    private final DefinitionId teamId;
    private final DefinitionId groupId;

    /** Creates a fully identified player purchase context. */
    public PurchaseContext(final AuthorizationSubject subject, final PlayerId playerId,
                           final MatchId matchId, final ArenaId arenaId,
                           final DefinitionId modeId, final DefinitionId teamId,
                           final Optional<DefinitionId> groupId) {
        this.subject = Objects.requireNonNull(subject, "subject");
        if (subject.kind() != AuthorizationSubject.Kind.PLAYER) {
            throw new IllegalArgumentException("shop purchase subject must be a player");
        }
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
        this.modeId = Objects.requireNonNull(modeId, "modeId");
        this.teamId = Objects.requireNonNull(teamId, "teamId");
        this.groupId = Objects.requireNonNull(groupId, "groupId").orElse(null);
    }

    /** @return authenticated authorization subject */ public AuthorizationSubject subject() { return subject; }
    /** @return stable player identity */ public PlayerId playerId() { return playerId; }
    /** @return owning match */ public MatchId matchId() { return matchId; }
    /** @return arena identity */ public ArenaId arenaId() { return arenaId; }
    /** @return selected mode */ public DefinitionId modeId() { return modeId; }
    /** @return assigned team */ public DefinitionId teamId() { return teamId; }
    /** @return optional configured group */ public Optional<DefinitionId> groupId() { return Optional.ofNullable(groupId); }
}
