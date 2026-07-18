package io.zartra.bedwars.shop.integration;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.shop.generator.GeneratorFleet;
import io.zartra.bedwars.shop.generator.ResourceDeliveryPort;
import io.zartra.bedwars.shop.item.ItemActionService;
import io.zartra.bedwars.shop.upgrade.TeamUpgradeService;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Match-scoped M11 coordinator consuming the M08 lifecycle without owning its transitions. */
public final class M11MatchRuntime {
    private final GeneratorFleet generators;
    private final ResourceDeliveryPort delivery;
    private final Map<DefinitionId, TeamUpgradeService> upgrades;
    private final ItemActionService items;
    private boolean started;
    private boolean cleaned;

    /** Creates a coordinator from independently tested M11 services. */
    public M11MatchRuntime(final GeneratorFleet generators, final ResourceDeliveryPort delivery,
                           final Collection<TeamUpgradeService> upgrades,
                           final ItemActionService items) {
        this.generators = Objects.requireNonNull(generators, "generators");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
        final Map<DefinitionId, TeamUpgradeService> copy = new TreeMap<DefinitionId, TeamUpgradeService>();
        for (TeamUpgradeService service : Objects.requireNonNull(upgrades, "upgrades")) {
            final TeamUpgradeService checked = Objects.requireNonNull(service, "upgrade");
            if (copy.put(checked.state().teamId(), checked) != null) {
                throw new IllegalArgumentException("duplicate team upgrade service");
            }
        }
        this.upgrades = Collections.unmodifiableMap(copy);
        this.items = Objects.requireNonNull(items, "items");
    }

    /** Applies one immutable M08 snapshot and returns the number of delivered generator batches. */
    public synchronized int onSnapshot(final MatchSnapshot snapshot, final Instant now) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(now, "now");
        if (cleaned) { return 0; }
        for (TeamUpgradeService service : upgrades.values()) { service.observe(snapshot); }
        items.synchronize(snapshot);
        if (snapshot.state() != MatchSnapshot.State.PLAYING) {
            cleanup();
            return 0;
        }
        if (!started) {
            generators.start(snapshot, now);
            started = true;
        }
        return generators.tick(snapshot, now, delivery);
    }

    /** Performs idempotent match-end cleanup across every M11 subsystem. */
    public synchronized void cleanup() {
        if (cleaned) { return; }
        generators.cleanup();
        items.cleanup();
        cleaned = true;
    }
    /** @return whether PLAYING initialization occurred */ public synchronized boolean started() { return started; }
    /** @return whether terminal cleanup occurred */ public synchronized boolean cleaned() { return cleaned; }
}
