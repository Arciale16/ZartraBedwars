package io.zartra.bedwars.game.addon;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Validates, resolves and publishes state-specific hotbars with deterministic overrides. */
public final class HotbarPolicy {
    private HotbarPolicy() { }

    /** Resolves immutable layers in global, group, mode, arena order. */
    public static Loadout resolve(final State state, final Context context,
                                  final List<Layer> layers, final Set<String> permissions) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(permissions, "permissions");
        final List<Layer> eligible = new ArrayList<Layer>();
        for (Layer layer : Objects.requireNonNull(layers, "layers")) {
            if (layer == null) { throw new IllegalArgumentException("layer cannot be null"); }
            if (layer.matches(context)) { eligible.add(layer); }
        }
        Collections.sort(eligible, new Comparator<Layer>() {
            @Override public int compare(final Layer left, final Layer right) {
                final int rank = Integer.compare(left.scope.rank, right.scope.rank);
                return rank == 0 ? left.id.compareTo(right.id) : rank;
            }
        });
        final Map<Integer, Slot> slots = new HashMap<Integer, Slot>();
        for (Layer layer : eligible) {
            for (Slot slot : layer.slots) {
                if (slot.states.contains(state)
                        && (!slot.permission.isPresent()
                        || permissions.contains(slot.permission.get()))) {
                    slots.put(Integer.valueOf(slot.slot), slot);
                }
            }
        }
        return new Loadout(state, slots);
    }

    /** Registry publishing only complete validated definitions and retaining last-known-good. */
    public static final class Registry {
        private List<Layer> current = Collections.emptyList();
        private long revision;

        /** Atomically validates and publishes a new definition set. */
        public synchronized long publish(final List<Layer> candidate) {
            final List<Layer> copy = new ArrayList<Layer>(Objects.requireNonNull(candidate, "candidate"));
            if (copy.isEmpty() || copy.contains(null)) {
                throw new IllegalArgumentException("at least one non-null hotbar layer is required");
            }
            final Set<DefinitionId> ids = new HashSet<DefinitionId>();
            for (Layer layer : copy) {
                if (!ids.add(layer.id)) { throw new IllegalArgumentException("duplicate layer ID"); }
            }
            current = Collections.unmodifiableList(copy);
            revision++;
            return revision;
        }

        /** @return current immutable definitions */ public synchronized List<Layer> definitions() { return current; }
        /** @return publication revision */ public synchronized long revision() { return revision; }
    }

    /** All distinct hotbar states required by the product baseline. */
    public enum State {
        /** Network lobby. */ LOBBY,
        /** Arena waiting room. */ WAITING,
        /** Active match. */ PLAYING,
        /** Spectator state. */ SPECTATOR,
        /** Setup flow. */ SETUP,
        /** Staff tooling. */ STAFF,
        /** Private game. */ PRIVATE_GAME,
        /** Atlas review. */ ATLAS,
        /** Replay viewer. */ REPLAY,
        /** Hotbar editor preview. */ GUI_EDITOR
    }

    /** Deterministic override scopes from least to most specific. */
    public enum Scope {
        /** Default for every player. */ GLOBAL(0),
        /** Server group override. */ GROUP(1),
        /** Mode override. */ MODE(2),
        /** Arena override. */ ARENA(3);
        private final int rank;
        Scope(final int rank) { this.rank = rank; }
    }

    /** Immutable selection context. */
    public static final class Context {
        private final DefinitionId group;
        private final DefinitionId mode;
        private final DefinitionId arena;
        /** Creates a context; nullable dimensions mean no matching override. */
        public Context(final DefinitionId group, final DefinitionId mode,
                       final DefinitionId arena) {
            this.group = group;
            this.mode = mode;
            this.arena = arena;
        }
    }

    /** Immutable configured override layer. */
    public static final class Layer {
        private final DefinitionId id;
        private final Scope scope;
        private final DefinitionId selector;
        private final List<Slot> slots;
        /** Creates a validated layer. Global layers require no selector; others require one. */
        public Layer(final DefinitionId id, final Scope scope, final DefinitionId selector,
                     final List<Slot> slots) {
            this.id = Objects.requireNonNull(id, "id");
            this.scope = Objects.requireNonNull(scope, "scope");
            if ((scope == Scope.GLOBAL) != (selector == null)) {
                throw new IllegalArgumentException("selector presence does not match scope");
            }
            this.selector = selector;
            final List<Slot> copy = new ArrayList<Slot>(Objects.requireNonNull(slots, "slots"));
            if (copy.isEmpty() || copy.contains(null)) {
                throw new IllegalArgumentException("layer requires slots");
            }
            final Set<Integer> occupied = new HashSet<Integer>();
            for (Slot slot : copy) {
                if (!occupied.add(Integer.valueOf(slot.slot))) {
                    throw new IllegalArgumentException("duplicate slot in layer");
                }
            }
            this.slots = Collections.unmodifiableList(copy);
        }
        private boolean matches(final Context context) {
            switch (scope) {
                case GLOBAL: return true;
                case GROUP: return selector.equals(context.group);
                case MODE: return selector.equals(context.mode);
                case ARENA: return selector.equals(context.arena);
                default: return false;
            }
        }
    }

    /** Immutable typed hotbar slot and action identity. */
    public static final class Slot {
        private final int slot;
        private final PlayerStateSnapshot.Item item;
        private final DefinitionId action;
        private final Optional<String> permission;
        private final Set<State> states;
        /** Creates a slot available in one or more states. */
        public Slot(final int slot, final PlayerStateSnapshot.Item item,
                    final DefinitionId action, final String permission,
                    final Set<State> states) {
            if (slot < 0 || slot > 8) { throw new IllegalArgumentException("hotbar slot must be 0..8"); }
            this.slot = slot;
            this.item = Objects.requireNonNull(item, "item");
            this.action = Objects.requireNonNull(action, "action");
            if (permission != null && !permission.matches("[a-z0-9.*_-]{1,128}")) {
                throw new IllegalArgumentException("permission is malformed");
            }
            this.permission = Optional.ofNullable(permission);
            final Set<State> copy = EnumSet.copyOf(Objects.requireNonNull(states, "states"));
            if (copy.isEmpty()) { throw new IllegalArgumentException("slot requires a state"); }
            this.states = Collections.unmodifiableSet(copy);
        }
        /** @return zero-based slot */ public int slot() { return slot; }
        /** @return semantic immutable item */ public PlayerStateSnapshot.Item item() { return item; }
        /** @return stable action identity */ public DefinitionId action() { return action; }
    }

    /** Immutable resolved loadout used by a closed platform projector. */
    public static final class Loadout {
        private final State state;
        private final Map<Integer, Slot> slots;
        private Loadout(final State state, final Map<Integer, Slot> slots) {
            this.state = state;
            this.slots = Collections.unmodifiableMap(new HashMap<Integer, Slot>(slots));
        }
        /** @return selected lifecycle state */ public State state() { return state; }
        /** @return immutable owned slots */ public Map<Integer, Slot> slots() { return slots; }
        /** Resolves an interaction only when the exact currently-owned slot/action still matches. */
        public Optional<ActionIntent> interact(final int slot, final DefinitionId observedAction) {
            final Slot selected = slots.get(Integer.valueOf(slot));
            return selected != null && selected.action.equals(observedAction)
                    ? Optional.of(new ActionIntent(state, selected.action))
                    : Optional.<ActionIntent>empty();
        }
    }

    /** Typed intent dispatched to the later feature owning the selected action. */
    public static final class ActionIntent {
        private final State state;
        private final DefinitionId action;
        private ActionIntent(final State state, final DefinitionId action) {
            this.state = state;
            this.action = action;
        }
        /** @return source hotbar state */ public State state() { return state; }
        /** @return selected stable action */ public DefinitionId action() { return action; }
    }
}
