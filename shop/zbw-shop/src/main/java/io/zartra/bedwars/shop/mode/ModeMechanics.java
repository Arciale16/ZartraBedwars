package io.zartra.bedwars.shop.mode;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.mode.ModeFramework;
import io.zartra.bedwars.game.model.MatchSnapshot;
import io.zartra.bedwars.game.model.PlayerSession;
import io.zartra.bedwars.scripting.api.ScriptId;
import io.zartra.bedwars.shop.api.RotationContracts;
import io.zartra.bedwars.shop.api.ShopIds;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Match-local implementation of the M11 named-mode and addon mechanic boundary.
 *
 * <p>The class consumes immutable M08 snapshots and validated M10 mode definitions. It never
 * mutates the M08 state machine. World, inventory, packet and owner-thread work is represented by
 * bounded immutable {@link EffectIntent}s and committed by an injected transactional port.</p>
 */
public final class ModeMechanics {
    private ModeMechanics() {
        throw new AssertionError("No instances");
    }

    /** Mechanics delivered in M11.1 Phase 2. */
    public enum Feature {
        /** Gun-based ranged combat. */ ARMED,
        /** Weighted safe lucky-block outcomes. */ LUCKY_BLOCK,
        /** Atomic team-owned state rotation. */ SWAPPAGE,
        /** Seven selectable class-like abilities. */ ULTIMATE,
        /** Voidless fall and automatic defense behavior. */ VOIDLESS,
        /** Accelerated generators, defense and bridges. */ RUSH,
        /** Bed-upgrade and match-local redstone behavior. */ BED_STEAL,
        /** Team-colour normalization. */ COLOR_CHANGER,
        /** Bounded sponge placement effects. */ SPONGE,
        /** Bounded reversible tower structures. */ POPUP_TOWER,
        /** Deterministic local shop rotation. */ ITEM_ROTATION,
        /** Per-arena generator interval projection. */ PER_ARENA_GENERATOR
    }

    /** Original ranged weapon archetypes. */
    public enum WeaponArchetype {
        /** Close-range rapid-fire weapon. */ RAPID,
        /** Medium-range automatic weapon. */ AUTOMATIC,
        /** Precision single-shot weapon. */ PRECISION,
        /** High-impact slow-fire weapon. */ IMPACT,
        /** Spread-projectile weapon. */ SPREAD
    }

    /** Original Ultimate ability families. */
    public enum UltimateAbility {
        /** Leap mobility. */ KANGAROO,
        /** Dash and recall. */ SWORDSMAN,
        /** Team-only area healing. */ HEALER,
        /** Bounded slow/freeze control. */ FROZO,
        /** Rapid owned construction. */ BUILDER,
        /** Protected-region-aware explosive action. */ DEMOLITION,
        /** Duplication-safe resource grant. */ GATHERER
    }

    /** Platform-neutral effect families. */
    public enum EffectType {
        /** Server-authoritative projectile. */ PROJECTILE,
        /** Inventory or match-resource mutation. */ RESOURCE,
        /** Reversible owned block sequence. */ BLOCK_SEQUENCE,
        /** Bounded entity spawn. */ ENTITY,
        /** Temporary status effect. */ STATUS,
        /** Sound/particle/title feedback. */ FEEDBACK,
        /** Safe position or team transition. */ TRANSITION,
        /** Team-owned state mutation. */ TEAM_STATE
    }

    /** Immutable M10 binding with explicit M11 deferred requirements. */
    public static final class Binding {
        private final ModeFramework.Definition definition;
        private final Feature feature;
        private final Set<String> requirements;

        /** Creates a binding and proves every mechanic remains assigned to M11. */
        public Binding(final ModeFramework.Definition definition, final Feature feature,
                       final Collection<String> requirements) {
            this.definition = Objects.requireNonNull(definition, "definition");
            this.feature = Objects.requireNonNull(feature, "feature");
            final Set<String> expected = new LinkedHashSet<String>();
            for (String requirement : Objects.requireNonNull(requirements, "requirements")) {
                if (requirement == null || !requirement.matches("ZBW-ADDON-[0-9]{3}")) {
                    throw new IllegalArgumentException("invalid addon requirement");
                }
                if (!expected.add(requirement)) {
                    throw new IllegalArgumentException("duplicate addon requirement");
                }
            }
            if (expected.isEmpty()) {
                throw new IllegalArgumentException("binding requires addon requirements");
            }
            final Set<String> m11 = new HashSet<String>();
            for (ModeFramework.DeferredBinding deferred : definition.deferredBindings()) {
                if ("M11".equals(deferred.ownerMilestone())) {
                    m11.add(deferred.requirementId());
                }
            }
            if (!m11.containsAll(expected)) {
                throw new IllegalArgumentException("M10 definition lacks required M11 binding");
            }
            this.requirements = Collections.unmodifiableSet(expected);
        }

        /** @return M10 definition */ public ModeFramework.Definition definition() {
            return definition;
        }
        /** @return implemented mechanics family */ public Feature feature() {
            return feature;
        }
        /** @return exact M11 addon requirement IDs */ public Set<String> requirements() {
            return requirements;
        }
    }

    /** Complete immutable mechanic configuration loaded through the M11 configuration boundary. */
    public static final class Configuration {
        private final Map<Feature, Limits> limits;
        private final Map<DefinitionId, Weapon> weapons;
        private final List<LuckyOutcome> luckyOutcomes;
        private final Map<UltimateAbility, Ability> abilities;
        private final Set<DefinitionId> deniedColourItems;

