package io.zartra.bedwars.config.authorization;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.authorization.AuthorizationService;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Default-deny authorization service using only exact injected grants and canonical aliases. */
public final class DefaultAuthorizationService implements AuthorizationService {
    private static final DefinitionId ALLOWED = DefinitionId.of("zartra", "authorization/explicit_grant");
    private static final DefinitionId DENIED = DefinitionId.of("zartra", "authorization/default_deny");
    private final GrantSource grants;
    private final AliasRegistry aliases;
    private final DecisionSink decisions;

    /** @param grants exact grant source @param aliases validated aliases @param decisions audit sink */
    public DefaultAuthorizationService(final GrantSource grants, final AliasRegistry aliases,
                                       final DecisionSink decisions) {
        this.grants = Objects.requireNonNull(grants, "grants");
        this.aliases = Objects.requireNonNull(aliases, "aliases");
        this.decisions = Objects.requireNonNull(decisions, "decisions");
    }

    @Override
    public AuthorizationDecision authorize(final AuthorizationRequest request) {
        Objects.requireNonNull(request, "request");
        final PermissionNode canonical = aliases.resolve(request.action());
        final AuthorizationDecision decision = grants.hasExactGrant(request.subject(), canonical,
                request.target()) ? AuthorizationDecision.allow(ALLOWED) : AuthorizationDecision.deny(DENIED);
        decisions.record(request.subject(), canonical, request.target(), decision);
        return decision;
    }

    /** Exact grant lookup port implemented by a platform permission adapter or immutable policy. */
    public interface GrantSource {
        /** @return whether this exact subject/action/target tuple is granted */
        boolean hasExactGrant(AuthorizationSubject subject, PermissionNode action, DefinitionId target);
    }

    /** Secret-free authorization audit sink. */
    public interface DecisionSink {
        /** Records stable identities and the decision; implementations must not throw. */
        void record(AuthorizationSubject subject, PermissionNode action, DefinitionId target,
                    AuthorizationDecision decision);
    }

    /** Immutable one-hop migration-alias registry. */
    public static final class AliasRegistry {
        private final Map<PermissionNode, PermissionNode> aliases;
        private AliasRegistry(final Map<PermissionNode, PermissionNode> aliases) {
            final Map<PermissionNode, PermissionNode> copy =
                    new LinkedHashMap<PermissionNode, PermissionNode>();
            for (Map.Entry<PermissionNode, PermissionNode> entry
                    : Objects.requireNonNull(aliases, "aliases").entrySet()) {
                final PermissionNode source = Objects.requireNonNull(entry.getKey(), "alias");
                final PermissionNode target = Objects.requireNonNull(entry.getValue(), "canonical");
                if (source.equals(target) || source.value().contains("*") || target.value().contains("*")
                        || !target.value().startsWith("zartrabedwars.")) {
                    throw new IllegalArgumentException("Alias must map to one explicit canonical node");
                }
                copy.put(source, target);
            }
            for (PermissionNode target : copy.values()) {
                if (copy.containsKey(target)) {
                    throw new IllegalArgumentException("Alias chains and cycles are forbidden");
                }
            }
            this.aliases = Collections.unmodifiableMap(copy);
        }
        /** @return empty registry */ public static AliasRegistry empty() {
            return new AliasRegistry(Collections.<PermissionNode, PermissionNode>emptyMap());
        }
        /** @return validated one-hop registry */
        public static AliasRegistry of(final Map<PermissionNode, PermissionNode> aliases) {
            return new AliasRegistry(aliases);
        }
        /** @return canonical node or the unchanged already-canonical node */
        public PermissionNode resolve(final PermissionNode node) {
            final PermissionNode resolved = aliases.get(Objects.requireNonNull(node, "node"));
            return resolved == null ? node : resolved;
        }
        /** @return deterministic aliases */ public Map<PermissionNode, PermissionNode> aliases() { return aliases; }
    }

