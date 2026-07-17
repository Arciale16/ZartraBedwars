package io.zartra.bedwars.game.selector;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.game.mode.ModeFramework.Layout;
import io.zartra.bedwars.game.mode.ModeFramework.ModeId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Deterministic filtering, searching, paging and stale-selection validation. */
public final class SelectorFramework {
    private SelectorFramework() { throw new AssertionError("No instances"); }

    /** Runtime lifecycle exposed to selection without leaking M08 internals. */
    public enum Lifecycle { /** Waiting for players. */ WAITING, /** Countdown is active. */ COUNTDOWN, /** Match already running. */ PLAYING, /** Reset in progress. */ RESETTING, /** Recovery blocks admission. */ RECOVERING }

    /** Typed exclusion reason safe for localization and diagnostics. */
    public enum Exclusion { /** Candidate is joinable. */ NONE, /** Disabled definition. */ DISABLED, /** Health gate failed. */ UNHEALTHY, /** World not ready. */ WORLD_UNAVAILABLE, /** Revision is not current. */ STALE, /** Mode mismatch. */ MODE_INCOMPATIBLE, /** Layout mismatch. */ LAYOUT_INCOMPATIBLE, /** No capacity. */ FULL, /** Lifecycle is closed. */ CLOSED }

    /** Immutable selector candidate derived only from typed arena/runtime facts. */
    public static final class Candidate {
        private final ArenaId arenaId;
        private final long definitionRevision;
        private final DefinitionId mapId;
        private final ModeId modeId;
        private final Layout layout;
        private final MessageKey displayName;
        private final boolean enabled;
        private final boolean healthy;
        private final boolean worldReady;
        private final Lifecycle lifecycle;
        private final int players;
        private final int reserved;
        private final int order;
        private final Set<DefinitionId> tags;
        /** Creates one validated typed candidate. */
        public Candidate(final ArenaId arenaId, final long definitionRevision,
                         final DefinitionId mapId, final ModeId modeId, final Layout layout,
                         final MessageKey displayName, final boolean enabled, final boolean healthy,
                         final boolean worldReady, final Lifecycle lifecycle, final int players,
                         final int reserved, final int order, final Collection<DefinitionId> tags) {
            this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
            if (definitionRevision < 0L) { throw new IllegalArgumentException("revision must not be negative"); }
            this.definitionRevision = definitionRevision;
            this.mapId = Objects.requireNonNull(mapId, "mapId");
            this.modeId = Objects.requireNonNull(modeId, "modeId");
            this.layout = Objects.requireNonNull(layout, "layout");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            if (players < 0 || reserved < 0 || players + reserved > layout.totalCapacity()) {
                throw new IllegalArgumentException("invalid candidate population");
            }
            this.enabled = enabled;
            this.healthy = healthy;
            this.worldReady = worldReady;
            this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
            this.players = players;
            this.reserved = reserved;
            this.order = order;
            final Set<DefinitionId> copy = new LinkedHashSet<DefinitionId>();
            for (DefinitionId tag : Objects.requireNonNull(tags, "tags")) { copy.add(Objects.requireNonNull(tag, "tag")); }
            this.tags = Collections.unmodifiableSet(copy);
        }
        /** @return arena identity */ public ArenaId arenaId() { return arenaId; }
        /** @return definition revision */ public long definitionRevision() { return definitionRevision; }
        /** @return map identity */ public DefinitionId mapId() { return mapId; }
        /** @return mode identity */ public ModeId modeId() { return modeId; }
        /** @return layout */ public Layout layout() { return layout; }
        /** @return localized label */ public MessageKey displayName() { return displayName; }
        /** @return current players */ public int players() { return players; }
        /** @return pending reservations */ public int reserved() { return reserved; }
        /** @return configured order */ public int order() { return order; }
        /** @return configured tags */ public Set<DefinitionId> tags() { return tags; }
        /** @return currently available slots */ public int available() { return layout.totalCapacity() - players - reserved; }
        /** @return exclusion for requested mode/layout and minimum capacity */
        public Exclusion exclusion(final ModeId requestedMode, final DefinitionId requestedLayout,
                                   final int requiredCapacity, final long minimumRevision) {
            if (!enabled) { return Exclusion.DISABLED; }
            if (!healthy) { return Exclusion.UNHEALTHY; }
            if (!worldReady) { return Exclusion.WORLD_UNAVAILABLE; }
            if (definitionRevision < minimumRevision) { return Exclusion.STALE; }
            if (requestedMode != null && !modeId.equals(requestedMode)) { return Exclusion.MODE_INCOMPATIBLE; }
            if (requestedLayout != null && !layout.id().equals(requestedLayout)) { return Exclusion.LAYOUT_INCOMPATIBLE; }
            if (lifecycle != Lifecycle.WAITING && lifecycle != Lifecycle.COUNTDOWN) { return Exclusion.CLOSED; }
            return available() < requiredCapacity ? Exclusion.FULL : Exclusion.NONE;
        }
    }