        /** Creates a complete configuration for all Phase 2 mechanics. */
        public Configuration(final Map<Feature, Limits> limits,
                             final Collection<Weapon> weapons,
                             final Collection<LuckyOutcome> luckyOutcomes,
                             final Collection<Ability> abilities,
                             final Collection<DefinitionId> deniedColourItems) {
            final Map<Feature, Limits> checkedLimits = new EnumMap<Feature, Limits>(Feature.class);
            checkedLimits.putAll(Objects.requireNonNull(limits, "limits"));
            if (checkedLimits.size() != Feature.values().length || checkedLimits.containsValue(null)) {
                throw new IllegalArgumentException("every mechanic requires limits");
            }
            this.limits = Collections.unmodifiableMap(checkedLimits);
            this.weapons = immutableByWeaponId(weapons);
            if (this.weapons.size() < WeaponArchetype.values().length) {
                throw new IllegalArgumentException("all original weapon archetypes are required");
            }
            this.luckyOutcomes = immutableOutcomes(luckyOutcomes);
            final Map<UltimateAbility, Ability> checkedAbilities =
                    new EnumMap<UltimateAbility, Ability>(UltimateAbility.class);
            for (Ability ability : Objects.requireNonNull(abilities, "abilities")) {
                final Ability checked = Objects.requireNonNull(ability, "ability");
                if (checkedAbilities.put(checked.type(), checked) != null) {
                    throw new IllegalArgumentException("duplicate ultimate ability");
                }
            }
            if (checkedAbilities.size() != UltimateAbility.values().length) {
                throw new IllegalArgumentException("all seven ultimate abilities are required");
            }
            this.abilities = Collections.unmodifiableMap(checkedAbilities);
            this.deniedColourItems = immutableIds(deniedColourItems);
        }

        /** @return limits for one mechanic */ public Limits limits(final Feature feature) {
            return Objects.requireNonNull(limits.get(Objects.requireNonNull(feature, "feature")),
                    "limits");
        }
        /** @return required ranged weapon */ public Weapon weapon(final DefinitionId id) {
            final Weapon result = weapons.get(Objects.requireNonNull(id, "id"));
            if (result == null) {
                throw new IllegalArgumentException("unknown weapon");
            }
            return result;
        }
        /** @return ordered weighted outcomes */ public List<LuckyOutcome> luckyOutcomes() {
            return luckyOutcomes;
        }
        /** @return required ultimate definition */ public Ability ability(final UltimateAbility type) {
            return abilities.get(Objects.requireNonNull(type, "type"));
        }
        /** @return colour conversion denylist */ public Set<DefinitionId> deniedColourItems() {
            return deniedColourItems;
        }
    }

    /** Common bounded limits and cooldown. */
    public static final class Limits {
        private final boolean enabled;
        private final int activeObjects;
        private final int batchSize;
        private final Duration cooldown;

        /** Creates limits suitable for owner-thread application. */
        public Limits(final boolean enabled, final int activeObjects, final int batchSize,
                      final Duration cooldown) {
            if (activeObjects < 0 || activeObjects > 4096 || batchSize < 1 || batchSize > 256) {
                throw new IllegalArgumentException("mechanic limits outside bounds");
            }
            this.cooldown = Objects.requireNonNull(cooldown, "cooldown");
            if (cooldown.isNegative() || cooldown.compareTo(Duration.ofHours(1)) > 0) {
                throw new IllegalArgumentException("mechanic cooldown outside bounds");
            }
            this.enabled = enabled;
            this.activeObjects = activeObjects;
            this.batchSize = batchSize;
        }

        /** @return administratively enabled */ public boolean enabled() { return enabled; }
        /** @return per-match owned object cap */ public int activeObjects() { return activeObjects; }
        /** @return maximum effects in one atomic batch */ public int batchSize() { return batchSize; }
        /** @return server-side cooldown */ public Duration cooldown() { return cooldown; }
    }

    /** Original server-authoritative ranged weapon definition. */
    public static final class Weapon {
        private final DefinitionId id;
        private final WeaponArchetype archetype;
        private final int magazine;
        private final int reserve;
        private final int projectiles;
        private final int damage;
        private final int range;
        private final int accuracy;
        private final int falloff;
        private final int headshotMultiplier;
        private final Duration cadence;
        private final Duration reload;

        /** Creates one bounded original weapon definition. */
        public Weapon(final DefinitionId id, final WeaponArchetype archetype, final int magazine,
                      final int reserve, final int projectiles, final int damage, final int range,
                      final int accuracy, final int falloff, final int headshotMultiplier,
                      final Duration cadence, final Duration reload) {
            if (magazine < 1 || magazine > 256 || reserve < 0 || reserve > 4096
                    || projectiles < 1 || projectiles > 32 || damage < 1 || damage > 100
                    || range < 1 || range > 256 || accuracy < 1 || accuracy > 100
                    || falloff < 0 || falloff > 100 || headshotMultiplier < 100
                    || headshotMultiplier > 500) {
                throw new IllegalArgumentException("weapon values outside bounds");
            }
            this.id = Objects.requireNonNull(id, "id");
            this.archetype = Objects.requireNonNull(archetype, "archetype");
            this.cadence = boundedDuration(cadence, Duration.ofMillis(25), Duration.ofSeconds(10));
            this.reload = boundedDuration(reload, Duration.ofMillis(100), Duration.ofSeconds(30));
            this.magazine = magazine;
            this.reserve = reserve;
            this.projectiles = projectiles;
            this.damage = damage;
            this.range = range;
            this.accuracy = accuracy;
            this.falloff = falloff;
            this.headshotMultiplier = headshotMultiplier;
        }

        /** @return weapon ID */ public DefinitionId id() { return id; }
        /** @return original archetype */ public WeaponArchetype archetype() { return archetype; }
        /** @return magazine capacity */ public int magazine() { return magazine; }
        /** @return reserve ammunition */ public int reserve() { return reserve; }
        /** @return projectiles per shot */ public int projectiles() { return projectiles; }
        /** @return base damage */ public int damage() { return damage; }
        /** @return maximum validated range */ public int range() { return range; }
        /** @return configured accuracy percent */ public int accuracy() { return accuracy; }
        /** @return falloff percent */ public int falloff() { return falloff; }
        /** @return headshot multiplier percent */ public int headshotMultiplier() {
            return headshotMultiplier;
        }
        /** @return minimum shot cadence */ public Duration cadence() { return cadence; }
        /** @return reload duration */ public Duration reload() { return reload; }
    }

    /** One safe weighted lucky-block outcome. */
    public static final class LuckyOutcome {
        private final DefinitionId id;
        private final Kind kind;
        private final int weight;
        private final int effectCount;
        private final Duration cooldown;

