package io.zartra.bedwars.arena.application;

import io.zartra.bedwars.api.diagnostic.Diagnostics;
import io.zartra.bedwars.api.health.Health;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import io.zartra.bedwars.world.api.WorldOperationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Non-blocking bounded health and diagnostic projection for arena lifecycle composition. */
public final class ArenaOperationalView implements Health.Source, Diagnostics.Contributor {
    private static final DefinitionId COMPONENT = DefinitionId.of("zartra", "arena/lifecycle");
    private final TimeSource timeSource;
    private final AtomicInteger configured = new AtomicInteger();
    private final AtomicInteger invalid = new AtomicInteger();
    private final AtomicInteger activeSetups = new AtomicInteger();
    private final AtomicLong worldFailures = new AtomicLong();

    /** Creates an empty projection updated explicitly by application adapters. */
    public ArenaOperationalView(final TimeSource timeSource) {
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
    }
    /** Updates bounded configured/invalid arena counts. */
    public void observeInventory(final int configuredArenas, final int invalidArenas) {
        if (configuredArenas < 0 || invalidArenas < 0 || invalidArenas > configuredArenas) {
            throw new IllegalArgumentException("invalid inventory counts");
        }
        configured.set(configuredArenas);
        invalid.set(invalidArenas);
    }
    /** Updates the current bounded setup-session count. */
    public void observeActiveSetups(final int count) {
        if (count < 0 || count > 256) { throw new IllegalArgumentException("active setup count is invalid"); }
        activeSetups.set(count);
    }
    /** Records a terminal world outcome without retaining arena/player identities. */
    public void observeWorld(final WorldOperationResult result) {
        if (Objects.requireNonNull(result, "result").status()
                != WorldOperationResult.Status.SUCCEEDED) { worldFailures.incrementAndGet(); }
    }
    /** @return whether a validation report contributes to invalid count calculations */
    public boolean isInvalid(final ArenaValidation.Report report) {
        return !Objects.requireNonNull(report, "report").mayEnable();
    }
    @Override public DefinitionId id() { return COMPONENT; }
    @Override public Health.Snapshot snapshot() {
        final Health.Status status = invalid.get() == 0 ? Health.Status.HEALTHY : Health.Status.DEGRADED;
        return new Health.Snapshot(COMPONENT, status,
                DefinitionId.of("zartra", status == Health.Status.HEALTHY
                        ? "arena/ready" : "arena/invalid_definitions"), timeSource.now());
    }
    @Override public List<Diagnostics.Field> fields() {
        final List<Diagnostics.Field> fields = new ArrayList<Diagnostics.Field>();
        fields.add(field("configured", configured.get()));
        fields.add(field("invalid", invalid.get()));
        fields.add(field("active_setups", activeSetups.get()));
        fields.add(field("world_failures", worldFailures.get()));
        return fields;
    }
    private static Diagnostics.Field field(final String name, final long value) {
        return new Diagnostics.Field(DefinitionId.of("zartra", "arena/" + name),
                Long.toString(value), Diagnostics.Classification.PUBLIC);
    }
}
