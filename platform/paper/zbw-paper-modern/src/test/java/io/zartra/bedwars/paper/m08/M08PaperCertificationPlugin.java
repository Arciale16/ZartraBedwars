package io.zartra.bedwars.paper.m08;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.addon.LobbyProjectionPolicy;
import io.zartra.bedwars.game.model.GameRules;
import io.zartra.bedwars.game.model.MatchCommand;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.MatchStateMachine;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import io.zartra.bedwars.game.model.TeamSnapshot;
import io.zartra.bedwars.paper.game.PaperGameEventTranslator;
import io.zartra.bedwars.paper.game.PaperGameProjectionRuntime;
import io.zartra.bedwars.paper.game.PaperLobbyProjection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Test-artifact-only exact Paper 1.21.1 M08 scenario certification plugin. */
public final class M08PaperCertificationPlugin extends JavaPlugin {
    private static final Instant NOW = Instant.parse("2026-07-15T00:00:00Z");
    private boolean lifecycle;
    private boolean reconnect;
    private boolean exactlyOnce;
    private boolean restoration;
    private boolean bossBar;
    private boolean runtimeLifecycle;

    @Override public void onEnable() {
        try {
            certifyStateMachine();
            certifyPaperProjection();
            certifyOwnerGuard();
        } catch (RuntimeException failure) {
            getLogger().severe("M08 certification failed: " + failure.getMessage());
            finish(false);
        }
    }

    private void certifyStateMachine() {
        final DefinitionId red = id("team/red");
        final DefinitionId blue = id("team/blue");
        final PlayerId first = player(1);
        final PlayerId second = player(2);
        final MatchStateMachine machine = new MatchStateMachine(
                MatchId.of(new UUID(1L, 8L)), ArenaId.of(new UUID(2L, 8L)),
                new GameRules(2, 2, 2, Duration.ofSeconds(30),
                        Duration.ofSeconds(5), Duration.ofSeconds(1)),
                Arrays.asList(TeamSnapshot.empty(red, 1), TeamSnapshot.empty(blue, 1)), NOW);
        machine.apply(MatchCommand.admit(red, state(first)), NOW.plusSeconds(1)).requireValue();
        machine.apply(MatchCommand.admit(blue, state(second)), NOW.plusSeconds(2)).requireValue();
        machine.apply(MatchCommand.startCountdown(), NOW.plusSeconds(3)).requireValue();
        machine.apply(MatchCommand.disconnect(first), NOW.plusSeconds(4)).requireValue();
        machine.apply(MatchCommand.reconnect(first), NOW.plusSeconds(5)).requireValue();
        reconnect = machine.snapshot().session(first).orElseThrow(
                () -> new IllegalStateException("session missing")).status()
                == io.zartra.bedwars.game.model.PlayerSession.Status.WAITING;
        machine.apply(MatchCommand.tick(), NOW.plusSeconds(6)).requireValue();
        machine.apply(MatchCommand.tick(), NOW.plusSeconds(7)).requireValue();
        final IdempotencyKey key = IdempotencyKey.of("zartra", "completion/paper_m08");
        machine.apply(MatchCommand.complete(id("outcome/red"), key), NOW.plusSeconds(8))
                .requireValue();
        machine.apply(MatchCommand.complete(id("outcome/red"), key), NOW.plusSeconds(9))
                .requireValue();
        machine.apply(MatchCommand.commitCompletion(key), NOW.plusSeconds(10)).requireValue();
        final long committedRevision = machine.snapshot().revision();
        machine.apply(MatchCommand.commitCompletion(key), NOW.plusSeconds(11)).requireValue();
        exactlyOnce = machine.snapshot().completionCommitted()
                && machine.snapshot().revision() == committedRevision;
        machine.apply(MatchCommand.restore(first), NOW.plusSeconds(12)).requireValue();
        machine.apply(MatchCommand.restore(second), NOW.plusSeconds(13)).requireValue();
        restoration = machine.snapshot().activeSessionCount() == 0;
        machine.apply(MatchCommand.finishReset(), NOW.plusSeconds(14)).requireValue();
        lifecycle = machine.snapshot().state() == MatchSnapshot.State.WAITING
                && machine.snapshot().sessions().isEmpty();
        if (!lifecycle || !reconnect || !exactlyOnce || !restoration) {
            throw new IllegalStateException("neutral lifecycle assertion failed");
        }
    }