        /** Creates a weighted outcome with hard effect limits. */
        public LuckyOutcome(final DefinitionId id, final Kind kind, final int weight,
                            final int effectCount, final Duration cooldown) {
            if (weight < 1 || weight > 1000000 || effectCount < 1 || effectCount > 64) {
                throw new IllegalArgumentException("lucky outcome outside bounds");
            }
            this.id = Objects.requireNonNull(id, "id");
            this.kind = Objects.requireNonNull(kind, "kind");
            this.cooldown = boundedDuration(cooldown, Duration.ZERO, Duration.ofHours(1));
            this.weight = weight;
            this.effectCount = effectCount;
        }

        /** Outcome families. */
        public enum Kind {
            /** Item/resource/buff. */ BENEFICIAL,
            /** Hostile/trap/debuff. */ HOSTILE,
            /** Owned entity. */ ENTITY,
            /** Reversible structure. */ STRUCTURE,
            /** Decorative feedback. */ FEEDBACK
        }

        /** @return outcome ID */ public DefinitionId id() { return id; }
        /** @return family */ public Kind kind() { return kind; }
        /** @return deterministic selection weight */ public int weight() { return weight; }
        /** @return bounded effects emitted */ public int effectCount() { return effectCount; }
        /** @return outcome cooldown */ public Duration cooldown() { return cooldown; }
    }

    /** One original Ultimate definition. */
    public static final class Ability {
        private final UltimateAbility type;
        private final Duration cooldown;
        private final int charges;
        private final int magnitude;
        private final int radius;

        /** Creates a server-authoritative ability definition. */
        public Ability(final UltimateAbility type, final Duration cooldown, final int charges,
                       final int magnitude, final int radius) {
            if (charges < 1 || charges > 64 || magnitude < 1 || magnitude > 1000
                    || radius < 0 || radius > 64) {
                throw new IllegalArgumentException("ability values outside bounds");
            }
            this.type = Objects.requireNonNull(type, "type");
            this.cooldown = boundedDuration(cooldown, Duration.ZERO, Duration.ofHours(1));
            this.charges = charges;
            this.magnitude = magnitude;
            this.radius = radius;
        }

        /** @return ability family */ public UltimateAbility type() { return type; }
        /** @return activation cooldown */ public Duration cooldown() { return cooldown; }
        /** @return maximum charges */ public int charges() { return charges; }
        /** @return bounded primary magnitude */ public int magnitude() { return magnitude; }
        /** @return bounded effect radius */ public int radius() { return radius; }
    }

    /** Immutable effect intent translated only by platform adapters. */
    public static final class EffectIntent {
        private final EffectType type;
        private final Feature feature;
        private final DefinitionId action;
        private final DefinitionId owner;
        private final int magnitude;
        private final Map<String, String> attributes;

        /** Creates a bounded secret-free intent. */
        public EffectIntent(final EffectType type, final Feature feature, final DefinitionId action,
                            final DefinitionId owner, final int magnitude,
                            final Map<String, String> attributes) {
            if (magnitude < 0 || magnitude > 1000000) {
                throw new IllegalArgumentException("effect magnitude outside bounds");
            }
            this.type = Objects.requireNonNull(type, "type");
            this.feature = Objects.requireNonNull(feature, "feature");
            this.action = Objects.requireNonNull(action, "action");
            this.owner = Objects.requireNonNull(owner, "owner");
            this.magnitude = magnitude;
            this.attributes = immutableAttributes(attributes);
        }

        /** @return effect family */ public EffectType type() { return type; }
        /** @return owning mechanic */ public Feature feature() { return feature; }
        /** @return semantic action */ public DefinitionId action() { return action; }
        /** @return player/team/match owner */ public DefinitionId owner() { return owner; }
        /** @return bounded primary magnitude */ public int magnitude() { return magnitude; }
        /** @return immutable adapter parameters */ public Map<String, String> attributes() {
            return attributes;
        }
    }

    /** Atomic platform mutation port; adapters must roll back a rejected batch. */
    public interface EffectPort {
        /** @return true only when the complete batch committed */
        boolean apply(MatchId matchId, IdempotencyKey key, List<EffectIntent> intents);
        /** Removes all objects owned by a completed/reset match. */ void cleanup(MatchId matchId);
    }

    /** Safe declarative hook into the disabled-by-default M11 scripting engine. */
    public interface ScriptHook {
        /** Invokes an allowlisted script without exposing platform objects. */
        boolean invoke(ScriptId scriptId, Map<String, String> input);
    }

    /** Secret-free non-blocking mechanics audit. */
    public interface AuditSink {
        /** Records one terminal operation. */ void record(AuditRecord record);
    }

    /** Immutable terminal audit entry. */
    public static final class AuditRecord {
        private final Feature feature;
        private final DefinitionId action;
        private final boolean success;
        private final String code;

        private AuditRecord(final Feature feature, final DefinitionId action,
                            final boolean success, final String code) {
            this.feature = feature;
            this.action = action;
            this.success = success;
            this.code = safeCode(code);
        }

        /** @return feature */ public Feature feature() { return feature; }
        /** @return semantic action */ public DefinitionId action() { return action; }
        /** @return success */ public boolean success() { return success; }
        /** @return stable result code */ public String code() { return code; }
    }

    /** Immutable operation result. */
    public static final class Result {
        private final boolean success;
        private final String code;
        private final List<EffectIntent> intents;

        private Result(final boolean success, final String code,
                       final Collection<EffectIntent> intents) {
            this.success = success;
            this.code = safeCode(code);
            this.intents = Collections.unmodifiableList(new ArrayList<EffectIntent>(intents));
        }

        /** @return whether the complete operation committed */ public boolean success() {
            return success;
        }
        /** @return stable result code */ public String code() { return code; }
        /** @return immutable committed intents */ public List<EffectIntent> intents() {
            return intents;
        }
    }

