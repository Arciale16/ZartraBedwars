package io.zartra.bedwars.arena.validation;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GeneratorTypeId;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaGenerator;
import io.zartra.bedwars.arena.model.ArenaLocation;
import io.zartra.bedwars.arena.model.ArenaNpc;
import io.zartra.bedwars.arena.model.ArenaRegion;
import io.zartra.bedwars.arena.model.ArenaTeam;
import io.zartra.bedwars.domain.team.TeamLayoutLimits;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Namespace for validation contracts, issues and the complete M07 rule set. */
public final class ArenaValidation {
    private static final DefinitionId MISSING_WORLD = code("missing_world");
    private static final DefinitionId MISSING_SPAWN = code("missing_spawn");
    private static final DefinitionId MISSING_BED = code("missing_bed");
    private static final DefinitionId MISSING_GENERATOR = code("missing_generator");
    private static final DefinitionId MISSING_NPC = code("missing_npc");
    private static final DefinitionId INVALID_REGION = code("invalid_region");
    private static final DefinitionId UNSAFE_SPAWN = code("unsafe_spawn");
    private static final DefinitionId BROKEN_REFERENCE = code("broken_reference");
    private static final DefinitionId COLLISION = code("location_collision");
    private static final DefinitionId INVALID_CAPACITY = code("invalid_capacity");
    private static final DefinitionId UNSUPPORTED_MODE = code("unsupported_mode");
    private static final DefinitionId INCONSISTENT_GROUP = code("inconsistent_group");

    private ArenaValidation() { throw new AssertionError("No instances"); }

    private static DefinitionId code(final String path) {
        return DefinitionId.of("zartra", "arena/validation/" + path);
    }

    /** Deterministic, thread-safe validation policy that performs no I/O. */
    public interface Validator {
        /** @return complete sorted validation report for one immutable bundle */
        Report validate(ArenaBundle bundle);
    }

    /** Complete built-in M07 validator for setup completeness, safety and references. */
    public static final class DefaultValidator implements Validator {
        private final ArenaValidationProfile profile;

        /** Creates the validator with the original starter prerequisites. */
        public DefaultValidator() {
            this(ArenaValidationProfile.standard());
        }

        /** Creates the validator with explicit typed prerequisites. */
        public DefaultValidator(final ArenaValidationProfile profile) {
            this.profile = Objects.requireNonNull(profile, "profile");
        }

        /** @return immutable prerequisite profile used by this validator */
        public ArenaValidationProfile profile() {
            return profile;
        }

        @Override public Report validate(final ArenaBundle bundle) {
            final ArenaBundle value = Objects.requireNonNull(bundle, "bundle");
            final List<Issue> issues = new ArrayList<Issue>();
            worldAndBounds(value, issues);
            teams(value, issues);
            generators(value, issues);
            npcs(value, issues);
            regions(value, issues);
            collisions(value, issues);
            return new Report(issues);
        }

        private static void worldAndBounds(final ArenaBundle bundle, final List<Issue> issues) {
            if (!bundle.arena().world().isPresent() || !bundle.arena().templateWorld().isPresent()) {
                issues.add(error(MISSING_WORLD, "world", "arena.validation.missing_world"));
            }
            if (!bundle.arena().waitingSpawn().isPresent()) {
                issues.add(error(MISSING_SPAWN, "waitingSpawn", "arena.validation.missing_waiting_spawn"));
            }
            if (!bundle.arena().spectatorSpawn().isPresent()) {
                issues.add(error(MISSING_SPAWN, "spectatorSpawn", "arena.validation.missing_spectator_spawn"));
            }
            if (!bundle.arena().bounds().isPresent()) {
                issues.add(error(INVALID_REGION, "bounds", "arena.validation.missing_bounds"));
            }
            if (bundle.arena().waitingSpawn().isPresent()) {
                safe(bundle, bundle.arena().waitingSpawn().get(), "waitingSpawn", issues);
            }
            if (bundle.arena().spectatorSpawn().isPresent()) {
                safe(bundle, bundle.arena().spectatorSpawn().get(), "spectatorSpawn", issues);
            }
        }

