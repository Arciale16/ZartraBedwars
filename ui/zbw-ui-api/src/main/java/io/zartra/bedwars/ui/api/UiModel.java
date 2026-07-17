package io.zartra.bedwars.ui.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GuiPageId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.command.api.PresentationActions.ActionId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Immutable values and extension contracts used by the neutral GUI framework. */
public final class UiModel {
    private UiModel() { throw new AssertionError("No instances"); }

    /** Stable component identity used for click de-duplication and stale-view rejection. */
    public static final class ComponentId implements Comparable<ComponentId> {
        private final DefinitionId value;
        private ComponentId(final DefinitionId value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return validated component ID */ public static ComponentId of(final String path) { return new ComponentId(DefinitionId.of("zartra", "component/" + path)); }
        /** @return parsed ID */ public static ComponentId parse(final String value) { return new ComponentId(DefinitionId.parse(value)); }
        /** @return underlying ID */ public DefinitionId value() { return value; }
        @Override public int compareTo(final ComponentId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public String toString() { return value.toString(); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof ComponentId && value.equals(((ComponentId) other).value); }
    }

    /** Opaque, unguessable identity of one viewer session. */
    public static final class SessionId implements Comparable<SessionId> {
        private final UUID value;
        private SessionId(final UUID value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return supplied UUID as a typed ID */ public static SessionId of(final UUID value) { return new SessionId(value); }
        /** @return random session ID */ public static SessionId random() { return of(UUID.randomUUID()); }
        /** @return parsed session ID */ public static SessionId parse(final String value) { return of(UUID.fromString(value)); }
        /** @return UUID */ public UUID value() { return value; }
        @Override public int compareTo(final SessionId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public String toString() { return value.toString(); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof SessionId && value.equals(((SessionId) other).value); }
    }

    /** Accessibility metadata that never relies on colour alone. */
    public static final class Accessibility {
        private final MessageKey semanticLabel;
        private final MessageKey nonColorCue;
        private final Set<InputAlternative> alternatives;
        /** Creates mandatory semantic and non-colour cues. */
        public Accessibility(final MessageKey semanticLabel, final MessageKey nonColorCue,
                             final Collection<InputAlternative> alternatives) {
            this.semanticLabel = Objects.requireNonNull(semanticLabel, "semanticLabel");
            this.nonColorCue = Objects.requireNonNull(nonColorCue, "nonColorCue");
            final Set<InputAlternative> copy = new LinkedHashSet<InputAlternative>();
            for (InputAlternative value : Objects.requireNonNull(alternatives, "alternatives")) {
                copy.add(Objects.requireNonNull(value, "alternative"));
            }
            if (copy.isEmpty()) { throw new IllegalArgumentException("input alternative required"); }
            this.alternatives = Collections.unmodifiableSet(copy);
        }
        /** @return semantic label */ public MessageKey semanticLabel() { return semanticLabel; }
        /** @return cue independent of colour */ public MessageKey nonColorCue() { return nonColorCue; }
        /** @return supported equivalent inputs */ public Set<InputAlternative> alternatives() { return alternatives; }
        /** Supported accessible alternatives. */ public enum InputAlternative { /** Command equivalent. */ COMMAND, /** Chat entry. */ CHAT, /** Keyboard navigation. */ KEYBOARD, /** Bedrock/form projection. */ BEDROCK }
    }

    /** Immutable inventory component. Slots are logical and are translated by platform adapters. */
    public static final class Component {
        private final ComponentId id;
        private final int slot;
        private final ActionId action;
        private final MessageKey label;
        private final List<MessageKey> lore;
        private final boolean enabled;
        private final Accessibility accessibility;
        /** Creates one component in the bounded 6-row inventory grid. */
        public Component(final ComponentId id, final int slot, final ActionId action,
                         final MessageKey label, final Collection<MessageKey> lore,
                         final boolean enabled, final Accessibility accessibility) {
            this.id = Objects.requireNonNull(id, "id");
            if (slot < 0 || slot >= 54) { throw new IllegalArgumentException("slot outside inventory"); }
            this.slot = slot;
            this.action = Objects.requireNonNull(action, "action");
            this.label = Objects.requireNonNull(label, "label");
            final List<MessageKey> copy = new ArrayList<MessageKey>();
            for (MessageKey value : Objects.requireNonNull(lore, "lore")) { copy.add(Objects.requireNonNull(value, "lore line")); }
            if (copy.size() > 16) { throw new IllegalArgumentException("too many lore lines"); }
            this.lore = Collections.unmodifiableList(copy);
            this.enabled = enabled;
            this.accessibility = Objects.requireNonNull(accessibility, "accessibility");
        }
        /** @return component ID */ public ComponentId id() { return id; }
        /** @return logical slot */ public int slot() { return slot; }
        /** @return shared action */ public ActionId action() { return action; }
        /** @return label key */ public MessageKey label() { return label; }
        /** @return lore keys */ public List<MessageKey> lore() { return lore; }
        /** @return whether actionable */ public boolean enabled() { return enabled; }
        /** @return accessibility metadata */ public Accessibility accessibility() { return accessibility; }
    }

    /** Immutable paginated query with bounded search and filtering. */
    public static final class Query {
        private final GuiPageId pageId;
        private final int page;
        private final String search;
        private final DefinitionId filter;
        private final Sort sort;
        /** Creates a bounded page query. */
        public Query(final GuiPageId pageId, final int page, final String search,
                     final DefinitionId filter, final Sort sort) {
            this.pageId = Objects.requireNonNull(pageId, "pageId");
            if (page < 0 || page > 10000) { throw new IllegalArgumentException("invalid page index"); }
            this.page = page;
            if (search == null || search.length() > 128) { throw new IllegalArgumentException("invalid search"); }
            this.search = search;
             this.filter = filter;
            this.sort = Objects.requireNonNull(sort, "sort");
        }
        /** @return unfiltered first page */ public static Query first(final GuiPageId pageId) { return new Query(pageId, 0, "", null, Sort.NATURAL); }
        /** @return page ID */ public GuiPageId pageId() { return pageId; }
        /** @return zero-based page */ public int page() { return page; }
        /** @return normalized search input */ public String search() { return search; }
        /** @return optional stable filter */ public Optional<DefinitionId> filter() { return Optional.ofNullable(filter); }
        /** @return sort */ public Sort sort() { return sort; }
        /** Sort choices supported uniformly by commands and GUIs. */ public enum Sort { /** Provider order. */ NATURAL, /** Ascending label. */ LABEL_ASCENDING, /** Descending label. */ LABEL_DESCENDING }
    }

    /** Immutable asynchronously loaded page state. */
    public static final class PageState {
        private final long revision;
        private final Status status;
        private final int page;
        private final int pageCount;
        private final List<Component> components;
        private final MessageKey message;
        /** Creates a validated page state. */
        public PageState(final long revision, final Status status, final int page,
                         final int pageCount, final Collection<Component> components,
                         final MessageKey message) {
            if (revision < 0L || page < 0 || pageCount < 1 || page >= pageCount) {
                throw new IllegalArgumentException("invalid page state bounds");
            }
            this.revision = revision;
            this.status = Objects.requireNonNull(status, "status");
            this.page = page;
            this.pageCount = pageCount;
            final List<Component> copy = new ArrayList<Component>();
            final Set<Integer> slots = new LinkedHashSet<Integer>();
            final Set<ComponentId> ids = new LinkedHashSet<ComponentId>();
            for (Component value : Objects.requireNonNull(components, "components")) {
                final Component component = Objects.requireNonNull(value, "component");
                if (!slots.add(component.slot()) || !ids.add(component.id())) { throw new IllegalArgumentException("duplicate component slot or ID"); }
                copy.add(component);
            }
            if (copy.size() > 54) { throw new IllegalArgumentException("too many components"); }
            this.components = Collections.unmodifiableList(copy);
            this.message = Objects.requireNonNull(message, "message");
        }
        /** @return revision */ public long revision() { return revision; }
        /** @return status */ public Status status() { return status; }
        /** @return zero-based page */ public int page() { return page; }
        /** @return page count */ public int pageCount() { return pageCount; }
        /** @return components */ public List<Component> components() { return components; }
        /** @return status message */ public MessageKey message() { return message; }
        /** @return component with ID */ public Optional<Component> component(final ComponentId id) {
            for (Component component : components) { if (component.id().equals(id)) { return Optional.of(component); } }
            return Optional.empty();
        }
        /** Async view states. */ public enum Status { /** Pending. */ LOADING, /** Data present. */ READY, /** No rows. */ EMPTY, /** Recoverable error. */ ERROR }
    }

    /** Immutable page extension contract. Loaders run off the owner thread. */
    public static final class PageDefinition {
        private final GuiPageId id;
        private final MessageKey title;
        private final PageLoader loader;
        private final Set<Interaction> interactions;
        /** Creates a page definition. */
        public PageDefinition(final GuiPageId id, final MessageKey title, final PageLoader loader,
                              final Collection<Interaction> interactions) {
            this.id = Objects.requireNonNull(id, "id");
            this.title = Objects.requireNonNull(title, "title");
            this.loader = Objects.requireNonNull(loader, "loader");
            final Set<Interaction> copy = new LinkedHashSet<Interaction>();
            for (Interaction value : Objects.requireNonNull(interactions, "interactions")) { copy.add(Objects.requireNonNull(value, "interaction")); }
            if (copy.isEmpty()) { throw new IllegalArgumentException("interaction policy required"); }
            this.interactions = Collections.unmodifiableSet(copy);
        }
        /** @return page ID */ public GuiPageId id() { return id; }
        /** @return title key */ public MessageKey title() { return title; }
        /** @return async loader */ public PageLoader loader() { return loader; }
        /** @return allowed inventory interactions */ public Set<Interaction> interactions() { return interactions; }
    }

    /** Async page-loading port. It must be bounded and must not block a Minecraft owner thread. */
    public interface PageLoader { /** @return eventual immutable page state */ CompletionStage<PageState> load(PlayerId viewer, Query query); }

    /** Explicit interaction allowlist; unlisted interactions are denied. */
    public enum Interaction { /** Primary click. */ PRIMARY, /** Secondary click. */ SECONDARY, /** Keyboard-equivalent action. */ KEYBOARD }

    /** Immutable click request including view revision and nonce for replay rejection. */
    public static final class Click {
        private final SessionId sessionId;
        private final ComponentId componentId;
        private final long viewRevision;
        private final UUID nonce;
        private final Instant occurredAt;
        /** Creates one click request. */
        public Click(final SessionId sessionId, final ComponentId componentId,
                     final long viewRevision, final UUID nonce, final Instant occurredAt) {
            this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
            this.componentId = Objects.requireNonNull(componentId, "componentId");
            if (viewRevision < 0L) { throw new IllegalArgumentException("revision must not be negative"); }
            this.viewRevision = viewRevision;
            this.nonce = Objects.requireNonNull(nonce, "nonce");
            this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        }
        /** @return session */ public SessionId sessionId() { return sessionId; }
        /** @return component */ public ComponentId componentId() { return componentId; }
        /** @return view revision */ public long viewRevision() { return viewRevision; }
        /** @return single-click nonce */ public UUID nonce() { return nonce; }
        /** @return occurrence time */ public Instant occurredAt() { return occurredAt; }
    }
}