    /** Canonical granular action families required by the PRD. */
    public enum CanonicalAction {
        /** View. */ VIEW("view"),
        /** Use. */ USE("use"),
        /** Create. */ CREATE("create"),
        /** Edit. */ EDIT("edit"),
        /** Delete. */ DELETE("delete"),
        /** Duplicate. */ DUPLICATE("duplicate"),
        /** Import. */ IMPORT("import"),
        /** Export. */ EXPORT("export"),
        /** Enable. */ ENABLE("enable"),
        /** Disable. */ DISABLE("disable"),
        /** Start. */ START("start"),
        /** Stop. */ STOP("stop"),
        /** Force. */ FORCE("force"),
        /** Reload. */ RELOAD("reload"),
        /** Reset. */ RESET("reset"),
        /** Backup. */ BACKUP("backup"),
        /** Restore. */ RESTORE("restore"),
        /** Migrate. */ MIGRATE("migrate"),
        /** Inspect. */ INSPECT("inspect"),
        /** Debug. */ DEBUG("debug"),
        /** Bypass. */ BYPASS("bypass"),
        /** Manage. */ MANAGE("manage"),
        /** Grant. */ GRANT("grant"),
        /** Revoke. */ REVOKE("revoke"),
        /** Set. */ SET("set"),
        /** Add. */ ADD("add"),
        /** Remove. */ REMOVE("remove"),
        /** Approve. */ APPROVE("approve"),
        /** Reject. */ REJECT("reject"),
        /** Override. */ OVERRIDE("override"),
        /** View real identities. */ VIEW_IDENTITY("view.identity"),
        /** View hidden data. */ VIEW_HIDDEN("view.hidden"),
        /** View private data. */ VIEW_PRIVATE("view.private");
        private final String token;
        CanonicalAction(final String token) { this.token = token; }
        /** @return canonical suffix */ public String token() { return token; }
    }

    /** Canonical permission-node factory; recommended roles never enter this model. */
    public static final class PermissionCatalog {
        private PermissionCatalog() { throw new AssertionError("No instances"); }
        /** @return canonical {@code zartrabedwars.surface.action} node */
        public static PermissionNode node(final String surface, final CanonicalAction action) {
            if (surface == null || !surface.matches("[a-z0-9][a-z0-9.-]{1,63}")) {
                throw new IllegalArgumentException("Invalid permission surface");
            }
            return PermissionNode.of("zartrabedwars." + surface + '.'
                    + Objects.requireNonNull(action, "action").token());
        }
        /** @return complete deterministic canonical action set for one surface */
        public static List<PermissionNode> nodes(final String surface) {
            final List<PermissionNode> nodes = new ArrayList<PermissionNode>();
            for (CanonicalAction action : CanonicalAction.values()) { nodes.add(node(surface, action)); }
            Collections.sort(nodes);
            return Collections.unmodifiableList(nodes);
        }
    }

    /** Immutable exact grant source suitable for deterministic policy composition and tests. */
    public static final class ImmutableGrantSource implements GrantSource {
        private final List<Grant> grants;
        private ImmutableGrantSource(final Collection<Grant> grants) {
            final List<Grant> copy = new ArrayList<Grant>();
            for (Grant grant : Objects.requireNonNull(grants, "grants")) {
                final Grant checked = Objects.requireNonNull(grant, "grant");
                if (copy.contains(checked)) { throw new IllegalArgumentException("Duplicate exact grant"); }
                copy.add(checked);
            }
            this.grants = Collections.unmodifiableList(copy);
        }
        /** @return exact immutable policy */ public static ImmutableGrantSource of(final Collection<Grant> grants) {
            return new ImmutableGrantSource(grants);
        }
        @Override public boolean hasExactGrant(final AuthorizationSubject subject,
                                               final PermissionNode action,
                                               final DefinitionId target) {
            return grants.contains(Grant.of(subject, action, target));
        }
    }

    /** Immutable exact subject/action/target grant. */
    public static final class Grant {
        private final AuthorizationSubject subject;
        private final PermissionNode action;
        private final DefinitionId target;
        private Grant(final AuthorizationSubject subject, final PermissionNode action,
                      final DefinitionId target) {
            this.subject = Objects.requireNonNull(subject, "subject");
            this.action = Objects.requireNonNull(action, "action");
            this.target = Objects.requireNonNull(target, "target");
        }
        /** @return exact grant */ public static Grant of(final AuthorizationSubject subject,
                                                          final PermissionNode action,
                                                          final DefinitionId target) {
            return new Grant(subject, action, target);
        }
        @Override public int hashCode() { return Objects.hash(subject, action, target); }
        @Override public boolean equals(final Object other) {
            if (this == other) { return true; }
            if (!(other instanceof Grant)) { return false; }
            final Grant that = (Grant) other;
            return subject.equals(that.subject) && action.equals(that.action) && target.equals(that.target);
        }
    }
}