        private static void teams(final ArenaBundle bundle, final List<Issue> issues) {
            if (!bundle.arena().group().equals(bundle.map().group())) {
                issues.add(error(INCONSISTENT_GROUP, "group",
                        "arena.validation.inconsistent_group"));
            }
            if (bundle.arena().teams().size() < TeamLayoutLimits.MINIMUM_TEAM_COUNT
                    || bundle.arena().teams().size() > TeamLayoutLimits.MAXIMUM_TEAM_COUNT) {
                issues.add(error(INVALID_CAPACITY, "teams", "arena.validation.minimum_teams"));
            }
            final long capacity = (long) bundle.arena().teams().size()
                    * (long) bundle.arena().teamSize();
            if (capacity < bundle.arena().maximumPlayers()) {
                issues.add(error(INVALID_CAPACITY, "maximumPlayers", "arena.validation.team_capacity"));
            }
            if (bundle.arena().teamSize() < bundle.map().minimumTeamSize()
                    || bundle.arena().teamSize() > bundle.map().maximumTeamSize()) {
                issues.add(error(INVALID_CAPACITY, "teamSize",
                        "arena.validation.unsupported_team_size"));
            }
            for (DefinitionId mode : bundle.arena().modes()) {
                if (!bundle.map().supportedModes().contains(mode)) {
                    issues.add(error(UNSUPPORTED_MODE, "modes." + mode.path(),
                            "arena.validation.unsupported_mode"));
                }
            }
            for (ArenaTeam team : bundle.arena().teams()) {
                final String prefix = "teams." + team.id().path();
                if (!team.spawn().isPresent()) {
                    issues.add(error(MISSING_SPAWN, prefix + ".spawn", "arena.validation.missing_team_spawn"));
                } else {
                    safe(bundle, team.spawn().get(), prefix + ".spawn", issues);
                }
                if (!team.bed().isPresent()) {
                    issues.add(error(MISSING_BED, prefix + ".bed", "arena.validation.missing_team_bed"));
                } else {
                    safe(bundle, team.bed().get(), prefix + ".bed", issues);
                }
            }
        }

        private void generators(final ArenaBundle bundle, final List<Issue> issues) {
            final Set<GeneratorTypeId> sharedTypes = new HashSet<GeneratorTypeId>();
            final Set<DefinitionId> teamGenerators = new HashSet<DefinitionId>();
            final Set<DefinitionId> teams = teamIds(bundle);
            for (ArenaGenerator generator : bundle.arena().generators()) {
                if (generator.teamId().isPresent()) {
                    if (!teams.contains(generator.teamId().get())) {
                        issues.add(error(BROKEN_REFERENCE, "generators." + generator.id().path(),
                                "arena.validation.generator_team_reference"));
                    } else {
                        teamGenerators.add(generator.teamId().get());
                    }
                } else {
                    sharedTypes.add(generator.type());
                }
                safe(bundle, generator.location(), "generators." + generator.id().path(), issues);
            }
            for (GeneratorTypeId type : profile.requiredSharedGeneratorTypes()) {
                if (!sharedTypes.contains(type)) {
                    issues.add(error(MISSING_GENERATOR,
                            "generators.required." + type.namespace() + "/" + type.path(),
                            "arena.validation.missing_required_generator"));
                }
            }
            if (profile.teamGeneratorRequired()) {
                for (DefinitionId team : teams) {
                    if (teamGenerators.contains(team)) { continue; }
                    issues.add(error(MISSING_GENERATOR, "teams." + team.path() + ".generator",
                            "arena.validation.missing_team_generator"));
                }
            }
        }

        private void npcs(final ArenaBundle bundle, final List<Issue> issues) {
            final Set<DefinitionId> teams = teamIds(bundle);
            final Set<DefinitionId> shops = new HashSet<DefinitionId>();
            final Set<DefinitionId> upgrades = new HashSet<DefinitionId>();
            for (ArenaNpc npc : bundle.arena().npcs()) {
                if (npc.teamId().isPresent() && !teams.contains(npc.teamId().get())) {
                    issues.add(error(BROKEN_REFERENCE, "npcs." + npc.id().path(),
                            "arena.validation.npc_team_reference"));
                }
                if (npc.teamId().isPresent() && npc.kind() == ArenaNpc.Kind.SHOP) {
                    shops.add(npc.teamId().get());
                }
                if (npc.teamId().isPresent() && npc.kind() == ArenaNpc.Kind.TEAM_UPGRADE) {
                    upgrades.add(npc.teamId().get());
                }
                safe(bundle, npc.location(), "npcs." + npc.id().path(), issues);
            }
            for (DefinitionId team : teams) {
                if (profile.shopNpcRequired() && !shops.contains(team)) {
                    issues.add(error(MISSING_NPC, "teams." + team.path() + ".shop",
                            "arena.validation.missing_shop_npc"));
                }
                if (profile.upgradeNpcRequired() && !upgrades.contains(team)) {
                    issues.add(error(MISSING_NPC, "teams." + team.path() + ".upgrade",
                            "arena.validation.missing_upgrade_npc"));
                }
            }
        }

