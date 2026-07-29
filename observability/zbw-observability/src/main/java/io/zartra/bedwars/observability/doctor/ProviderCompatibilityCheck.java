package io.zartra.bedwars.observability.doctor;

import io.zartra.bedwars.api.diagnostic.Diagnostics;
import io.zartra.bedwars.api.doctor.PluginDoctor;
import io.zartra.bedwars.api.health.Health;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.scheduler.TaskContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Secret-safe Plugin Doctor check for the materialized M21 provider boundary.
 *
 * <p>ZBW-INT-002/003/005/006/007/008/009 and ZBW-DEPLOY-005: this check observes lifecycle
 * compatibility only. It neither loads vendor classes nor invokes feature or domain behavior.</p>
 */
public final class ProviderCompatibilityCheck implements PluginDoctor.Check {
    private static final DefinitionId CHECK_ID =
            DefinitionId.of("zartra", "doctor/provider-compatibility");
    private static final DefinitionId HEALTHY =
            DefinitionId.of("zartra", "doctor/providers-healthy");
    private static final DefinitionId DEGRADED =
            DefinitionId.of("zartra", "doctor/providers-degraded");
    private static final DefinitionId FAILED =
            DefinitionId.of("zartra", "doctor/providers-failed");
    private final List<ProviderId> required;
    private final Map<ProviderId, List<Provider>> providers;
    private final Set<ProviderId> rejectedDuplicates;

    /**
     * Creates a deterministic provider compatibility check.
     *
     * @param required required provider identities
     * @param installed installed provider instances
     * @param rejectedDuplicates identities rejected by the composition registry as duplicates
     */
    public ProviderCompatibilityCheck(
            final Collection<ProviderId> required,
            final Collection<? extends Provider> installed,
            final Collection<ProviderId> rejectedDuplicates) {
        final TreeSet<ProviderId> requiredCopy =
                new TreeSet<ProviderId>(Objects.requireNonNull(required, "required"));
        if (requiredCopy.isEmpty()) {
            throw new IllegalArgumentException("required providers must not be empty or null");
        }
        this.required = Collections.unmodifiableList(
                new ArrayList<ProviderId>(requiredCopy));
        final Map<ProviderId, List<Provider>> grouped =
                new TreeMap<ProviderId, List<Provider>>();
        for (Provider provider : Objects.requireNonNull(installed, "installed")) {
            final Provider value = Objects.requireNonNull(provider, "provider");
            final ProviderId id = value.descriptor().id();
            grouped.computeIfAbsent(id, ignored -> new ArrayList<Provider>()).add(value);
        }
        final Map<ProviderId, List<Provider>> immutable =
                new TreeMap<ProviderId, List<Provider>>();
        for (Map.Entry<ProviderId, List<Provider>> entry : grouped.entrySet()) {
            immutable.put(entry.getKey(), Collections.unmodifiableList(
                    new ArrayList<Provider>(entry.getValue())));
        }
        providers = Collections.unmodifiableMap(immutable);
        final TreeSet<ProviderId> duplicates = new TreeSet<ProviderId>(
                Objects.requireNonNull(rejectedDuplicates, "rejectedDuplicates"));
        this.rejectedDuplicates = Collections.unmodifiableSet(duplicates);
    }

    /**
     * Returns the canonical M21 provider certification inventory.
     *
     * @return Vault, LuckPerms, Citizens, ZNPCsPlus, DecentHolograms, AlessioDP, Grim,
     *         Vulcan, CloudNet, WorldEdit, FAWE, WorldGuard, SlimeWorldManager and Multiverse-Core identities
     */
    public static List<ProviderId> m21ProviderIds() {
        final List<ProviderId> result = new ArrayList<ProviderId>();
        result.add(ProviderId.of("zartra", "vault"));
        result.add(ProviderId.of("zartra", "luckperms"));
        result.add(ProviderId.of("zartra", "citizens"));
        result.add(ProviderId.of("zartra", "znpcsplus"));
        result.add(ProviderId.of("zartra", "decentholograms"));
        result.add(ProviderId.of("zartra", "alessiodp-parties"));
        result.add(ProviderId.of("zartra", "grim"));
        result.add(ProviderId.of("zartra", "vulcan"));
        result.add(ProviderId.of("zartra", "cloudnet"));
        result.add(ProviderId.of("zartra", "worldedit"));
        result.add(ProviderId.of("zartra", "fawe"));
        result.add(ProviderId.of("zartra", "worldguard"));
        result.add(ProviderId.of("zartra", "slimeworldmanager"));
        result.add(ProviderId.of("zartra", "multiverse-core"));
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    @Override public DefinitionId id() { return CHECK_ID; }

    @Override
    public PluginDoctor.Result inspect(final TaskContext context) {
        Objects.requireNonNull(context, "context");
        final List<Diagnostics.Field> evidence = new ArrayList<Diagnostics.Field>();
        final Map<State, Integer> counts = new EnumMap<State, Integer>(State.class);
        for (State state : State.values()) {
            counts.put(state, 0);
        }
        for (ProviderId id : required) {
            final State state = state(id);
            counts.put(state, counts.get(state) + 1);
            evidence.add(new Diagnostics.Field(
                    DefinitionId.of("zartra", "provider/" + id.path()),
                    state.name().toLowerCase(java.util.Locale.ROOT),
                    Diagnostics.Classification.PUBLIC));
        }
        final Health.Status health;
        final DefinitionId reason;
        if (counts.get(State.DUPLICATE) > 0 || counts.get(State.INCOMPATIBLE) > 0) {
            health = Health.Status.UNAVAILABLE;
            reason = FAILED;
        } else if (counts.get(State.ABSENT) > 0) {
            health = Health.Status.DEGRADED;
            reason = DEGRADED;
        } else {
            health = Health.Status.HEALTHY;
            reason = HEALTHY;
        }
        return new PluginDoctor.Result(CHECK_ID, health, reason, evidence);
    }

    /** @return compatibility state for one required provider */
    public State state(final ProviderId id) {
        Objects.requireNonNull(id, "id");
        final List<Provider> matches = providers.get(id);
        if (rejectedDuplicates.contains(id) || matches != null && matches.size() > 1) {
            return State.DUPLICATE;
        }
        if (matches == null || matches.isEmpty()) {
            return State.ABSENT;
        }
        final Provider.HealthStatus status = matches.get(0).health().status();
        return status == Provider.HealthStatus.DISABLED ? State.ABSENT
                : status == Provider.HealthStatus.UNAVAILABLE ? State.INCOMPATIBLE
                : State.PRESENT;
    }

    /** Provider compatibility result used by M21 certification and Plugin Doctor. */
    public enum State {
        /** Compatible provider binding is installed. */
        PRESENT,
        /** Optional provider is not installed. */
        ABSENT,
        /** Provider is installed but its API contract is unsupported or failed. */
        INCOMPATIBLE,
        /** More than one binding claims the same canonical provider identity. */
        DUPLICATE
    }
}
