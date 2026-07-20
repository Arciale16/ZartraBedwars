package io.zartra.bedwars.progression.application;

import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.progression.experience.ExperiencePolicy;
import io.zartra.bedwars.progression.level.LevelFormula;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.model.ProgressionAccount;
import io.zartra.bedwars.progression.prestige.PrestigePolicy;
import io.zartra.bedwars.progression.reward.RewardEngine;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Authorized application use cases for gameplay-independent progression administration. */
public final class ProgressionService {
    /** Exact M03 action nodes. */
    public static final PermissionNode GRANT_XP = PermissionNode.of("zartrabedwars.admin.progression.grant-xp");
    /** Exact M03 action nodes. */ public static final PermissionNode REMOVE_XP = PermissionNode.of("zartrabedwars.admin.progression.remove-xp");
    /** Exact M03 action nodes. */ public static final PermissionNode GRANT_REWARD = PermissionNode.of("zartrabedwars.admin.progression.grant-reward");
    /** Exact M03 action nodes. */ public static final PermissionNode INSPECT = PermissionNode.of("zartrabedwars.admin.progression.inspect");
    /** Exact M03 action nodes. */ public static final PermissionNode RECALCULATE = PermissionNode.of("zartrabedwars.admin.progression.recalculate");
    /** Exact M03 action nodes. */ public static final PermissionNode PRESTIGE = PermissionNode.of("zartrabedwars.admin.progression.prestige");

    private final AuthorizationService authorization;
    private final MutationPort mutations;
    private final ExperiencePolicy experiencePolicy;
    private final LevelFormula levelFormula;

    /** Creates a use-case service. */
    public ProgressionService(final AuthorizationService authorization, final MutationPort mutations,
                              final ExperiencePolicy experiencePolicy, final LevelFormula levelFormula) {
        this.authorization = Objects.requireNonNull(authorization, "authorization");
        this.mutations = Objects.requireNonNull(mutations, "mutations");
        this.experiencePolicy = Objects.requireNonNull(experiencePolicy, "experiencePolicy");
        this.levelFormula = Objects.requireNonNull(levelFormula, "levelFormula");
    }

    /** Grants policy-calculated XP exactly once. */
    public MutationResult grantExperience(final AuthorizationSubject actor,
            final PlayerProgressionId player, final DefinitionId source, final long requested,
            final int multiplierBasisPoints, final long bonus, final int recentAwards,
            final IdempotencyKey key, final Instant now) {
        require(actor, GRANT_XP, player);
        final ExperiencePolicy.Award award = experiencePolicy.calculate(source, requested,
                multiplierBasisPoints, bonus, recentAwards, now);
        return mutations.changeExperience(player, award.awarded(), key, actor, now, levelFormula.version());
    }

    /** Removes XP when the installed mutation policy permits negative adjustment. */
    public MutationResult removeExperience(final AuthorizationSubject actor,
            final PlayerProgressionId player, final long amount, final IdempotencyKey key,
            final Instant now) {
        require(actor, REMOVE_XP, player);
        if (amount < 1) { throw new IllegalArgumentException("amount must be positive"); }
        return mutations.changeExperience(player, -amount, key, actor, now, levelFormula.version());
    }

    /** Registers and delivers or queues a generic reward. */
    public RewardEngine.Outcome grantReward(final AuthorizationSubject actor,
            final RewardEngine engine, final RewardEngine.Plan plan, final Instant now,
            final boolean online) {
        require(actor, GRANT_REWARD, plan.recipient());
        return Objects.requireNonNull(engine, "engine").grant(plan, now, online);
    }

    /** Reads a progression snapshot through the authorized port. */
    public Optional<ProgressionAccount> inspect(final AuthorizationSubject actor,
                                                final PlayerProgressionId player) {
        require(actor, INSPECT, player);
        return mutations.inspect(player);
    }

    /** Recalculates derived level state using the currently configured formula version. */
    public MutationResult recalculate(final AuthorizationSubject actor,
            final PlayerProgressionId player, final IdempotencyKey key, final Instant now) {
        require(actor, RECALCULATE, player);
        return mutations.recalculate(player, levelFormula, key, actor, now);
    }

    /** Commits prestige state, history, audit and reward intent in one idempotent transaction. */
    public MutationResult prestige(final AuthorizationSubject actor,
            final PlayerProgressionId player, final PrestigePolicy policy,
            final int requestedTier, final IdempotencyKey key, final Instant now) {
        require(actor, PRESTIGE, player);
        return mutations.prestige(player, Objects.requireNonNull(policy, "policy"),
                requestedTier, key, actor, now);
    }

    private void require(final AuthorizationSubject actor, final PermissionNode node,
                         final PlayerProgressionId player) {
        final DefinitionId target = DefinitionId.of("progression", player.toString().replace(':', '-'));
        if (!authorization.authorize(AuthorizationRequest.of(actor, node, target)).isAllowed()) {
            throw new SecurityException("progression action denied");
        }
    }

    /** Transactional persistence boundary for all account mutations and their audit evidence. */
    public interface MutationPort {
        /** Applies signed XP, ledger, history, unlock and reward intents atomically and idempotently. */
        MutationResult changeExperience(PlayerProgressionId player, long delta, IdempotencyKey key,
                                        AuthorizationSubject actor, Instant now, int formulaVersion);
        /** @return current snapshot */ Optional<ProgressionAccount> inspect(PlayerProgressionId player);
        /** Recalculates derived state atomically, retaining migration history. */
        MutationResult recalculate(PlayerProgressionId player, LevelFormula formula,
                                   IdempotencyKey key, AuthorizationSubject actor, Instant now);
        /** Atomically commits prestige or rolls back every related record. */
        MutationResult prestige(PlayerProgressionId player, PrestigePolicy policy, int requestedTier,
                                IdempotencyKey key, AuthorizationSubject actor, Instant now);
    }

    /** Immutable mutation acknowledgement. */
    public static final class MutationResult {
        private final ProgressionAccount account;
        private final boolean duplicate;
        /** Creates an acknowledgement. */ public MutationResult(final ProgressionAccount account,
                                                                  final boolean duplicate) {
            this.account = Objects.requireNonNull(account, "account");
            this.duplicate = duplicate;
        }
        /** @return committed account */ public ProgressionAccount account() { return account; }
        /** @return whether prior evidence was returned */ public boolean duplicate() { return duplicate; }
    }
}