        private static void regions(final ArenaBundle bundle, final List<Issue> issues) {
            if (!bundle.arena().bounds().isPresent()) { return; }
            final ArenaRegion bounds = bundle.arena().bounds().get();
            for (ArenaRegion region : bundle.arena().protectedRegions()) {
                if (!bounds.contains(region.minimum()) || !bounds.contains(region.maximum())) {
                    issues.add(error(INVALID_REGION, "protectedRegions." + region.id().path(),
                            "arena.validation.protected_region_outside_bounds"));
                }
            }
        }

        private static void collisions(final ArenaBundle bundle, final List<Issue> issues) {
            final Set<String> occupied = new HashSet<String>();
            for (ArenaTeam team : bundle.arena().teams()) {
                if (team.spawn().isPresent()) { collision(team.spawn().get(), "team spawn", occupied, issues); }
                if (team.bed().isPresent()) { collision(team.bed().get(), "team bed", occupied, issues); }
            }
            for (ArenaGenerator generator : bundle.arena().generators()) {
                collision(generator.location(), "generator", occupied, issues);
            }
            for (ArenaNpc npc : bundle.arena().npcs()) {
                collision(npc.location(), "npc", occupied, issues);
            }
        }

        private static void collision(final ArenaLocation location, final String field,
                                      final Set<String> occupied, final List<Issue> issues) {
            final String key = Math.floor(location.x()) + ":" + Math.floor(location.y())
                    + ":" + Math.floor(location.z());
            if (!occupied.add(key)) {
                issues.add(error(COLLISION, field, "arena.validation.location_collision"));
            }
        }

        private static void safe(final ArenaBundle bundle, final ArenaLocation location,
                                 final String field, final List<Issue> issues) {
            final boolean inside = !bundle.arena().bounds().isPresent()
                    || bundle.arena().bounds().get().contains(location);
            if (!inside || location.y() <= bundle.arena().voidY()
                    || location.y() < bundle.arena().buildMinimumY()
                    || location.y() > bundle.arena().buildMaximumY()) {
                issues.add(error(UNSAFE_SPAWN, field, "arena.validation.unsafe_location"));
            }
        }

        private static Set<DefinitionId> teamIds(final ArenaBundle bundle) {
            final Set<DefinitionId> result = new HashSet<DefinitionId>();
            for (ArenaTeam team : bundle.arena().teams()) { result.add(team.id()); }
            return result;
        }
    }

    private static Issue error(final DefinitionId code, final String field,
                               final String messageKey) {
        return new Issue(code, Severity.ERROR, field, messageKey);
    }

    /** Immutable actionable validation issue. */
    public static final class Issue implements Comparable<Issue> {
        private final DefinitionId code;
        private final Severity severity;
        private final String field;
        private final String messageKey;
        /** Creates a localized-by-caller issue with no arbitrary user text. */
        public Issue(final DefinitionId code, final Severity severity, final String field,
                     final String messageKey) {
            this.code = Objects.requireNonNull(code, "code");
            this.severity = Objects.requireNonNull(severity, "severity");
            if (field == null || !field.matches("[A-Za-z0-9_./-]{1,192}")) {
                throw new IllegalArgumentException("field must be a bounded path");
            }
            if (messageKey == null || !messageKey.matches("[a-z0-9][a-z0-9_.-]{0,127}")) {
                throw new IllegalArgumentException("messageKey must be stable and localizable");
            }
            this.field = field;
            this.messageKey = messageKey;
        }
        /** @return stable issue identity */ public DefinitionId code() { return code; }
        /** @return severity */ public Severity severity() { return severity; }
        /** @return actionable field path */ public String field() { return field; }
        /** @return localization key */ public String messageKey() { return messageKey; }
        @Override public int compareTo(final Issue other) {
            final int fieldOrder = field.compareTo(Objects.requireNonNull(other, "other").field);
            return fieldOrder == 0 ? code.compareTo(other.code) : fieldOrder;
        }
    }

    /** Immutable complete report; ERROR issues block enable and save-as-known-good. */
    public static final class Report {
        private final List<Issue> issues;
        /** Creates a sorted report. */
        public Report(final List<Issue> issues) {
            final List<Issue> copy = new ArrayList<Issue>(Objects.requireNonNull(issues, "issues"));
            if (copy.contains(null)) { throw new IllegalArgumentException("issues contains null"); }
            Collections.sort(copy);
            this.issues = Collections.unmodifiableList(copy);
        }
        /** @return all issues in deterministic order */ public List<Issue> issues() { return issues; }
        /** @return whether no error blocks enable */
        public boolean mayEnable() {
            for (Issue issue : issues) { if (issue.severity() == Severity.ERROR) { return false; } }
            return true;
        }
    }

    /** Validation severity. */ public enum Severity { INFO, WARNING, ERROR }
}
