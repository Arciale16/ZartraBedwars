package io.zartra.bedwars.config.reload;

import io.zartra.bedwars.api.configuration.ConfigurationKey;
import io.zartra.bedwars.api.configuration.ReloadTarget;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.config.schema.ConfigurationModel.Document;
import io.zartra.bedwars.config.schema.ConfigurationModel.Issue;
import io.zartra.bedwars.config.schema.ConfigurationModel.OptionDefinition;
import io.zartra.bedwars.config.schema.ConfigurationModel.Schema;
import io.zartra.bedwars.config.schema.ConfigurationModel.ValidatedConfiguration;
import io.zartra.bedwars.config.schema.ConfigurationModel.ValidationReport;
import io.zartra.bedwars.config.schema.ConfigurationModel.Validator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Thread-safe all-or-nothing reload coordinator for one logical configuration schema. */
public final class TransactionalReloadService {
    private final Validator validator;
    private final Schema schema;
    private final Map<ReloadTarget, Participant> participants;
    private volatile ValidatedConfiguration active;

    /**
     * Creates a coordinator around an already validated last-known-good snapshot.
     *
     * @param validator strict validator
     * @param active initial published snapshot
     * @param participants unique targeted runtime participants
     */
    public TransactionalReloadService(final Validator validator,
                                      final ValidatedConfiguration active,
                                      final Collection<Participant> participants) {
        this.validator = Objects.requireNonNull(validator, "validator");
        this.active = Objects.requireNonNull(active, "active");
        this.schema = active.schema();
        final Map<ReloadTarget, Participant> collected = new EnumMap<ReloadTarget, Participant>(ReloadTarget.class);
        for (Participant participant : Objects.requireNonNull(participants, "participants")) {
            final Participant checked = Objects.requireNonNull(participant, "participant");
            if (collected.put(checked.target(), checked) != null) {
                throw new IllegalArgumentException("Duplicate reload participant target");
            }
        }
        this.participants = Collections.unmodifiableMap(collected);
    }

    /** @return current immutable last-known-good snapshot without blocking */
    public ValidatedConfiguration active() { return active; }

    /**
     * Validates and applies only changes belonging to the explicitly requested targets.
     *
     * @param document untrusted candidate
     * @param requested non-empty target allowlist
     * @return deterministic reload outcome
     */
    public synchronized ReloadReport reload(final Document document,
                                            final Set<ReloadTarget> requested) {
        Objects.requireNonNull(document, "document");
        if (requested == null || requested.isEmpty()) {
            throw new IllegalArgumentException("At least one reload target is required");
        }
        final Set<ReloadTarget> allowed = Collections.unmodifiableSet(EnumSet.copyOf(requested));
        final ValidationReport validation = validator.validate(schema, document);
        if (!validation.isValid()) {
            return ReloadReport.failure(Status.VALIDATION_FAILED,
                    Collections.<ConfigurationKey>emptySet(), validation.issues(),
                    Collections.<ConfigurationKey>emptySet(), null);
        }
        final ValidatedConfiguration candidate = validation.configuration().get();
        final Set<ConfigurationKey> changed = active.changedKeys(candidate);
        if (changed.isEmpty()) { return ReloadReport.success(Status.NO_CHANGES, changed); }
        final Set<ConfigurationKey> restart = new TreeSet<ConfigurationKey>();
        final Set<ReloadTarget> affected = EnumSet.noneOf(ReloadTarget.class);
        for (ConfigurationKey key : changed) {
            final OptionDefinition<?> definition = schema.definition(key).get();
            affected.add(definition.metadata().reloadTarget());
            if (definition.metadata().restartRequired()) { restart.add(key); }
        }
        if (!restart.isEmpty()) {
            return ReloadReport.failure(Status.RESTART_REQUIRED, changed,
                    Collections.<Issue>emptyList(), restart,
                    DefinitionId.of("zartra", "reload/restart_required"));
        }
        if (!allowed.containsAll(affected)) {
            return ReloadReport.failure(Status.TARGET_MISMATCH, changed,
                    Collections.<Issue>emptyList(), Collections.<ConfigurationKey>emptySet(),
                    DefinitionId.of("zartra", "reload/target_mismatch"));
        }
        final List<PreparedChange> prepared = new ArrayList<PreparedChange>();
        try {
            for (ReloadTarget target : affected) {
                final Participant participant = participants.get(target);
                if (participant != null) {
                    prepared.add(Objects.requireNonNull(participant.prepare(active, candidate),
                            "preparedChange"));
                }
            }
        } catch (RuntimeException exception) {
            final boolean restored = rollback(prepared);
            return ReloadReport.failure(Status.PREPARE_FAILED, changed,
                    Collections.<Issue>emptyList(), Collections.<ConfigurationKey>emptySet(),
                    DefinitionId.of("zartra", restored
                            ? "reload/prepare_failed" : "reload/rollback_failed"));
        }
        final List<PreparedChange> applied = new ArrayList<PreparedChange>();
        try {
            for (PreparedChange change : prepared) {
                applied.add(change);
                change.apply();
            }
        } catch (RuntimeException exception) {
            final boolean appliedRestored = rollback(applied);
            final boolean preparedRestored = rollbackRemaining(prepared, applied.size());
            return ReloadReport.failure(Status.APPLY_FAILED, changed,
                    Collections.<Issue>emptyList(), Collections.<ConfigurationKey>emptySet(),
                    DefinitionId.of("zartra", appliedRestored && preparedRestored
                            ? "reload/apply_failed" : "reload/rollback_failed"));
        }
        active = candidate;
        return ReloadReport.success(Status.APPLIED, changed);
    }