    /**
     * Stateful match-local runtime. Instances are confined to one application coordinator; all
     * public mutation methods are synchronized to make accidental concurrent calls deterministic.
     */
    public static final class Runtime {
        private final MatchId matchId;
        private final Map<Feature, Binding> bindings;
        private final Configuration configuration;
        private final EffectPort effects;
        private final ScriptHook scripts;
        private final AuditSink audit;
        private final Set<IdempotencyKey> completed = new LinkedHashSet<IdempotencyKey>();
        private final Map<String, Instant> cooldowns = new HashMap<String, Instant>();
        private final Map<String, WeaponState> weaponStates = new HashMap<String, WeaponState>();
        private final Map<PlayerId, UltimateState> ultimateStates =
                new HashMap<PlayerId, UltimateState>();
        private final Map<DefinitionId, BedState> bedStates =
                new HashMap<DefinitionId, BedState>();
        private MatchSnapshot snapshot;
        private int activeObjects;

        /** Creates a runtime from complete M10 bindings and M11 configuration. */
        public Runtime(final MatchId matchId, final Collection<Binding> bindings,
                       final Configuration configuration, final EffectPort effects,
                       final ScriptHook scripts, final AuditSink audit) {
            this.matchId = Objects.requireNonNull(matchId, "matchId");
            this.bindings = immutableBindings(bindings);
            this.configuration = Objects.requireNonNull(configuration, "configuration");
            this.effects = Objects.requireNonNull(effects, "effects");
            this.scripts = Objects.requireNonNull(scripts, "scripts");
            this.audit = Objects.requireNonNull(audit, "audit");
        }

        /** Observes M08 lifecycle state without changing it. Non-playing state clears local data. */
        public synchronized void observe(final MatchSnapshot value) {
            final MatchSnapshot checked = Objects.requireNonNull(value, "snapshot");
            if (!matchId.equals(checked.matchId())) {
                throw new IllegalArgumentException("snapshot belongs to another match");
            }
            snapshot = checked;
            if (checked.state() != MatchSnapshot.State.PLAYING) {
                cleanup();
            }
        }

        /** Fires an Armed weapon after cadence, ammo and server-side range validation. */
        public synchronized Result fire(final PlayerId player, final DefinitionId weaponId,
                                        final int targetDistance, final boolean headshot,
                                        final Instant now, final IdempotencyKey key) {
            final DefinitionId action = DefinitionId.of("zartra", "armed/fire");
            final Optional<Result> rejected = begin(Feature.ARMED, action, player, now, key);
            if (rejected.isPresent()) {
                return rejected.get();
            }
            final Weapon weapon = configuration.weapon(weaponId);
            if (targetDistance < 0 || targetDistance > weapon.range()) {
                return reject(Feature.ARMED, action, "target_out_of_range");
            }
            final String stateKey = player + "|" + weapon.id();
            WeaponState state = weaponStates.get(stateKey);
            if (state == null) {
                state = new WeaponState(weapon.magazine(), weapon.reserve(), Instant.EPOCH);
            }
            if (state.magazine == 0) {
                return reject(Feature.ARMED, action, "magazine_empty");
            }
            if (now.isBefore(state.nextAction)) {
                return reject(Feature.ARMED, action, "cadence");
            }
            final int scaled = headshot
                    ? weapon.damage() * weapon.headshotMultiplier() / 100 : weapon.damage();
            final int damage = scaled * Math.max(0, 100 - weapon.falloff() * targetDistance
                    / weapon.range()) / 100;
            final Map<String, String> attributes = attributes(
                    "weapon", weapon.id().toString(), "projectiles", weapon.projectiles(),
                    "accuracy", weapon.accuracy());
            final EffectIntent intent = new EffectIntent(EffectType.PROJECTILE, Feature.ARMED,
                    action, DefinitionId.of("player", player.toString()), damage, attributes);
            final Result result = commit(Feature.ARMED, action, player, now, key,
                    Collections.singletonList(intent));
            if (result.success()) {
                weaponStates.put(stateKey,
                        new WeaponState(state.magazine - 1, state.reserve, now.plus(weapon.cadence())));
            }
            return result;
        }

        /** Reloads an Armed weapon and supports server-side cancellation by not committing state. */
        public synchronized Result reload(final PlayerId player, final DefinitionId weaponId,
                                          final Instant now, final IdempotencyKey key) {
            final DefinitionId action = DefinitionId.of("zartra", "armed/reload");
            final Optional<Result> rejected = begin(Feature.ARMED, action, player, now, key);
            if (rejected.isPresent()) {
                return rejected.get();
            }
            final Weapon weapon = configuration.weapon(weaponId);
            final String stateKey = player + "|" + weapon.id();
            final WeaponState state = weaponStates.containsKey(stateKey)
                    ? weaponStates.get(stateKey)
                    : new WeaponState(weapon.magazine(), weapon.reserve(), Instant.EPOCH);
            final int required = weapon.magazine() - state.magazine;
            final int transferred = Math.min(required, state.reserve);
            if (transferred == 0) {
                return reject(Feature.ARMED, action, "reload_unavailable");
            }
            final EffectIntent intent = new EffectIntent(EffectType.FEEDBACK, Feature.ARMED,
                    action, DefinitionId.of("player", player.toString()), transferred,
                    Collections.singletonMap("duration_ms", Long.toString(weapon.reload().toMillis())));
            final Result result = commit(Feature.ARMED, action, player, now, key,
                    Collections.singletonList(intent));
            if (result.success()) {
                weaponStates.put(stateKey, new WeaponState(state.magazine + transferred,
                        state.reserve - transferred, now.plus(weapon.reload())));
            }
            return result;
        }

