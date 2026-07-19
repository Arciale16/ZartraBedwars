package io.zartra.bedwars.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.CurrencyAccount;
import io.zartra.bedwars.progression.model.CurrencyId;
import io.zartra.bedwars.progression.model.EntitlementGrant;
import io.zartra.bedwars.progression.model.EntitlementId;
import io.zartra.bedwars.progression.model.ExperienceAmount;
import io.zartra.bedwars.progression.model.ExperienceLedgerEntry;
import io.zartra.bedwars.progression.model.LedgerEntry;
import io.zartra.bedwars.progression.model.LevelDefinition;
import io.zartra.bedwars.progression.model.LevelState;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.model.PrestigeDefinition;
import io.zartra.bedwars.progression.model.PrestigeState;
import io.zartra.bedwars.progression.model.ProgressionAccount;
import io.zartra.bedwars.progression.model.RewardId;
import io.zartra.bedwars.progression.model.RewardRecord;
import io.zartra.bedwars.progression.model.TransactionId;
import io.zartra.bedwars.storage.api.RecordRevision;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Exercises defensive branches and every Phase 1 model projection accessor. */
class ModelBoundaryCoverageTest {
    private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");
    private final PlayerProgressionId player = PlayerProgressionId.of(PlayerId.of(new UUID(5, 6)));
    private final AuditMetadata audit = new AuditMetadata("admin", CorrelationId.of(new UUID(7, 8)), NOW, NOW);
    private final IdempotencyKey key = IdempotencyKey.of("zartra", "operation/coverage");
    private final TransactionId transaction = TransactionId.of("zartra", "transaction/coverage");

    @Test void allAggregateAccessorsAndIdentityBranchesAreCovered() {
        assertNotNull(player.playerId());
        assertEquals(player.toString(), player.playerId().toString());
        assertFalse(player.equals("player"));
        final LevelState level = new LevelState(3, ExperienceAmount.of(300), NOW);
        assertEquals(3, level.level());
        assertEquals(300L, level.experience().value());
        assertEquals(NOW, level.attainedAt());
        assertEquals(level.hashCode(), new LevelState(3, ExperienceAmount.of(300), NOW).hashCode());
        assertFalse(level.equals("level"));
        assertFalse(level.equals(new LevelState(4, ExperienceAmount.of(300), NOW)));
        assertFalse(level.equals(new LevelState(3, ExperienceAmount.of(301), NOW)));
        final PrestigeState prestige = new PrestigeState(2, NOW);
        assertEquals(2, prestige.prestige());
        assertEquals(NOW, prestige.attainedAt());
        assertEquals(prestige.hashCode(), new PrestigeState(2, NOW).hashCode());
        assertFalse(prestige.equals("prestige"));
        assertFalse(prestige.equals(new PrestigeState(3, NOW)));
        final ProgressionAccount account = new ProgressionAccount(player, ExperienceAmount.of(300), level,
                prestige, Collections.singleton(EntitlementId.of("zartra", "unlock/coverage")),
                RecordRevision.of(2), audit);
        assertEquals(player, account.id());
        assertEquals(level, account.level());
        assertEquals(prestige, account.prestige());
        assertEquals(300L, account.experience().value());
        assertThrows(IllegalArgumentException.class, () -> new ProgressionAccount(player,
                ExperienceAmount.zero(), level, prestige, Collections.singleton(null),
                RecordRevision.initial(), audit));
    }

    @Test void everyLedgerAndRegistrationAccessorIsCovered() {
        final CurrencyId currency = CurrencyId.of("zartra", "tokens");
        final CurrencyAccount account = new CurrencyAccount(player, currency, 20,
                RecordRevision.of(1), audit);
        assertEquals(player, account.owner());
        assertEquals(currency, account.currencyId());
        assertEquals(1L, account.revision().value());
        assertEquals(audit, account.audit());
        final LedgerEntry ledger = new LedgerEntry(transaction, player, currency, -5, 15, key, audit);
        assertEquals(transaction, ledger.transactionId());
        assertEquals(player, ledger.owner());
        assertEquals(currency, ledger.currencyId());
        assertEquals(audit, ledger.audit());
        final ExperienceLedgerEntry experience = new ExperienceLedgerEntry(transaction, player,
                -5, ExperienceAmount.of(295), key, audit);
        assertEquals(transaction, experience.transactionId());
        assertEquals(player, experience.owner());
        assertEquals(295L, experience.resultingExperience().value());
        assertEquals(key, experience.idempotencyKey());
        assertEquals(audit, experience.audit());
        final RewardRecord reward = new RewardRecord(RewardId.of("zartra", "reward/coverage"),
                player, key, audit);
        assertNotNull(reward.rewardId());
        assertEquals(key, reward.idempotencyKey());
        assertEquals(audit, reward.audit());
        final EntitlementGrant grant = new EntitlementGrant(player,
                EntitlementId.of("zartra", "unlock/coverage"), key, audit);
        assertEquals(player, grant.owner());
        assertNotNull(grant.entitlementId());
        assertEquals(audit, grant.audit());
    }

    @Test void definitionEqualityBranchesAndNullGuardsFailClosed() {
        final LevelDefinition level = new LevelDefinition(3, ExperienceAmount.of(300));
        assertEquals(3, level.level());
        assertEquals(300L, level.requiredExperience().value());
        assertFalse(level.equals(new LevelDefinition(4, ExperienceAmount.of(300))));
        assertFalse(level.equals(new LevelDefinition(3, ExperienceAmount.of(301))));
        final PrestigeDefinition prestige = new PrestigeDefinition(2, 100, "Second");
        assertEquals(2, prestige.prestige());
        assertEquals(100, prestige.requiredLevel());
        assertEquals("Second", prestige.displayName());
        assertEquals(prestige.hashCode(), new PrestigeDefinition(2, 100, "Second").hashCode());
        assertFalse(prestige.equals("prestige"));
        assertFalse(prestige.equals(new PrestigeDefinition(3, 100, "Second")));
        assertFalse(prestige.equals(new PrestigeDefinition(2, 101, "Second")));
        assertFalse(prestige.equals(new PrestigeDefinition(2, 100, "Other")));
        assertThrows(NullPointerException.class, () -> new CurrencyAccount(null,
                CurrencyId.of("zartra", "tokens"), 0, RecordRevision.initial(), audit));
    }
}