    private void certifyPaperProjection() {
        final PaperLobbyProjection lobby = new PaperLobbyProjection();
        final MatchId matchId = MatchId.of(new UUID(3L, 8L));
        lobby.bossBar(matchId, LobbyProjectionPolicy.bossBar(
                LobbyProjectionPolicy.BossBarState.PLAYING, "bossbar.playing", 0.5D,
                LobbyProjectionPolicy.BossBarColor.BLUE,
                LobbyProjectionPolicy.BossBarStyle.SEGMENTED_10,
                Duration.ofMillis(100), Collections.<PlayerId>emptySet()));
        final boolean created = lobby.activeBossBars() == 1;
        lobby.bossBar(matchId, LobbyProjectionPolicy.bossBar(
                LobbyProjectionPolicy.BossBarState.POST_GAME, "bossbar.complete", 1.0D,
                LobbyProjectionPolicy.BossBarColor.GREEN,
                LobbyProjectionPolicy.BossBarStyle.SOLID,
                Duration.ofMillis(100), Collections.<PlayerId>emptySet()));
        lobby.removeBossBar(matchId);
        bossBar = created && lobby.activeBossBars() == 0;
        final PaperGameProjectionRuntime runtime = new PaperGameProjectionRuntime(
                this, new NoOpSink(), lobby);
        runtime.start();
        final boolean started = runtime.isStarted();
        runtime.stop();
        runtimeLifecycle = started && !runtime.isStarted();
        if (!bossBar || !runtimeLifecycle) {
            throw new IllegalStateException("Paper projection lifecycle assertion failed");
        }
    }

    private void certifyOwnerGuard() {
        final CountDownLatch complete = new CountDownLatch(1);
        final AtomicBoolean rejected = new AtomicBoolean();
        final Thread worker = new Thread(() -> {
            try {
                new PaperLobbyProjection().close();
            } catch (IllegalStateException expected) {
                rejected.set(true);
            } finally {
                complete.countDown();
            }
        }, "zbw-m08-owner-guard");
        worker.start();
        try {
            if (!complete.await(10, TimeUnit.SECONDS) || !rejected.get()) {
                throw new IllegalStateException("off-owner mutation was not rejected");
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("owner guard interrupted", failure);
        }
        finish(true);
    }

    private void finish(final boolean ownerGuard) {
        final Thread writer = new Thread(() -> {
            final boolean offOwner = !Bukkit.isPrimaryThread();
            final boolean success = ownerGuard && offOwner && lifecycle && reconnect
                    && exactlyOnce && restoration && bossBar && runtimeLifecycle;
            final String evidence = "{\n"
                    + "  \"schema_version\": 1,\n"
                    + "  \"runtime\": \"Paper 1.21.1 build 133\",\n"
                    + "  \"server_sha256\": \""
                    + io.zartra.bedwars.compat.modern.Paper121CompatibilityAdapter.SERVER_SHA256
                    + "\",\n  \"waiting_through_reset\": " + lifecycle
                    + ",\n  \"reconnect_recovery\": " + reconnect
                    + ",\n  \"exactly_once_completion\": " + exactlyOnce
                    + ",\n  \"player_state_restoration\": " + restoration
                    + ",\n  \"bossbar_create_update_remove\": " + bossBar
                    + ",\n  \"event_runtime_register_unregister\": " + runtimeLifecycle
                    + ",\n  \"off_owner_mutation_rejected\": " + ownerGuard
                    + ",\n  \"evidence_written_off_owner\": " + offOwner
                    + ",\n  \"success\": " + success + "\n}\n";
            try {
                final Path path = getDataFolder().toPath().resolve(
                        "m08-primary-certification.json");
                Files.createDirectories(path.getParent());
                Files.write(path, evidence.getBytes(StandardCharsets.UTF_8));
            } catch (IOException failure) {
                getLogger().severe("M08 certification evidence write failed");
            }
            Bukkit.getScheduler().runTask(this, Bukkit::shutdown);
        }, "zbw-m08-certification-evidence");
        writer.setDaemon(false);
        writer.start();
    }

    private static PlayerStateSnapshot state(final PlayerId playerId) {
        return new PlayerStateSnapshot(playerId, PlayerStateSnapshot.Inventory.empty(36),
                new PlayerStateSnapshot.Location(id("world/lobby"), 0.0D, 64.0D, 0.0D,
                        0.0F, 0.0F), PlayerStateSnapshot.Mode.SURVIVAL, true);
    }

    private static DefinitionId id(final String path) {
        return DefinitionId.of("zartra", path);
    }

    private static PlayerId player(final long value) {
        return PlayerId.of(new UUID(0L, value));
    }

    private static final class NoOpSink implements PaperGameEventTranslator.Sink {
        @Override public void disconnected(final PlayerId playerId) { }
        @Override public void died(final PlayerId playerId) { }
        @Override public void leaveDelaySignal(
                final PlayerId playerId,
                final io.zartra.bedwars.game.addon.LeaveDelayPolicy.Signal signal) { }
    }
}