        /** Selects and applies a bounded LuckyBlock outcome from a deterministic seed. */
        public synchronized Result openLuckyBlock(final PlayerId player, final long seed,
                                                  final Instant now, final IdempotencyKey key) {
            final DefinitionId action = DefinitionId.of("zartra", "lucky-block/open");
            final Optional<Result> rejected = begin(Feature.LUCKY_BLOCK, action, player, now, key);
            if (rejected.isPresent()) {
                return rejected.get();
            }
            final LuckyOutcome outcome = selectOutcome(configuration.luckyOutcomes(), seed);
            final String outcomeKey = player + "|lucky|" + outcome.id();
            if (cooldowns.containsKey(outcomeKey) && now.isBefore(cooldowns.get(outcomeKey))) {
                return reject(Feature.LUCKY_BLOCK, action, "outcome_cooldown");
            }
            final EffectType type = outcome.kind() == LuckyOutcome.Kind.ENTITY
                    ? EffectType.ENTITY : outcome.kind() == LuckyOutcome.Kind.STRUCTURE
                    ? EffectType.BLOCK_SEQUENCE : outcome.kind() == LuckyOutcome.Kind.FEEDBACK
                    ? EffectType.FEEDBACK : outcome.kind() == LuckyOutcome.Kind.BENEFICIAL
                    ? EffectType.RESOURCE : EffectType.STATUS;
            final List<EffectIntent> intents = new ArrayList<EffectIntent>();
            for (int index = 0; index < outcome.effectCount(); index++) {
                intents.add(new EffectIntent(type, Feature.LUCKY_BLOCK, outcome.id(),
                        DefinitionId.of("player", player.toString()), index + 1,
                        Collections.<String, String>emptyMap()));
            }
            final boolean ownsObjects = outcome.kind() == LuckyOutcome.Kind.ENTITY
                    || outcome.kind() == LuckyOutcome.Kind.STRUCTURE;
            if (ownsObjects && activeObjects + outcome.effectCount()
                    > configuration.limits(Feature.LUCKY_BLOCK).activeObjects()) {
                return reject(Feature.LUCKY_BLOCK, action, "effect_limit");
            }
            final Result result = commit(Feature.LUCKY_BLOCK, action, player, now, key, intents);
            if (result.success()) {
                cooldowns.put(outcomeKey, now.plus(outcome.cooldown()));
                activeObjects += ownsObjects ? outcome.effectCount() : 0;
            }
            return result;
        }

        /** Atomically rotates every team and its owned match-local component state. */
        public synchronized Result swap(final Instant now, final IdempotencyKey key) {
            final DefinitionId action = DefinitionId.of("zartra", "swappage/rotate");
            final Optional<Result> rejected = begin(Feature.SWAPPAGE, action, null, now, key);
            if (rejected.isPresent()) {
                return rejected.get();
            }
            final List<DefinitionId> teams = new ArrayList<DefinitionId>();
            snapshot.teams().forEach(team -> teams.add(team.teamId()));
            Collections.sort(teams);
            final List<EffectIntent> intents = new ArrayList<EffectIntent>();
            for (int index = 0; index < teams.size(); index++) {
                final DefinitionId source = teams.get(index);
                final DefinitionId target = teams.get((index + 1) % teams.size());
                intents.add(new EffectIntent(EffectType.TEAM_STATE, Feature.SWAPPAGE, action,
                        source, 1, Collections.singletonMap("target_team", target.toString())));
            }
            return commit(Feature.SWAPPAGE, action, null, now, key, intents);
        }

        /** Selects an Ultimate while waiting/countdown; M08 retains lifecycle ownership. */
        public synchronized Result selectUltimate(final PlayerId player, final UltimateAbility type,
                                                  final Instant now, final IdempotencyKey key) {
            final DefinitionId action = DefinitionId.of("zartra", "ultimate/select");
            if (snapshot == null || snapshot.state() == MatchSnapshot.State.RESETTING
                    || snapshot.state() == MatchSnapshot.State.COMPLETING
                    || !hasPlayer(player)) {
                return reject(Feature.ULTIMATE, action, "invalid_state");
            }
            if (completed.contains(key)) {
                return reject(Feature.ULTIMATE, action, "duplicate");
            }
            final Ability ability = configuration.ability(type);
            ultimateStates.put(player, new UltimateState(type, ability.charges(), Instant.EPOCH));
            completed.add(key);
            return accept(Feature.ULTIMATE, action, Collections.<EffectIntent>emptyList());
        }

        /** Activates one selected Ultimate through bounded platform intents. */
        public synchronized Result activateUltimate(final PlayerId player, final Instant now,
                                                    final IdempotencyKey key) {
            final DefinitionId action = DefinitionId.of("zartra", "ultimate/activate");
            final Optional<Result> rejected = begin(Feature.ULTIMATE, action, player, now, key);
            if (rejected.isPresent()) {
                return rejected.get();
            }
            final UltimateState state = ultimateStates.get(player);
            if (state == null || state.charges == 0) {
                return reject(Feature.ULTIMATE, action, "ultimate_unavailable");
            }
            if (now.isBefore(state.readyAt)) {
                return reject(Feature.ULTIMATE, action, "ultimate_cooldown");
            }
            final Ability ability = configuration.ability(state.type);
            final EffectType effect = state.type == UltimateAbility.BUILDER
                    ? EffectType.BLOCK_SEQUENCE : state.type == UltimateAbility.GATHERER
                    ? EffectType.RESOURCE : state.type == UltimateAbility.HEALER
                    || state.type == UltimateAbility.FROZO ? EffectType.STATUS
                    : state.type == UltimateAbility.DEMOLITION ? EffectType.PROJECTILE
                    : EffectType.TRANSITION;
            final Map<String, String> values = attributes("ability", state.type.name(),
                    "radius", ability.radius(), "charges_remaining", state.charges - 1);
            final EffectIntent intent = new EffectIntent(effect, Feature.ULTIMATE, action,
                    DefinitionId.of("player", player.toString()), ability.magnitude(), values);
            final Result result = commit(Feature.ULTIMATE, action, player, now, key,
                    Collections.singletonList(intent));
            if (result.success()) {
                ultimateStates.put(player, new UltimateState(state.type, state.charges - 1,
                        now.plus(ability.cooldown())));
                if (effect == EffectType.BLOCK_SEQUENCE) {
                    activeObjects += ability.magnitude();
                }
            }
            return result;
        }

