package io.zartra.bedwars.ui.api;

import io.zartra.bedwars.api.identity.GuiPageId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.ui.api.UiModel.Click;
import io.zartra.bedwars.ui.api.UiModel.Component;
import io.zartra.bedwars.ui.api.UiModel.ComponentId;
import io.zartra.bedwars.ui.api.UiModel.PageDefinition;
import io.zartra.bedwars.ui.api.UiModel.PageState;
import io.zartra.bedwars.ui.api.UiModel.Query;
import io.zartra.bedwars.ui.api.UiModel.SessionId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Bounded, thread-safe GUI navigation engine. Loading is asynchronous, stale results and clicks
 * fail closed, and platform adapters remain responsible for owner-thread inventory rendering.
 */
public final class UiFramework {
    private final Registry pages;
    private final TimeSource time;
    private final Duration sessionTtl;
    private final Duration duplicateWindow;
    private final int maximumSessions;
    private final int maximumHistory;
    private final Map<SessionId, MutableSession> sessions = new LinkedHashMap<SessionId, MutableSession>();

    /** Creates a bounded session engine. */
    public UiFramework(final Registry pages, final TimeSource time, final Duration sessionTtl,
                       final Duration duplicateWindow, final int maximumSessions,
                       final int maximumHistory) {
        this.pages = Objects.requireNonNull(pages, "pages");
        this.time = Objects.requireNonNull(time, "time");
        this.sessionTtl = positive(sessionTtl, "sessionTtl");
        this.duplicateWindow = positive(duplicateWindow, "duplicateWindow");
        if (maximumSessions < 1 || maximumSessions > 100000 || maximumHistory < 1 || maximumHistory > 64) {
            throw new IllegalArgumentException("session bounds outside supported range");
        }
        this.maximumSessions = maximumSessions;
        this.maximumHistory = maximumHistory;
    }

    /** Opens a page and returns the asynchronous load request to observe. */
    public synchronized LoadHandle open(final PlayerId viewer, final Query query) {
        cleanupInternal(time.now());
        if (sessions.size() >= maximumSessions) { throw new IllegalStateException("GUI session capacity reached"); }
        final PageDefinition definition = pages.require(query.pageId());
        final SessionId id = SessionId.random();
        final MutableSession session = new MutableSession(id, Objects.requireNonNull(viewer, "viewer"),
                query, time.now().plus(sessionTtl));
        sessions.put(id, session);
        return load(session, definition);
    }

    /** Navigates to a new page while preserving bounded back history. */
    public synchronized LoadHandle navigate(final SessionId id, final Query query) {
        final MutableSession session = requireLive(id);
        final PageDefinition definition = pages.require(query.pageId());
        session.history.addLast(session.query);
        while (session.history.size() > maximumHistory) { session.history.removeFirst(); }
        session.query = query;
        session.state = loading(query.page());
        touch(session);
        return load(session, definition);
    }

    /** Returns to the previous query when present. */
    public synchronized Optional<LoadHandle> back(final SessionId id) {
        final MutableSession session = requireLive(id);
        if (session.history.isEmpty()) { return Optional.empty(); }
        session.query = session.history.removeLast();
        session.state = loading(session.query.page());
        touch(session);
        return Optional.of(load(session, pages.require(session.query.pageId())));
    }

    /** Starts a refresh without changing navigation history. */
    public synchronized LoadHandle refresh(final SessionId id) {
        final MutableSession session = requireLive(id);
        session.state = loading(session.query.page());
        touch(session);
        return load(session, pages.require(session.query.pageId()));
    }

    /** Accepts a completed load only when it is the newest request for this live session. */
    public synchronized LoadVerdict accept(final SessionId id, final long sequence,
                                           final PageState state) {
        final MutableSession session = sessions.get(Objects.requireNonNull(id, "id"));
        if (session == null || session.expiresAt.isBefore(time.now())) { return LoadVerdict.EXPIRED; }
        if (sequence != session.loadSequence) { return LoadVerdict.STALE; }
        final PageState checked = Objects.requireNonNull(state, "state");
        if (checked.page() != session.query.page()) { return LoadVerdict.STALE; }
        session.state = checked;
         touch(session);
        return LoadVerdict.ACCEPTED;
    }