    private static boolean rollback(final List<PreparedChange> changes) {
        boolean restored = true;
        for (int index = changes.size() - 1; index >= 0; index--) {
            try { changes.get(index).rollback(); }
            catch (RuntimeException exception) { restored = false; }
        }
        return restored;
    }
    private static boolean rollbackRemaining(final List<PreparedChange> prepared, final int applied) {
        boolean restored = true;
        for (int index = prepared.size() - 1; index >= applied; index--) {
            try { prepared.get(index).rollback(); }
            catch (RuntimeException exception) { restored = false; }
        }
        return restored;
    }

    /** Target-specific transactional reload participant. */
    public interface Participant {
        /** @return unique target handled by the participant */ ReloadTarget target();
        /** @return reversible prepared change without publishing it */
        PreparedChange prepare(ValidatedConfiguration current, ValidatedConfiguration candidate);
    }

    /** Reversible change prepared before any participant applies. */
    public interface PreparedChange {
        /** Applies the prepared state. */ void apply();
        /** Restores state held before preparation/application. */ void rollback();
    }

    /** Reload outcome classification. */
    public enum Status {
        /** Candidate was atomically published. */ APPLIED,
        /** Candidate equals the active snapshot. */ NO_CHANGES,
        /** Strict validation rejected the candidate. */ VALIDATION_FAILED,
        /** At least one changed option is restart-only. */ RESTART_REQUIRED,
        /** Requested target set omitted an affected target. */ TARGET_MISMATCH,
        /** Participant preparation failed before publication. */ PREPARE_FAILED,
        /** Participant application failed and every prepared change was rolled back. */ APPLY_FAILED
    }

    /** Immutable targeted reload report. */
    public static final class ReloadReport {
        private final Status status;
        private final Set<ConfigurationKey> changed;
        private final List<Issue> issues;
        private final Set<ConfigurationKey> restart;
        private final DefinitionId failure;
        private ReloadReport(final Status status, final Collection<ConfigurationKey> changed,
                             final Collection<Issue> issues,
                             final Collection<ConfigurationKey> restart,
                             final DefinitionId failure) {
            this.status = status;
            this.changed = Collections.unmodifiableSet(new TreeSet<ConfigurationKey>(changed));
            this.issues = Collections.unmodifiableList(new ArrayList<Issue>(issues));
            this.restart = Collections.unmodifiableSet(new TreeSet<ConfigurationKey>(restart));
            this.failure = failure;
        }
        private static ReloadReport success(final Status status,
                                            final Collection<ConfigurationKey> changed) {
            return new ReloadReport(status, changed, Collections.<Issue>emptyList(),
                    Collections.<ConfigurationKey>emptySet(), null);
        }
        private static ReloadReport failure(final Status status,
                                            final Collection<ConfigurationKey> changed,
                                            final Collection<Issue> issues,
                                            final Collection<ConfigurationKey> restart,
                                            final DefinitionId failure) {
            return new ReloadReport(status, changed, issues, restart, failure);
        }
        /** @return outcome */ public Status status() { return status; }
        /** @return changed keys */ public Set<ConfigurationKey> changedKeys() { return changed; }
        /** @return validation issues */ public List<Issue> issues() { return issues; }
        /** @return restart-only changed keys */ public Set<ConfigurationKey> restartRequiredKeys() { return restart; }
        /** @return stable failure reason */ public Optional<DefinitionId> failure() { return Optional.ofNullable(failure); }
        /** @return whether the active snapshot changed */ public boolean wasApplied() { return status == Status.APPLIED; }
    }
}
