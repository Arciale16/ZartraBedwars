package io.zartra.bedwars.game.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Immutable match-team state with bounded membership and bed/elimination state. */
public final class TeamSnapshot {
    private static final Comparator<PlayerId> PLAYER_ORDER = new Comparator<PlayerId>() {
        @Override public int compare(final PlayerId left, final PlayerId right) {
            return left.toString().compareTo(right.toString());
        }
    };
    private final TeamDefinition definition;
    private final List<PlayerId> members;
    private final boolean bedPresent;
    private final boolean eliminated;

    /** Creates a validated immutable team snapshot. */
    public TeamSnapshot(final DefinitionId teamId, final int capacity,
                        final List<PlayerId> members, final boolean bedPresent,
                        final boolean eliminated) {
        this(TeamDefinition.compatibility(teamId, capacity), members, bedPresent, eliminated);
    }

    /** Creates a validated immutable team snapshot retaining arena-derived metadata. */
    public TeamSnapshot(final TeamDefinition definition, final List<PlayerId> members,
                        final boolean bedPresent, final boolean eliminated) {
        this.definition = Objects.requireNonNull(definition, "definition");
        final List<PlayerId> copy = new ArrayList<PlayerId>(
                Objects.requireNonNull(members, "members"));
        if (copy.contains(null) || new LinkedHashSet<PlayerId>(copy).size() != copy.size()
                || copy.size() > definition.capacity()) {
            throw new IllegalArgumentException("team membership must be unique and within capacity");
        }
        Collections.sort(copy, PLAYER_ORDER);
        this.members = Collections.unmodifiableList(copy);
        this.bedPresent = bedPresent;
        this.eliminated = eliminated;
    }

    /** @return empty team at match admission */
    public static TeamSnapshot empty(final DefinitionId id, final int capacity) {
        return new TeamSnapshot(id, capacity, Collections.<PlayerId>emptyList(), true, false);
    }
    /** @return empty team retaining complete arena-derived metadata */
    public static TeamSnapshot empty(final TeamDefinition definition) {
        return new TeamSnapshot(definition, Collections.<PlayerId>emptyList(), true, false);
    }
    /** @return complete immutable team definition */ public TeamDefinition definition() { return definition; }
    /** @return stable team identity */ public DefinitionId teamId() { return definition.id(); }
    /** @return configured display label */ public String displayName() { return definition.displayName(); }
    /** @return configured semantic color */ public DefinitionId color() { return definition.color(); }
    /** @return member capacity */ public int capacity() { return definition.capacity(); }
    /** @return deterministically ordered immutable members */ public List<PlayerId> members() { return members; }
    /** @return whether respawn bed remains */ public boolean bedPresent() { return bedPresent; }
    /** @return whether the team has no remaining active players */ public boolean eliminated() { return eliminated; }
    /** @return whether this player belongs to the team */ public boolean contains(final PlayerId player) { return members.contains(player); }
    /** @return whether another player may be admitted */ public boolean hasCapacity() { return members.size() < capacity(); }

    /** @return copy containing one additional player */
    public TeamSnapshot add(final PlayerId player) {
        Objects.requireNonNull(player, "player");
        if (members.contains(player)) { return this; }
        if (!hasCapacity() || eliminated) { throw new IllegalStateException("team cannot admit player"); }
        final List<PlayerId> copy = new ArrayList<PlayerId>(members);
        copy.add(player);
        return new TeamSnapshot(definition, copy, bedPresent, eliminated);
    }

    /** @return copy without the player */
    public TeamSnapshot remove(final PlayerId player) {
        final List<PlayerId> copy = new ArrayList<PlayerId>(members);
        if (!copy.remove(Objects.requireNonNull(player, "player"))) { return this; }
        return new TeamSnapshot(definition, copy, bedPresent, eliminated);
    }

    /** @return copy with the bed destroyed */
    public TeamSnapshot destroyBed() {
        return bedPresent ? new TeamSnapshot(definition, members, false, eliminated) : this;
    }

    /** @return copy marked eliminated */
    public TeamSnapshot eliminate() {
        return eliminated ? this : new TeamSnapshot(definition, members, bedPresent, true);
    }

    /** @return reusable empty state for a reset match */
    public TeamSnapshot reset() { return empty(definition); }

    @Override public int hashCode() {
        return Objects.hash(definition, members, bedPresent, eliminated);
    }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof TeamSnapshot)) { return false; }
        final TeamSnapshot that = (TeamSnapshot) other;
        return bedPresent == that.bedPresent && eliminated == that.eliminated
                && definition.equals(that.definition)
                && members.equals(that.members);
    }
}