    /** Immutable bounded selector query shared by command and GUI surfaces. */
    public static final class Query {
        private final ModeId mode;
        private final DefinitionId layout;
        private final DefinitionId map;
        private final DefinitionId tag;
        private final String search;
        private final int requiredCapacity;
        private final long minimumRevision;
        private final int page;
        private final int pageSize;
        private final Order order;
        /** Creates one validated selector query. */
        public Query(final ModeId mode, final DefinitionId layout, final DefinitionId map,
                     final DefinitionId tag, final String search, final int requiredCapacity,
                     final long minimumRevision, final int page, final int pageSize,
                     final Order order) {
            this.mode = mode;
            this.layout = layout;
            this.map = map;
            this.tag = tag;
            if (search == null || search.length() > 128 || search.indexOf('\r') >= 0 || search.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("invalid search");
            }
            if (requiredCapacity < 1 || requiredCapacity > 256 || minimumRevision < 0L
                    || page < 0 || page > 10000 || pageSize < 1 || pageSize > 45) {
                throw new IllegalArgumentException("invalid selector bounds");
            }
            this.search = search.trim().toLowerCase(Locale.ROOT);
            this.requiredCapacity = requiredCapacity;
            this.minimumRevision = minimumRevision;
            this.page = page;
            this.pageSize = pageSize;
            this.order = Objects.requireNonNull(order, "order");
        }
        /** @return requested mode */ public Optional<ModeId> mode() { return Optional.ofNullable(mode); }
        /** @return requested layout */ public Optional<DefinitionId> layout() { return Optional.ofNullable(layout); }
        /** @return requested map */ public Optional<DefinitionId> map() { return Optional.ofNullable(map); }
        /** @return requested tag */ public Optional<DefinitionId> tag() { return Optional.ofNullable(tag); }
        /** @return normalized search */ public String search() { return search; }
        /** @return required capacity */ public int requiredCapacity() { return requiredCapacity; }
        /** @return minimum candidate revision */ public long minimumRevision() { return minimumRevision; }
        /** @return zero-based page */ public int page() { return page; }
        /** @return page size */ public int pageSize() { return pageSize; }
        /** @return ordering */ public Order order() { return order; }
    }

    /** Deterministic candidate ordering. */
    public enum Order { /** Configured weight then identity. */ CONFIGURED, /** Most available capacity first. */ CAPACITY, /** Stable arena identity. */ IDENTITY }

    /** Page loading states shared with M09 presentation. */
    public enum Status { /** Provider not finished. */ LOADING, /** Candidates present. */ READY, /** No candidates. */ EMPTY, /** Provider failed safely. */ ERROR }

    /** Immutable filtered page plus exclusion diagnostics. */
    public static final class Page {
        private final long viewRevision;
        private final Status status;
        private final int page;
        private final int pageCount;
        private final List<Candidate> candidates;
        private final List<Excluded> exclusions;
        private final MessageKey message;
        private Page(final long viewRevision, final Status status, final int page, final int pageCount,
                     final Collection<Candidate> candidates, final Collection<Excluded> exclusions,
                     final MessageKey message) {
            this.viewRevision = viewRevision;
            this.status = status;
            this.page = page;
            this.pageCount = pageCount;
            this.candidates = Collections.unmodifiableList(new ArrayList<Candidate>(candidates));
            this.exclusions = Collections.unmodifiableList(new ArrayList<Excluded>(exclusions));
            this.message = message;
        }
        /** @return view revision */ public long viewRevision() { return viewRevision; }
        /** @return state */ public Status status() { return status; }
        /** @return zero-based page */ public int page() { return page; }
        /** @return at least one page */ public int pageCount() { return pageCount; }
        /** @return immutable visible candidates */ public List<Candidate> candidates() { return candidates; }
        /** @return immutable exclusion evidence */ public List<Excluded> exclusions() { return exclusions; }
        /** @return localized state message */ public MessageKey message() { return message; }
    }

    /** Candidate exclusion evidence without player-private data. */
    public static final class Excluded {
        private final ArenaId arenaId;
        private final Exclusion reason;
        private Excluded(final ArenaId arenaId, final Exclusion reason) { this.arenaId = arenaId;
        this.reason = reason;
        }
        /** @return arena identity */ public ArenaId arenaId() { return arenaId; }
        /** @return exclusion reason */ public Exclusion reason() { return reason; }
    }