    /** Validates one click against session, revision, component and nonce. */
    public synchronized ClickResult click(final PlayerId viewer, final Click click) {
        final MutableSession session = sessions.get(Objects.requireNonNull(click, "click").sessionId());
        if (session == null || session.expiresAt.isBefore(time.now())) { return ClickResult.rejected(ClickVerdict.EXPIRED); }
        if (!session.viewer.equals(Objects.requireNonNull(viewer, "viewer"))) { return ClickResult.rejected(ClickVerdict.WRONG_VIEWER); }
        if (click.viewRevision() != session.state.revision()) { return ClickResult.rejected(ClickVerdict.STALE); }
        final Instant previousComponentClick = session.componentClicks.get(click.componentId());
        if (previousComponentClick != null
                && previousComponentClick.plus(duplicateWindow).isAfter(click.occurredAt())) {
            return ClickResult.rejected(ClickVerdict.DUPLICATE);
        }
        final Instant previous = session.nonces.get(click.nonce());
        if (previous != null && previous.plus(duplicateWindow).isAfter(click.occurredAt())) {
            return ClickResult.rejected(ClickVerdict.DUPLICATE);
        }
        session.nonces.put(click.nonce(), click.occurredAt());
        session.componentClicks.put(click.componentId(), click.occurredAt());
        session.nonces.entrySet().removeIf(entry -> entry.getValue().plus(duplicateWindow).isBefore(time.now()));
        session.componentClicks.entrySet().removeIf(
                entry -> entry.getValue().plus(duplicateWindow).isBefore(time.now()));
        if (session.nonces.size() > 256) { session.nonces.remove(session.nonces.keySet().iterator().next()); }
        final Optional<Component> component = session.state.component(click.componentId());
        if (!component.isPresent()) { return ClickResult.rejected(ClickVerdict.UNKNOWN_COMPONENT); }
        if (!component.get().enabled()) { return ClickResult.rejected(ClickVerdict.DISABLED); }
        touch(session);
        return ClickResult.accepted(component.get());
    }

    /** Closes one session and discards its navigation and replay state. */
    public synchronized boolean close(final SessionId id) { return sessions.remove(Objects.requireNonNull(id, "id")) != null; }

    /** Expires sessions deterministically. @return number removed */
    public synchronized int cleanup() { return cleanupInternal(time.now()); }

    /** @return immutable diagnostic snapshot */
    public synchronized Optional<SessionSnapshot> snapshot(final SessionId id) {
        final MutableSession value = sessions.get(Objects.requireNonNull(id, "id"));
        return value == null ? Optional.empty() : Optional.of(value.snapshot());
    }

    /** @return immutable page metadata for a renderer or documentation generator */
    public PageDefinition pageDefinition(final GuiPageId id) { return pages.require(id); }

    private LoadHandle load(final MutableSession session, final PageDefinition definition) {
        session.loadSequence++;
        final CompletionStage<PageState> stage = definition.loader().load(session.viewer, session.query);
        if (stage == null) { throw new IllegalStateException("page loader returned null"); }
        return new LoadHandle(session.id, session.loadSequence, stage);
    }

    private MutableSession requireLive(final SessionId id) {
        final MutableSession session = sessions.get(Objects.requireNonNull(id, "id"));
        if (session == null || session.expiresAt.isBefore(time.now())) {
            sessions.remove(id);
            throw new IllegalArgumentException("unknown or expired GUI session");
        }
        return session;
    }

    private void touch(final MutableSession session) { session.expiresAt = time.now().plus(sessionTtl); }