        /** Applies Voidless low-boundary recovery/damage or a configured automatic defense. */
        public synchronized Result voidless(final PlayerId player, final boolean defense,
                                            final int magnitude, final Instant now,
                                            final IdempotencyKey key) {
            return boundedEffect(Feature.VOIDLESS,
                    defense ? "voidless/defense" : "voidless/fall", player,
                    defense ? EffectType.BLOCK_SEQUENCE : EffectType.TRANSITION,
                    magnitude, now, key, defense);
        }

        /** Applies Rush bridge, automatic defense or accelerated generator intent. */
        public synchronized Result rush(final PlayerId player, final RushAction actionType,
                                        final int magnitude, final Instant now,
                                        final IdempotencyKey key) {
            final EffectType type = actionType == RushAction.GENERATOR
                    ? EffectType.RESOURCE : EffectType.BLOCK_SEQUENCE;
            return boundedEffect(Feature.RUSH, "rush/" + actionType.name().toLowerCase(), player,
                    type, magnitude, now, key, type == EffectType.BLOCK_SEQUENCE);
        }

        /** Rush mechanic actions. */
        public enum RushAction {
            /** Accelerated generator/tier override. */ GENERATOR,
            /** Automatic team-owned defense. */ DEFENSE,
            /** Animated expanding bridge. */ BRIDGE
        }

        /** Applies bounded reversible sponge feedback/effects. */
        public synchronized Result sponge(final PlayerId player, final int effectCount,
                                          final Instant now, final IdempotencyKey key) {
            return boundedEffect(Feature.SPONGE, "sponge/place", player, EffectType.FEEDBACK,
                    effectCount, now, key, true);
        }

        /** Applies a bounded team-coloured reversible pop-up tower. */
        public synchronized Result popupTower(final PlayerId player, final int blocks,
                                              final Instant now, final IdempotencyKey key) {
            return boundedEffect(Feature.POPUP_TOWER, "popup-tower/place", player,
                    EffectType.BLOCK_SEQUENCE, blocks, now, key, true);
        }

        /** Grants a signed BedSteal token and match-local redstone after an eligible bed break. */
        public synchronized Result bedDestroyed(final PlayerId player, final DefinitionId enemyTeam,
                                                final Instant now, final IdempotencyKey key) {
            final DefinitionId action = DefinitionId.of("zartra", "bed-steal/destroy");
            final Optional<Result> rejected = begin(Feature.BED_STEAL, action, player, now, key);
            if (rejected.isPresent()) {
                return rejected.get();
            }
            final DefinitionId ownTeam = teamOf(player);
            if (ownTeam.equals(Objects.requireNonNull(enemyTeam, "enemyTeam"))
                    || !snapshot.team(enemyTeam).isPresent()) {
                return reject(Feature.BED_STEAL, action, "invalid_enemy_bed");
            }
            final BedState current = bedStates.containsKey(ownTeam)
                    ? bedStates.get(ownTeam) : new BedState(0, 0, 0);
            final EffectIntent intent = new EffectIntent(EffectType.RESOURCE, Feature.BED_STEAL,
                    action, DefinitionId.of("player", player.toString()), 1,
                    Collections.singletonMap("resource", "redstone"));
            final Result result = commit(Feature.BED_STEAL, action, player, now, key,
                    Collections.singletonList(intent));
            if (result.success()) {
                bedStates.put(ownTeam,
                        new BedState(current.level, current.tokens + 1, current.redstone + 1));
            }
            return result;
        }

        /** Consumes one token at the owner's active bed and increases bed/max-health level. */
        public synchronized Result upgradeBed(final PlayerId player, final Instant now,
                                              final IdempotencyKey key) {
            final DefinitionId action = DefinitionId.of("zartra", "bed-steal/upgrade");
            final Optional<Result> rejected = begin(Feature.BED_STEAL, action, player, now, key);
            if (rejected.isPresent()) {
                return rejected.get();
            }
            final DefinitionId team = teamOf(player);
            final BedState current = bedStates.containsKey(team)
                    ? bedStates.get(team) : new BedState(0, 0, 0);
            if (current.tokens == 0 || current.level >= 64) {
                return reject(Feature.BED_STEAL, action, "bed_upgrade_unavailable");
            }
            final EffectIntent intent = new EffectIntent(EffectType.TEAM_STATE, Feature.BED_STEAL,
                    action, team, current.level + 1,
                    Collections.singletonMap("max_health_increment", "2"));
            final Result result = commit(Feature.BED_STEAL, action, player, now, key,
                    Collections.singletonList(intent));
            if (result.success()) {
                bedStates.put(team,
                        new BedState(current.level + 1, current.tokens - 1, current.redstone));
            }
            return result;
        }

        /** @return immutable current BedSteal team state */
        public synchronized BedState bedState(final DefinitionId team) {
            final BedState value = bedStates.get(Objects.requireNonNull(team, "team"));
            return value == null ? new BedState(0, 0, 0) : value;
        }

        /** Converts a supported item without changing amount or canonical metadata. */
        public synchronized ColouredItem convertColour(final ColouredItem item,
                                                       final DefinitionId teamColour) {
            Objects.requireNonNull(item, "item");
            Objects.requireNonNull(teamColour, "teamColour");
            if (!configuration.limits(Feature.COLOR_CHANGER).enabled()
                    || item.protectedItem() || configuration.deniedColourItems().contains(item.id())) {
                return item;
            }
            return item.withColour(teamColour);
        }

        /** Deterministically selects a local item rotation; M19/M20 later distribute it. */
        public synchronized RotationContracts.Snapshot rotate(
                final RotationContracts.Definition definition, final Instant now) {
            requireEnabled(Feature.ITEM_ROTATION);
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(now, "now");
            if (now.isBefore(definition.startsAt())
                    || definition.endsAt().isPresent() && !now.isBefore(definition.endsAt().get())) {
                throw new IllegalArgumentException("rotation is inactive");
            }
            final long elapsed = Duration.between(definition.startsAt(), now).toMillis();
            final long periodMillis = definition.period().toMillis();
            final long revision = elapsed / periodMillis + 1L;
            final Instant start = definition.startsAt().plusMillis((revision - 1L) * periodMillis);
            final List<RotationContracts.PoolEntry> pool =
                    new ArrayList<RotationContracts.PoolEntry>(definition.pool());
            pool.sort(Comparator.comparing(entry -> entry.itemId().toString()));
            final List<ShopIds.ItemId> selected = new ArrayList<ShopIds.ItemId>();
            final int offset = Math.floorMod((int) (revision ^ definition.id().hashCode()), pool.size());
            for (int index = 0; index < definition.slots(); index++) {
                selected.add(pool.get((offset + index) % pool.size()).itemId());
            }
            return new RotationContracts.Snapshot(definition.id(), revision, start,
                    start.plus(definition.period()), selected);
        }

