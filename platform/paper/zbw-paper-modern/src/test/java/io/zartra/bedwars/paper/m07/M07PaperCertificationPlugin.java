package io.zartra.bedwars.paper.m07;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.api.identity.MapId;
import io.zartra.bedwars.api.identity.ResourceId;
import io.zartra.bedwars.arena.archive.ArenaArchive;
import io.zartra.bedwars.arena.archive.CanonicalArenaArchiveCodec;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.model.ArenaGenerator;
import io.zartra.bedwars.arena.model.ArenaLocation;
import io.zartra.bedwars.arena.model.ArenaNpc;
import io.zartra.bedwars.arena.model.ArenaRegion;
import io.zartra.bedwars.arena.model.ArenaTeam;
import io.zartra.bedwars.arena.model.MapDefinition;
import io.zartra.bedwars.arena.setup.SetupMutation;
import io.zartra.bedwars.arena.setup.SetupSession;
import io.zartra.bedwars.arena.setup.SetupSessionId;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import io.zartra.bedwars.paper.bootstrap.PaperFoundationRuntime;
import io.zartra.bedwars.paper.bootstrap.ZartraBedWarsPlugin;
import io.zartra.bedwars.world.api.WorldKey;
import io.zartra.bedwars.world.api.WorldOperation;
import io.zartra.bedwars.world.api.WorldOperationResult;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/** Test-artifact-only exact Paper 1.21.1 M07 scenario certification plugin. */
public final class M07PaperCertificationPlugin extends JavaPlugin {
    private static final Instant SCENARIO_TIME = Instant.parse("2026-07-15T00:00:00Z");
    private final List<WorldOperationResult> results = new ArrayList<WorldOperationResult>();
    private PaperFoundationRuntime runtime;
    private boolean arenaValid;
    private boolean setupUndoRedo;
    private boolean archiveRoundTrip;

    @Override public void onEnable() {
        try {
            runtime = runtime();
            certifyArenaApplication();
            startWorldLifecycle();
        } catch (ReflectiveOperationException | RuntimeException failure) {
            getLogger().severe("M07 certification initialization failed: " + failure.getMessage());
            finish(failure);
        }
    }

    private PaperFoundationRuntime runtime() throws ReflectiveOperationException {
        final Plugin dependency = Bukkit.getPluginManager().getPlugin("ZartraBedWars");
        if (!(dependency instanceof ZartraBedWarsPlugin) || !dependency.isEnabled()) {
            throw new IllegalStateException("M06 Paper foundation dependency is unavailable");
        }
        final Field field = ZartraBedWarsPlugin.class.getDeclaredField("runtime");
        field.setAccessible(true);
        return (PaperFoundationRuntime) field.get(dependency);
    }

    private void certifyArenaApplication() {
        final ArenaBundle bundle = completeBundle();
        arenaValid = new ArenaValidation.DefaultValidator().validate(bundle).mayEnable();
        SetupSession session = SetupSession.begin(SetupSessionId.of(UUID.fromString(
                "70000000-0000-0000-0000-000000000001")),
                AuthorizationSubject.of(AuthorizationSubject.Kind.SERVICE, id("actor/certifier")),
                1, bundle);
        session = session.mutate(SetupMutation.metadata(id("metadata/paper_e2e"), "verified"),
                SCENARIO_TIME.plusSeconds(1));
        final ArenaBundle changed = session.draft();
        session = session.undo();
        session = session.redo();
        setupUndoRedo = changed.equals(session.draft()) && session.draftRevision() == 3;
        final CanonicalArenaArchiveCodec codec = new CanonicalArenaArchiveCodec();
        final ArenaArchive archive = codec.encode(id("archive/paper_e2e"), session.draft(),
                SCENARIO_TIME).requireValue();
        archiveRoundTrip = session.draft().equals(codec.decode(archive).requireValue());
        if (!arenaValid || !setupUndoRedo || !archiveRoundTrip) {
            throw new IllegalStateException("M07 arena scenario assertion failed");
        }
    }

    private void startWorldLifecycle() {
        final Duration timeout = Duration.ofSeconds(45);
        final WorldKey template = WorldKey.of("m07_template");
        final WorldKey clone = WorldKey.of("m07_clone");
        run(WorldOperation.create(WorldOperation.Type.LOAD, template, null, timeout))
                .thenCompose(ignored -> run(WorldOperation.create(
                        WorldOperation.Type.UNLOAD, template, null, timeout)))
                .thenCompose(ignored -> run(WorldOperation.create(
                        WorldOperation.Type.CLONE, clone, template, timeout)))
                .thenCompose(ignored -> run(WorldOperation.create(
                        WorldOperation.Type.RESET, clone, template, timeout)))
                .thenCompose(ignored -> run(WorldOperation.create(
                        WorldOperation.Type.UNLOAD, clone, null, timeout)))
                .whenComplete((ignored, failure) -> finish(failure));
    }

    private CompletionStage<WorldOperationResult> run(final WorldOperation operation) {
        return runtime.submit(operation).completion().thenApply(result -> {
            synchronized (results) { results.add(result); }
            if (result.status() != WorldOperationResult.Status.SUCCEEDED) {
                throw new IllegalStateException("world operation failed: " + result.status());
            }
            return result;
        });
    }