    private int cleanupInternal(final Instant now) {
        final int before = sessions.size();
        sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt.isAfter(now));
        return before - sessions.size();
    }

    private static PageState loading(final int page) {
        return new PageState(0L, PageState.Status.LOADING, page, page + 1,
                Collections.<Component>emptyList(), MessageKey.of("ui.loading"));
    }

    private static Duration positive(final Duration value, final String label) {
        if (value == null || value.isZero() || value.isNegative()) { throw new IllegalArgumentException(label + " must be positive"); }
        return value;
    }

    /** Bounded page-extension registry. Registration is exact and duplicate IDs are rejected. */
    public static final class Registry {
        private final Map<GuiPageId, PageDefinition> definitions = new LinkedHashMap<GuiPageId, PageDefinition>();
        private final int capacity;
        /** Creates a registry populated with validated built-in pages. */
        public Registry(final int capacity, final Collection<PageDefinition> builtIns) {
            if (capacity < 1 || capacity > 10000) { throw new IllegalArgumentException("invalid page capacity"); }
            this.capacity = capacity;
            for (PageDefinition definition : Objects.requireNonNull(builtIns, "builtIns")) { register(definition); }
        }
        /** Registers one extension page. */
        public synchronized void register(final PageDefinition definition) {
            final PageDefinition checked = Objects.requireNonNull(definition, "definition");
            if (definitions.size() >= capacity) { throw new IllegalStateException("page registry capacity reached"); }
            if (definitions.put(checked.id(), checked) != null) { throw new IllegalArgumentException("duplicate GUI page " + checked.id()); }
        }
        /** @return page or throws for an unknown ID */
        public synchronized PageDefinition require(final GuiPageId id) {
            final PageDefinition value = definitions.get(Objects.requireNonNull(id, "id"));
            if (value == null) { throw new IllegalArgumentException("unknown GUI page " + id); }
            return value;
        }
        /** @return immutable deterministic page inventory */
        public synchronized List<PageDefinition> inventory() { return Collections.unmodifiableList(new ArrayList<PageDefinition>(definitions.values())); }
    }

    /** One asynchronous page load linked to its session sequence. */
    public static final class LoadHandle {
        private final SessionId sessionId;
        private final long sequence;
        private final CompletionStage<PageState> stage;
        private LoadHandle(final SessionId sessionId, final long sequence, final CompletionStage<PageState> stage) {
            this.sessionId = sessionId;
             this.sequence = sequence;
            this.stage = stage;
        }
        /** @return session */ public SessionId sessionId() { return sessionId; }
        /** @return monotonic session-local load sequence */ public long sequence() { return sequence; }
        /** @return eventual page state */ public CompletionStage<PageState> stage() { return stage; }
    }

    /** Load acceptance outcomes. */ public enum LoadVerdict { /** Applied. */ ACCEPTED, /** Older than latest request. */ STALE, /** Session absent or expired. */ EXPIRED }
    /** Click validation outcomes. */ public enum ClickVerdict { /** Valid. */ ACCEPTED, /** Session expired. */ EXPIRED, /** Viewer mismatch. */ WRONG_VIEWER, /** Revision mismatch. */ STALE, /** Replayed click. */ DUPLICATE, /** Component absent. */ UNKNOWN_COMPONENT, /** Component disabled. */ DISABLED }

    /** Validated click result containing a component only on acceptance. */
    public static final class ClickResult {
        private final ClickVerdict verdict;
        private final Component component;
        private ClickResult(final ClickVerdict verdict, final Component component) { this.verdict = verdict;
         this.component = component;
        }
        private static ClickResult accepted(final Component component) { return new ClickResult(ClickVerdict.ACCEPTED, component); }
        private static ClickResult rejected(final ClickVerdict verdict) { return new ClickResult(verdict, null); }
        /** @return verdict */ public ClickVerdict verdict() { return verdict; }
        /** @return component only when accepted */ public Optional<Component> component() { return Optional.ofNullable(component); }
    }

    /** Immutable session diagnostic without platform inventory references. */
    public static final class SessionSnapshot {
        private final SessionId id;
         private final PlayerId viewer;
        private final Query query;
        private final PageState state;
         private final List<Query> history;
        private final Instant expiresAt;
        private SessionSnapshot(final SessionId id, final PlayerId viewer, final Query query,
                                final PageState state, final Collection<Query> history, final Instant expiresAt) {
            this.id = id;
             this.viewer = viewer;
             this.query = query;
            this.state = state;
            this.history = Collections.unmodifiableList(new ArrayList<Query>(history));
            this.expiresAt = expiresAt;
        }
        /** @return session ID */ public SessionId id() { return id; }
        /** @return viewer */ public PlayerId viewer() { return viewer; }
        /** @return query */ public Query query() { return query; }
        /** @return state */ public PageState state() { return state; }
        /** @return back history */ public List<Query> history() { return history; }
        /** @return expiry */ public Instant expiresAt() { return expiresAt; }
    }

    private static final class MutableSession {
        private final SessionId id;
         private final PlayerId viewer;
        private final Deque<Query> history = new ArrayDeque<Query>();
        private final Map<UUID, Instant> nonces = new LinkedHashMap<UUID, Instant>();
        private final Map<ComponentId, Instant> componentClicks =
                new LinkedHashMap<ComponentId, Instant>();
        private Query query;
         private PageState state;
         private Instant expiresAt;
        private long loadSequence;
        private MutableSession(final SessionId id, final PlayerId viewer, final Query query, final Instant expiresAt) {
            this.id = id;
             this.viewer = viewer;
             this.query = query;
             this.state = loading(query.page());
            this.expiresAt = expiresAt;
        }
        private SessionSnapshot snapshot() { return new SessionSnapshot(id, viewer, query, state, history, expiresAt); }
    }
}