        /** Executes a configured safe script hook for extension outcomes or diagnostics. */
        public synchronized boolean invokeScript(final Feature feature, final ScriptId script,
                                                 final Map<String, String> input) {
            requireEnabled(feature);
            return scripts.invoke(Objects.requireNonNull(script, "script"),
                    immutableAttributes(input));
        }

        /** Clears all match-local state and requests removal of every owned platform object. */
        public synchronized void cleanup() {
            completed.clear();
            cooldowns.clear();
            weaponStates.clear();
            ultimateStates.clear();
            bedStates.clear();
            activeObjects = 0;
            effects.cleanup(matchId);
        }

        private Result boundedEffect(final Feature feature, final String actionPath,
                                     final PlayerId player, final EffectType type,
                                     final int magnitude, final Instant now,
                                     final IdempotencyKey key, final boolean ownsObjects) {
            final DefinitionId action = DefinitionId.of("zartra", actionPath);
            final Optional<Result> rejected = begin(feature, action, player, now, key);
            if (rejected.isPresent()) {
                return rejected.get();
            }
            final Limits limits = configuration.limits(feature);
            if (magnitude < 1 || magnitude > limits.batchSize()
                    || ownsObjects && activeObjects + magnitude > limits.activeObjects()) {
                return reject(feature, action, "effect_limit");
            }
            final EffectIntent intent = new EffectIntent(type, feature, action,
                    DefinitionId.of("player", player.toString()), magnitude,
                    Collections.<String, String>emptyMap());
            final Result result = commit(feature, action, player, now, key,
                    Collections.singletonList(intent));
            if (result.success() && ownsObjects) {
                activeObjects += magnitude;
            }
            return result;
        }

        private Optional<Result> begin(final Feature feature, final DefinitionId action,
                                       final PlayerId player, final Instant now,
                                       final IdempotencyKey key) {
            Objects.requireNonNull(now, "now");
            Objects.requireNonNull(key, "key");
            requireEnabled(feature);
            if (snapshot == null || snapshot.state() != MatchSnapshot.State.PLAYING
                    || player != null && !hasPlayer(player)) {
                return Optional.of(reject(feature, action, "invalid_state"));
            }
            if (completed.contains(key)) {
                return Optional.of(reject(feature, action, "duplicate"));
            }
            final String cooldownKey = feature + "|" + (player == null ? matchId : player);
            if (cooldowns.containsKey(cooldownKey) && now.isBefore(cooldowns.get(cooldownKey))) {
                return Optional.of(reject(feature, action, "cooldown"));
            }
            return Optional.empty();
        }

        private Result commit(final Feature feature, final DefinitionId action,
                              final PlayerId player, final Instant now,
                              final IdempotencyKey key, final List<EffectIntent> intents) {
            final Limits limits = configuration.limits(feature);
            if (intents.size() > limits.batchSize()) {
                return reject(feature, action, "batch_limit");
            }
            if (!effects.apply(matchId, key, Collections.unmodifiableList(
                    new ArrayList<EffectIntent>(intents)))) {
                return reject(feature, action, "platform_rejected");
            }
            completed.add(key);
            if (completed.size() > 8192) {
                completed.remove(completed.iterator().next());
            }
            final String cooldownKey = feature + "|" + (player == null ? matchId : player);
            cooldowns.put(cooldownKey, now.plus(limits.cooldown()));
            return accept(feature, action, intents);
        }

        private Result accept(final Feature feature, final DefinitionId action,
                              final Collection<EffectIntent> intents) {
            final Result result = new Result(true, "ok", intents);
            audit.record(new AuditRecord(feature, action, true, "ok"));
            return result;
        }

        private Result reject(final Feature feature, final DefinitionId action, final String code) {
            final Result result = new Result(false, code, Collections.<EffectIntent>emptyList());
            audit.record(new AuditRecord(feature, action, false, code));
            return result;
        }

        private void requireEnabled(final Feature feature) {
            if (!configuration.limits(feature).enabled() || !bindings.containsKey(feature)) {
                throw new IllegalStateException("mechanic is disabled or unbound");
            }
        }

        private boolean hasPlayer(final PlayerId player) {
            return snapshot != null && snapshot.session(player).isPresent();
        }

        private DefinitionId teamOf(final PlayerId player) {
            final Optional<PlayerSession> session = snapshot.session(player);
            if (!session.isPresent()) {
                throw new IllegalArgumentException("player is not in the match");
            }
            return session.get().teamId();
        }
    }

    /** Immutable BedSteal match-local team state. */
    public static final class BedState {
        private final int level;
        private final int tokens;
        private final int redstone;

        private BedState(final int level, final int tokens, final int redstone) {
            this.level = level;
            this.tokens = tokens;
            this.redstone = redstone;
        }

        /** @return bed protection/health level */ public int level() { return level; }
        /** @return signed unspent upgrade tokens */ public int tokens() { return tokens; }
        /** @return match-local redstone tender */ public int redstone() { return redstone; }
    }

    /** Immutable item boundary used by colour conversion adapters. */
    public static final class ColouredItem {
        private final DefinitionId id;
        private final DefinitionId colour;
        private final int amount;
        private final boolean protectedItem;
        private final Map<String, String> metadata;

        /** Creates an immutable canonical item snapshot. */
        public ColouredItem(final DefinitionId id, final DefinitionId colour, final int amount,
                            final boolean protectedItem, final Map<String, String> metadata) {
            if (amount < 1 || amount > 64) {
                throw new IllegalArgumentException("item amount outside bounds");
            }
            this.id = Objects.requireNonNull(id, "id");
            this.colour = Objects.requireNonNull(colour, "colour");
            this.amount = amount;
            this.protectedItem = protectedItem;
            this.metadata = immutableAttributes(metadata);
        }