    /** Revision-bound selection token used for stale-view rejection. */
    public static final class Selection {
        private final ArenaId arenaId;
        private final long definitionRevision;
        private final long viewRevision;
        /** Creates one selection token. */
        public Selection(final ArenaId arenaId, final long definitionRevision, final long viewRevision) {
            this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
            if (definitionRevision < 0L || viewRevision < 0L) { throw new IllegalArgumentException("revision must not be negative"); }
            this.definitionRevision = definitionRevision;
            this.viewRevision = viewRevision;
        }
        /** @return arena */ public ArenaId arenaId() { return arenaId; }
        /** @return definition revision */ public long definitionRevision() { return definitionRevision; }
        /** @return selector view revision */ public long viewRevision() { return viewRevision; }
    }

    /** Stateless deterministic selection engine. */
    public static final class Service {
        /** @return filtered page with complete exclusion evidence */
        public Page page(final Collection<Candidate> source, final Query query, final long viewRevision) {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(query, "query");
            if (viewRevision < 0L) { throw new IllegalArgumentException("viewRevision must not be negative"); }
            final List<Candidate> accepted = new ArrayList<Candidate>();
            final List<Excluded> excluded = new ArrayList<Excluded>();
            for (Candidate candidate : source) {
                final Candidate checked = Objects.requireNonNull(candidate, "candidate");
                Exclusion reason = checked.exclusion(query.mode().orElse(null), query.layout().orElse(null),
                        query.requiredCapacity(), query.minimumRevision());
                if (reason == Exclusion.NONE && query.map().isPresent() && !query.map().get().equals(checked.mapId())) {
                    reason = Exclusion.LAYOUT_INCOMPATIBLE;
                }
                if (reason == Exclusion.NONE && query.tag().isPresent() && !checked.tags().contains(query.tag().get())) {
                    reason = Exclusion.LAYOUT_INCOMPATIBLE;
                }
                final String identityText = checked.arenaId().toString().toLowerCase(Locale.ROOT);
                final String mapText = checked.mapId().toString().toLowerCase(Locale.ROOT);
                if (reason == Exclusion.NONE && !query.search().isEmpty()
                        && !identityText.contains(query.search()) && !mapText.contains(query.search())) {
                    reason = Exclusion.LAYOUT_INCOMPATIBLE;
                }
                if (reason == Exclusion.NONE) { accepted.add(checked); }
                else { excluded.add(new Excluded(checked.arenaId(), reason)); }
            }
            accepted.sort(comparator(query.order()));
            final int pageCount = Math.max(1, (accepted.size() + query.pageSize() - 1) / query.pageSize());
            final int selectedPage = Math.min(query.page(), pageCount - 1);
            final int first = Math.min(accepted.size(), selectedPage * query.pageSize());
            final int last = Math.min(accepted.size(), first + query.pageSize());
            final List<Candidate> visible = accepted.subList(first, last);
            final Status status = visible.isEmpty() ? Status.EMPTY : Status.READY;
            return new Page(viewRevision, status, selectedPage, pageCount, visible, excluded,
                    MessageKey.of(status == Status.READY ? "selector.ready" : "selector.empty"));
        }

        /** @return first deterministic available candidate for quick join */
        public Optional<Selection> quickJoin(final Collection<Candidate> source, final Query query,
                                             final long viewRevision) {
            final Page page = page(source, query, viewRevision);
            return page.candidates().isEmpty() ? Optional.<Selection>empty()
                    : Optional.of(select(page, page.candidates().get(0).arenaId()));
        }

        /** Validates an arena click against the exact rendered page. */
        public Selection select(final Page page, final ArenaId arenaId) {
            Objects.requireNonNull(page, "page");
            Objects.requireNonNull(arenaId, "arenaId");
            for (Candidate candidate : page.candidates()) {
                if (candidate.arenaId().equals(arenaId)) {
                    return new Selection(arenaId, candidate.definitionRevision(), page.viewRevision());
                }
            }
            throw new IllegalArgumentException("arena was not present in selector view");
        }

        /** @return whether the token still matches current view and arena revision */
        public boolean current(final Selection selection, final long currentViewRevision,
                               final Candidate currentCandidate) {
            return Objects.requireNonNull(selection, "selection").viewRevision() == currentViewRevision
                    && selection.arenaId().equals(Objects.requireNonNull(currentCandidate, "currentCandidate").arenaId())
                    && selection.definitionRevision() == currentCandidate.definitionRevision()
                    && currentCandidate.exclusion(null, null, 1, 0L) == Exclusion.NONE;
        }
    }

    private static Comparator<Candidate> comparator(final Order order) {
        final Comparator<Candidate> identity = Comparator.comparing(candidate -> candidate.arenaId().toString());
        if (order == Order.CAPACITY) {
            return Comparator.comparingInt(Candidate::available).reversed().thenComparing(identity);
        }
        if (order == Order.IDENTITY) { return identity; }
        return Comparator.comparingInt(Candidate::order).thenComparing(identity);
    }
}
