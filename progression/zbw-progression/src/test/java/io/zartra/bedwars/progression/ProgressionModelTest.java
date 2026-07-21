package io.zartra.bedwars.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.CurrencyAccount;
import io.zartra.bedwars.progression.model.CurrencyDefinition;
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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Tests immutable M12 Phase 1 model boundaries. */
class ProgressionModelTest {
    private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");

    private PlayerProgressionId player() {
        return PlayerProgressionId.of(PlayerId.of(new UUID(1, 2)));
    }

    private AuditMetadata audit() {
        return new AuditMetadata("system:test", CorrelationId.of(new UUID(3, 4)), NOW, NOW.plusSeconds(1));
    }

    @Test void typedIdsAreTypeSafeAndStable() {
        assertEquals(CurrencyId.of("zartra", "coins"), CurrencyId.parse("zartra:coins"));
        assertEquals(TransactionId.of("zartra", "tx/1"), TransactionId.parse("zartra:tx/1"));
        assertEquals(RewardId.of("zartra", "win"), RewardId.parse("zartra:win"));
        assertEquals(EntitlementId.of("zartra", "unlock/one"), EntitlementId.parse("zartra:unlock/one"));
        assertNotEquals(CurrencyId.of("zartra", "coins"), CurrencyId.of("zartra", "tokens"));
        assertEquals(player(), player());
        assertEquals(player().hashCode(), player().hashCode());
        assertThrows(RuntimeException.class, () -> CurrencyId.parse("bad"));
    }

    @Test void amountsDefinitionsAndStatesRejectInvalidValues() {
        assertEquals(12L, ExperienceAmount.of(5).plus(ExperienceAmount.of(7)).value());
        assertEquals(0L, ExperienceAmount.zero().value());
        assertThrows(IllegalArgumentException.class, () -> ExperienceAmount.of(-1));
        assertThrows(ArithmeticException.class, () -> ExperienceAmount.of(Long.MAX_VALUE).plus(ExperienceAmount.of(1)));
        final LevelDefinition level = new LevelDefinition(2, ExperienceAmount.of(100));
        assertEquals(level, new LevelDefinition(2, ExperienceAmount.of(100)));
        assertEquals(level.hashCode(), new LevelDefinition(2, ExperienceAmount.of(100)).hashCode());
        assertFalse(level.equals("level"));
        assertThrows(IllegalArgumentException.class, () -> new LevelDefinition(0, ExperienceAmount.zero()));
        final LevelState levelState = new LevelState(2, ExperienceAmount.of(120), NOW);
        assertEquals(levelState, new LevelState(2, ExperienceAmount.of(120), NOW));
        assertThrows(IllegalArgumentException.class, () -> new LevelState(0, ExperienceAmount.zero(), NOW));
        final PrestigeDefinition prestige = new PrestigeDefinition(1, 100, "First");
        assertEquals(prestige, new PrestigeDefinition(1, 100, "First"));
        assertThrows(IllegalArgumentException.class, () -> new PrestigeDefinition(-1, 1, "bad"));
        assertThrows(IllegalArgumentException.class, () -> new PrestigeDefinition(1, 0, "bad"));
        assertThrows(IllegalArgumentException.class, () -> new PrestigeDefinition(1, 1, ""));
        final PrestigeState prestigeState = new PrestigeState(1, NOW);
        assertEquals(prestigeState, new PrestigeState(1, NOW));
        assertThrows(IllegalArgumentException.class, () -> new PrestigeState(-1, NOW));
    }

    @Test void progressionAndCurrencySnapshotsAreDefensive() {
        final LinkedHashSet<EntitlementId> grants = new LinkedHashSet<EntitlementId>();
        grants.add(EntitlementId.of("zartra", "unlock/one"));
        final ProgressionAccount account = new ProgressionAccount(player(), ExperienceAmount.of(120),
                new LevelState(2, ExperienceAmount.of(120), NOW), new PrestigeState(1, NOW),
                grants, RecordRevision.of(4), audit());
        grants.clear();
        assertEquals(1, account.entitlements().size());
        assertThrows(UnsupportedOperationException.class, () -> account.entitlements().clear());
        assertEquals(4L, account.revision().value());
        assertEquals("system:test", account.audit().actor());
        final CurrencyDefinition definition = new CurrencyDefinition(CurrencyId.of("zartra", "coins"), "Coins", 1000, true);
        assertEquals("Coins", definition.displayName());
        assertEquals(1000L, definition.maximumBalance());
        assertEquals(true, definition.enabled());
        assertThrows(IllegalArgumentException.class, () -> new CurrencyDefinition(definition.id(), "", 1, true));
        assertThrows(IllegalArgumentException.class, () -> new CurrencyDefinition(definition.id(), "Coins", -1, true));
        final CurrencyAccount currency = new CurrencyAccount(player(), definition.id(), 25, RecordRevision.initial(), audit());
        assertEquals(25L, currency.balance());
        assertThrows(IllegalArgumentException.class, () -> new CurrencyAccount(player(), definition.id(), -1, RecordRevision.initial(), audit()));
    }

    @Test void ledgerRewardAndEntitlementRecordsCarryIdempotencyAndAudit() {
        final IdempotencyKey key = IdempotencyKey.of("zartra", "event/1");
        final TransactionId transaction = TransactionId.of("zartra", "tx/1");
        final CurrencyId currency = CurrencyId.of("zartra", "coins");
        final LedgerEntry ledger = new LedgerEntry(transaction, player(), currency, 10, 10, key, audit());
        assertEquals(10L, ledger.delta());
        assertEquals(10L, ledger.resultingBalance());
        assertEquals(key, ledger.idempotencyKey());
        assertThrows(IllegalArgumentException.class, () -> new LedgerEntry(transaction, player(), currency, 0, 0, key, audit()));
        assertThrows(IllegalArgumentException.class, () -> new LedgerEntry(transaction, player(), currency, -1, -1, key, audit()));
        final ExperienceLedgerEntry experience = new ExperienceLedgerEntry(transaction, player(), 5, ExperienceAmount.of(5), key, audit());
        assertEquals(5L, experience.delta());
        assertThrows(IllegalArgumentException.class, () -> new ExperienceLedgerEntry(transaction, player(), 0, ExperienceAmount.zero(), key, audit()));
        final RewardRecord reward = new RewardRecord(RewardId.of("zartra", "win"), player(), key, audit());
        assertEquals(player(), reward.recipient());
        final EntitlementGrant grant = new EntitlementGrant(player(), EntitlementId.of("zartra", "unlock/one"), key, audit());
        assertEquals(key, grant.idempotencyKey());
        assertEquals(Arrays.asList("system:test", NOW, NOW.plusSeconds(1)), Arrays.asList(audit().actor(), audit().createdAt(), audit().updatedAt()));
        assertThrows(IllegalArgumentException.class, () -> new AuditMetadata("", CorrelationId.random(), NOW, NOW));
        assertThrows(IllegalArgumentException.class, () -> new AuditMetadata("actor", CorrelationId.random(), NOW, NOW.minusSeconds(1)));
    }
}