        /** @return canonical item ID */ public DefinitionId id() { return id; }
        /** @return semantic colour ID */ public DefinitionId colour() { return colour; }
        /** @return unchanged amount */ public int amount() { return amount; }
        /** @return signed/model/cosmetic/deny conversion flag */ public boolean protectedItem() {
            return protectedItem;
        }
        /** @return unchanged canonical metadata */ public Map<String, String> metadata() {
            return metadata;
        }
        /** @return equivalent item with only semantic colour changed */
        public ColouredItem withColour(final DefinitionId value) {
            return colour.equals(value) ? this
                    : new ColouredItem(id, value, amount, protectedItem, metadata);
        }
    }

    private static final class WeaponState {
        private final int magazine;
        private final int reserve;
        private final Instant nextAction;

        private WeaponState(final int magazine, final int reserve, final Instant nextAction) {
            this.magazine = magazine;
            this.reserve = reserve;
            this.nextAction = nextAction;
        }
    }

    private static final class UltimateState {
        private final UltimateAbility type;
        private final int charges;
        private final Instant readyAt;

        private UltimateState(final UltimateAbility type, final int charges, final Instant readyAt) {
            this.type = type;
            this.charges = charges;
            this.readyAt = readyAt;
        }
    }

    private static Map<Feature, Binding> immutableBindings(final Collection<Binding> values) {
        final Map<Feature, Binding> result = new EnumMap<Feature, Binding>(Feature.class);
        for (Binding binding : Objects.requireNonNull(values, "bindings")) {
            final Binding checked = Objects.requireNonNull(binding, "binding");
            if (result.put(checked.feature(), checked) != null) {
                throw new IllegalArgumentException("duplicate mechanic binding");
            }
        }
        if (result.size() != Feature.values().length) {
            throw new IllegalArgumentException("every Phase 2 mechanic requires an M10 binding");
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<DefinitionId, Weapon> immutableByWeaponId(
            final Collection<Weapon> values) {
        final Map<DefinitionId, Weapon> result = new LinkedHashMap<DefinitionId, Weapon>();
        for (Weapon weapon : Objects.requireNonNull(values, "weapons")) {
            final Weapon checked = Objects.requireNonNull(weapon, "weapon");
            if (result.put(checked.id(), checked) != null) {
                throw new IllegalArgumentException("duplicate weapon ID");
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<LuckyOutcome> immutableOutcomes(final Collection<LuckyOutcome> values) {
        final List<LuckyOutcome> result = new ArrayList<LuckyOutcome>();
        final Set<DefinitionId> ids = new HashSet<DefinitionId>();
        long total = 0;
        for (LuckyOutcome outcome : Objects.requireNonNull(values, "luckyOutcomes")) {
            final LuckyOutcome checked = Objects.requireNonNull(outcome, "outcome");
            if (!ids.add(checked.id())) {
                throw new IllegalArgumentException("duplicate lucky outcome");
            }
            total += checked.weight();
            if (total > 10000000L) {
                throw new IllegalArgumentException("lucky outcome weight sum exceeds bounds");
            }
            result.add(checked);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("lucky outcomes are required");
        }
        result.sort(Comparator.comparing(LuckyOutcome::id));
        return Collections.unmodifiableList(result);
    }

    private static LuckyOutcome selectOutcome(final List<LuckyOutcome> outcomes, final long seed) {
        long total = 0;
        for (LuckyOutcome outcome : outcomes) {
            total += outcome.weight();
        }
        long selected = Math.floorMod(seed ^ 0x5DEECE66DL, total);
        for (LuckyOutcome outcome : outcomes) {
            if (selected < outcome.weight()) {
                return outcome;
            }
            selected -= outcome.weight();
        }
        throw new IllegalStateException("weighted selection invariant failed");
    }

    private static Set<DefinitionId> immutableIds(final Collection<DefinitionId> values) {
        final Set<DefinitionId> result = new LinkedHashSet<DefinitionId>();
        for (DefinitionId value : Objects.requireNonNull(values, "values")) {
            if (!result.add(Objects.requireNonNull(value, "value"))) {
                throw new IllegalArgumentException("duplicate definition ID");
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Map<String, String> immutableAttributes(final Map<String, String> values) {
        final Map<String, String> result = new LinkedHashMap<String, String>();
        final List<Map.Entry<String, String>> entries =
                new ArrayList<Map.Entry<String, String>>(
                        Objects.requireNonNull(values, "attributes").entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        for (Map.Entry<String, String> entry : entries) {
            final String key = bounded(entry.getKey(), 64, "attribute key");
            final String value = bounded(entry.getValue(), 512, "attribute value");
            if (result.put(key, value) != null || result.size() > 64) {
                throw new IllegalArgumentException("duplicate or excessive attributes");
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> attributes(final String firstKey, final Object firstValue,
                                                  final String secondKey, final Object secondValue,
                                                  final String thirdKey, final Object thirdValue) {
        final Map<String, String> result = new LinkedHashMap<String, String>();
        result.put(firstKey, String.valueOf(firstValue));
        result.put(secondKey, String.valueOf(secondValue));
        result.put(thirdKey, String.valueOf(thirdValue));
        return result;
    }

    private static Duration boundedDuration(final Duration value, final Duration minimum,
                                            final Duration maximum) {
        final Duration checked = Objects.requireNonNull(value, "duration");
        if (checked.compareTo(minimum) < 0 || checked.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("duration outside bounds");
        }
        return checked;
    }

    private static String bounded(final String value, final int maximum, final String label) {
        if (value == null || value.isEmpty() || value.length() > maximum
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0
                || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(label + " is malformed");
        }
        return value;
    }

    private static String safeCode(final String value) {
        if (value == null || !value.matches("[a-z0-9_.-]{1,64}")) {
            throw new IllegalArgumentException("invalid result code");
        }
        return value;
    }
}