    private void finish(final Throwable failure) {
        final Thread writer = new Thread(() -> {
            final boolean offOwner = !Bukkit.isPrimaryThread();
            final boolean leakFree;
            synchronized (results) {
                leakFree = !results.isEmpty()
                        && results.get(results.size() - 1).resources().leakFreeAfterUnload();
            }
            final boolean success = failure == null && arenaValid && setupUndoRedo
                    && archiveRoundTrip && results.size() == 5 && leakFree && offOwner;
            final String evidence = "{\n"
                    + "  \"schema_version\": 1,\n"
                    + "  \"runtime\": \"Paper 1.21.1 build 133\",\n"
                    + "  \"server_sha256\": \""
                    + io.zartra.bedwars.compat.modern.Paper121CompatibilityAdapter.SERVER_SHA256
                    + "\",\n  \"arena_validation\": " + arenaValid
                    + ",\n  \"setup_undo_redo\": " + setupUndoRedo
                    + ",\n  \"archive_round_trip\": " + archiveRoundTrip
                    + ",\n  \"operations\": " + results.size()
                    + ",\n  \"filesystem_evidence_off_owner\": " + offOwner
                    + ",\n  \"leak_free_after_unload\": " + leakFree
                    + ",\n  \"success\": " + success + "\n}\n";
            try {
                final Path path = getDataFolder().toPath().resolve(
                        "m07-primary-certification.json");
                Files.createDirectories(path.getParent());
                Files.write(path, evidence.getBytes(StandardCharsets.UTF_8));
            } catch (IOException exception) {
                getLogger().severe("M07 certification evidence write failed");
            }
            Bukkit.getScheduler().runTask(this, Bukkit::shutdown);
        }, "zbw-m07-certification-evidence");
        writer.setDaemon(false);
        writer.start();
    }

    private static ArenaBundle completeBundle() {
        final ArenaId arenaId = ArenaId.of(UUID.fromString(
                "71000000-0000-0000-0000-000000000001"));
        final MapId mapId = MapId.of(UUID.fromString(
                "72000000-0000-0000-0000-000000000001"));
        final DefinitionId redId = id("team/red");
        final DefinitionId blueId = id("team/blue");
        final ArenaTeam red = ArenaTeam.create(redId, "Red", id("color/red"))
                .withSpawn(location(10, 10, 10)).withBed(location(12, 10, 10),
                        id("facing/east"));
        final ArenaTeam blue = ArenaTeam.create(blueId, "Blue", id("color/blue"))
                .withSpawn(location(90, 10, 90)).withBed(location(88, 10, 90),
                        id("facing/west"));
        final ArenaDefinition arena = ArenaDefinition.builder(arenaId, mapId, "Paper E2E",
                SCENARIO_TIME).worlds(WorldKey.of("m07_clone"), WorldKey.of("m07_template"))
                .modes(Collections.singleton(id("mode/standard"))).playerLimits(2, 4, 2)
                .waitingSpawn(location(50, 20, 50)).spectatorSpawn(location(50, 30, 50))
                .bounds(ArenaRegion.between(id("region/bounds"), location(0, 0, 0),
                        location(100, 100, 100))).limits(-1, 0, 100)
                .teams(Arrays.asList(red, blue)).generators(Arrays.asList(
                        generator("generator/red", "team_iron", "iron", redId, 15, 10, 10),
                        generator("generator/blue", "team_iron", "iron", blueId, 85, 10, 90),
                        generator("generator/diamond", "diamond", "diamond", null, 40, 10, 40),
                        generator("generator/emerald", "emerald", "emerald", null, 60, 10, 60)))
                .npcs(Arrays.asList(
                        npc("npc/red/shop", ArenaNpc.Kind.SHOP, redId, 14, 10, 10),
                        npc("npc/red/upgrade", ArenaNpc.Kind.TEAM_UPGRADE, redId, 16, 10, 10),
                        npc("npc/blue/shop", ArenaNpc.Kind.SHOP, blueId, 84, 10, 90),
                        npc("npc/blue/upgrade", ArenaNpc.Kind.TEAM_UPGRADE, blueId, 82, 10, 90)))
                .status(ArenaDefinition.Status.DISABLED).revision(1, SCENARIO_TIME).build();
        final MapDefinition map = new MapDefinition(mapId, "Paper E2E", SCENARIO_TIME,
                SCENARIO_TIME, 1, id("template/standard"), id("group/default"),
                id("author/certifier"), "Exact Paper certification",
                new HashSet<DefinitionId>(Collections.singleton(id("mode/standard"))), 1, 4,
                Collections.<DefinitionId>emptySet(),
                Collections.<DefinitionId, String>emptyMap(), id("validation/valid"));
        return new ArenaBundle(arena, map);
    }

    private static ArenaGenerator generator(final String name, final String type,
                                             final String resource, final DefinitionId team,
                                             final double x, final double y, final double z) {
        return ArenaGenerator.of(id(name), GeneratorTypeId.of("zartra", type),
                ResourceId.of("zartra", resource), team, location(x, y, z),
                Duration.ofSeconds(1));
    }

    private static ArenaNpc npc(final String name, final ArenaNpc.Kind kind,
                                final DefinitionId team, final double x,
                                final double y, final double z) {
        return ArenaNpc.of(id(name), kind, team, location(x, y, z));
    }

    private static DefinitionId id(final String path) {
        return DefinitionId.of("zartra", path);
    }

    private static ArenaLocation location(final double x, final double y, final double z) {
        return ArenaLocation.of(x, y, z, 0, 0);
    }
}
