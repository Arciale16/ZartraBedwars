package io.zartra.bedwars.command.api;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.GuiPageId;
import io.zartra.bedwars.api.localization.LocalizationService;
import io.zartra.bedwars.api.localization.MessageKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Shared action vocabulary used by command and GUI adapters. It preserves presentation parity
 * while application use cases remain the sole owners of arena and game rules.
 */
public final class PresentationActions {
    private PresentationActions() { throw new AssertionError("No instances"); }

    /** Stable identity for one user-visible query or mutation. */
    public static final class ActionId implements Comparable<ActionId> {
        private final DefinitionId value;
        private ActionId(final DefinitionId value) { this.value = Objects.requireNonNull(value, "value"); }
        /** @return a validated M09 action ID */ public static ActionId of(final String path) { return new ActionId(DefinitionId.of("zartra", "action/" + path)); }
        /** @return parsed canonical ID */ public static ActionId parse(final String value) { return new ActionId(DefinitionId.parse(value)); }
        /** @return underlying definition ID */ public DefinitionId value() { return value; }
        @Override public int compareTo(final ActionId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
        @Override public String toString() { return value.toString(); }
        @Override public int hashCode() { return value.hashCode(); }
        @Override public boolean equals(final Object other) { return this == other || other instanceof ActionId && value.equals(((ActionId) other).value); }
    }

    /** Presentation surface that initiated an action. */
    public enum Surface { /** Unified command tree. */ COMMAND, /** Paginated GUI. */ GUI }

    /** Immutable action request free of Bukkit and raw sender objects. */
    public static final class Request {
        private final AuthorizationSubject actor;
        private final ActionId action;
        private final DefinitionId target;
        private final long revision;
        private final CorrelationId correlationId;
        private final CommandModel.Arguments arguments;
        private final Surface surface;
        /** Creates one validated request. */
        public Request(final AuthorizationSubject actor, final ActionId action,
                       final DefinitionId target, final long revision,
                       final CorrelationId correlationId, final CommandModel.Arguments arguments,
                       final Surface surface) {
            this.actor = Objects.requireNonNull(actor, "actor");
            this.action = Objects.requireNonNull(action, "action");
            this.target = Objects.requireNonNull(target, "target");
            if (revision < 0L) { throw new IllegalArgumentException("revision must not be negative"); }
            this.revision = revision;
            this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
            this.arguments = Objects.requireNonNull(arguments, "arguments");
            this.surface = Objects.requireNonNull(surface, "surface");
        }
        /** @return authenticated actor */ public AuthorizationSubject actor() { return actor; }
        /** @return action */ public ActionId action() { return action; }
        /** @return protected target */ public DefinitionId target() { return target; }
        /** @return optimistic revision */ public long revision() { return revision; }
        /** @return audit correlation */ public CorrelationId correlationId() { return correlationId; }
        /** @return validated arguments */ public CommandModel.Arguments arguments() { return arguments; }
        /** @return originating surface */ public Surface surface() { return surface; }
    }

    /** Immutable localized action response shared by commands and GUIs. */
    public static final class Response {
        private final Status status;
        private final MessageKey message;
        private final LocalizationService.Parameters parameters;
        private final long revision;
        private Response(final Status status, final MessageKey message,
                         final LocalizationService.Parameters parameters, final long revision) {
            this.status = Objects.requireNonNull(status, "status");
            this.message = Objects.requireNonNull(message, "message");
            this.parameters = Objects.requireNonNull(parameters, "parameters");
            if (revision < 0L) { throw new IllegalArgumentException("revision must not be negative"); }
            this.revision = revision;
        }
        /** @return a response with typed localization parameters */
        public static Response of(final Status status, final MessageKey message,
                                  final LocalizationService.Parameters parameters,
                                  final long revision) {
            return new Response(status, message, parameters, revision);
        }
        /** @return a parameter-free response */
        public static Response simple(final Status status, final String message, final long revision) {
            return of(status, MessageKey.of(message), LocalizationService.Parameters.empty(), revision);
        }
        /** @return status */ public Status status() { return status; }
        /** @return message key */ public MessageKey message() { return message; }
        /** @return safe parameters */ public LocalizationService.Parameters parameters() { return parameters; }
        /** @return resulting revision */ public long revision() { return revision; }
        /** Result categories with identical command and GUI semantics. */
        public enum Status { /** Success. */ SUCCESS, /** Invalid input. */ INVALID, /** Denied. */ FORBIDDEN, /** Stale state. */ CONFLICT, /** Absent target. */ NOT_FOUND, /** Cancelled. */ CANCELLED, /** Timed out. */ TIMEOUT, /** Operational error. */ ERROR }
    }

    /** Application use-case adapter. Implementations contain no presentation code. */
    public interface UseCase { /** @return eventual structured response */ CompletionStage<Response> execute(Request request); }

    /** Immutable metadata joining one command path and one GUI page to the same action. */
    public static final class Definition {
        private final ActionId id;
        private final String commandPath;
        private final GuiPageId pageId;
        private final PermissionNode permission;
        private final boolean destructive;
        private final Set<String> requirementIds;
        private Definition(final ActionId id, final String commandPath, final GuiPageId pageId,
                           final PermissionNode permission, final boolean destructive,
                           final Collection<String> requirementIds) {
            this.id = Objects.requireNonNull(id, "id");
            if (commandPath == null || !commandPath.matches("/[a-z0-9][a-z0-9 _-]{1,190}")) {
                throw new IllegalArgumentException("invalid command path");
            }
            this.commandPath = commandPath;
            this.pageId = Objects.requireNonNull(pageId, "pageId");
            this.permission = Objects.requireNonNull(permission, "permission");
            this.destructive = destructive;
            final Set<String> ids = new LinkedHashSet<String>();
            for (String value : Objects.requireNonNull(requirementIds, "requirementIds")) {
                if (value == null || !value.matches("ZBW-[A-Z]+-[0-9]{3}")) {
                    throw new IllegalArgumentException("invalid requirement ID");
                }
                ids.add(value);
            }
            if (ids.isEmpty()) { throw new IllegalArgumentException("requirement IDs required"); }
            this.requirementIds = Collections.unmodifiableSet(ids);
        }
        /** @return action ID */ public ActionId id() { return id; }
        /** @return canonical command path */ public String commandPath() { return commandPath; }
        /** @return corresponding GUI page */ public GuiPageId pageId() { return pageId; }
        /** @return exact permission */ public PermissionNode permission() { return permission; }
        /** @return whether confirmation is mandatory */ public boolean destructive() { return destructive; }
        /** @return directly covered requirements */ public Set<String> requirementIds() { return requirementIds; }
    }

    /** Immutable exact registry; missing and duplicate action bindings fail closed. */
    public static final class Registry {
        private final Map<ActionId, UseCase> handlers;
        /** Creates a complete registry for the supplied catalogue. */
        public Registry(final Collection<Definition> definitions, final Map<ActionId, UseCase> bindings) {
            final Map<ActionId, UseCase> copy = new LinkedHashMap<ActionId, UseCase>();
            for (Definition definition : Objects.requireNonNull(definitions, "definitions")) {
                final UseCase useCase = Objects.requireNonNull(bindings, "bindings").get(definition.id());
                if (useCase == null) { throw new IllegalArgumentException("missing action binding " + definition.id()); }
                if (copy.put(definition.id(), useCase) != null) { throw new IllegalArgumentException("duplicate action " + definition.id()); }
            }
            if (copy.size() != bindings.size()) { throw new IllegalArgumentException("unknown action binding"); }
            handlers = Collections.unmodifiableMap(copy);
        }
        /** Executes the exact bound use case, or fails closed for an unknown action. */
        public CompletionStage<Response> execute(final Request request) {
            final UseCase useCase = handlers.get(Objects.requireNonNull(request, "request").action());
            if (useCase == null) {
                return CompletableFuture.completedFuture(Response.simple(Response.Status.NOT_FOUND,
                        "presentation.action.unknown", request.revision()));
            }
            try {
                final CompletionStage<Response> response = useCase.execute(request);
                if (response == null) { throw new IllegalStateException("use case returned null"); }
                return response;
            } catch (RuntimeException failure) {
                return CompletableFuture.completedFuture(Response.simple(Response.Status.ERROR,
                        "presentation.action.failed", request.revision()));
            }
        }
        /** @return bound handler for parity and integration diagnostics */
        public Optional<UseCase> handler(final ActionId action) { return Optional.ofNullable(handlers.get(action)); }
        /** @return number of exact bindings */ public int size() { return handlers.size(); }
    }

    /** Deterministic M07/M08 presentation inventory assigned to Milestone 9. */
    public static final class Catalog {
        private Catalog() { throw new AssertionError("No instances"); }

        /** @return complete immutable action definitions used by generators and parity tests */
        public static List<Definition> standard() {
            final List<Definition> definitions = new ArrayList<Definition>();
            add(definitions, "arena", new String[] {"list", "status", "health", "create", "rename",
                    "enable", "disable", "delete", "duplicate", "import", "export", "backup",
                    "restore", "validate", "diagnostics"}, "arena", "ZBW-ARENA-001",
                    "ZBW-ARENA-003", "ZBW-ARENA-004", "ZBW-ARENA-006", "ZBW-ARENA-007",
                    "ZBW-ARENA-009", "ZBW-UX-001", "ZBW-UX-006");
            add(definitions, "setup", new String[] {"start", "progress", "edit", "team", "spawn",
                    "bed", "generator", "shop-location", "upgrade-location", "region", "property",
                    "markers", "preview", "apply", "undo", "redo", "save", "rollback", "status",
                    "exit"}, "setup", "ZBW-ARENA-002", "ZBW-ARENA-005", "ZBW-ARENA-008",
                    "ZBW-ARENA-009", "ZBW-ADDON-408", "ZBW-ADDON-409", "ZBW-ADDON-410",
                    "ZBW-ADDON-411", "ZBW-ADDON-412", "ZBW-ADDON-413", "ZBW-ADDON-414",
                    "ZBW-ADDON-415", "ZBW-ADDON-416", "ZBW-ADDON-417", "ZBW-ADDON-418",
                    "ZBW-ADDON-419", "ZBW-ADDON-420", "ZBW-ADDON-421", "ZBW-ADDON-422",
                    "ZBW-ADDON-423", "ZBW-UX-001", "ZBW-UX-002", "ZBW-UX-003", "ZBW-UX-006");
            add(definitions, "game", new String[] {"join", "leave", "lobby-status", "match-status",
                    "start", "force-start", "stop", "recover", "reconnect-diagnostics",
                    "restoration-diagnostics", "health", "diagnostics"}, "game", "ZBW-GAME-001",
                    "ZBW-GAME-002", "ZBW-GAME-003", "ZBW-GAME-006", "ZBW-GAME-008",
                    "ZBW-GAME-010", "ZBW-UX-001", "ZBW-UX-006");
            add(definitions, "hotbar-manager", new String[] {"edit", "preview", "reload", "validate",
                    "inspect"}, "hotbar.manager", range("ZBW-ADDON-", 1, 9));
            add(definitions, "deposit", new String[] {"hand", "resources", "all", "reload", "inspect"},
                    "deposit", range("ZBW-ADDON-", 108, 114));
            add(definitions, "arena-start-message", new String[] {"edit", "preview", "reload",
                    "validate", "inspect"}, "arena.start.message", range("ZBW-ADDON-", 124, 130));
            add(definitions, "anti-drop", new String[] {"edit", "preview", "reload", "validate",
                    "inspect"}, "anti.drop", range("ZBW-ADDON-", 148, 154));
            add(definitions, "leave-delay", new String[] {"edit", "preview", "reload", "validate",
                    "inspect"}, "leave.delay", range("ZBW-ADDON-", 334, 340));
            add(definitions, "tab-sorter", new String[] {"edit", "preview", "reload", "validate",
                    "inspect"}, "tab.sorter", range("ZBW-ADDON-", 398, 407));
            add(definitions, "bossbar", new String[] {"edit", "preview", "reload", "validate",
                    "inspect"}, "bossbar", range("ZBW-ADDON-", 424, 431));
            add(definitions, "adventure-mode", new String[] {"edit", "preview", "reload", "validate",
                    "inspect"}, "adventure.mode", range("ZBW-ADDON-", 432, 437));
            final Map<ActionId, Definition> unique = new LinkedHashMap<ActionId, Definition>();
            for (Definition definition : definitions) {
                if (unique.put(definition.id(), definition) != null) {
                    throw new IllegalStateException("duplicate standard action " + definition.id());
                }
            }
            return Collections.unmodifiableList(new ArrayList<Definition>(unique.values()));
        }

        /** @return additive immutable M10 selector, queue, mode and spectator actions */
        public static List<Definition> m10() {
            final List<Definition> definitions = new ArrayList<Definition>();
            addM10(definitions, "selector", new String[] {"quick-join", "arena", "mode", "map",
                    "layout", "team-size"}, "selector", false,
                    "ZBW-GAME-007", "ZBW-CONTENT-003");
            addM10(definitions, "queue", new String[] {"join", "leave", "status"},
                    "matchmaking.queue", false, "ZBW-GAME-007");
            addM10(definitions, "spectator", new String[] {"enter", "leave", "target", "next",
                    "previous", "status", "options"}, "spectator", false,
                    merge(new String[] {"ZBW-GAME-009"}, range("ZBW-ADDON-", 92, 101),
                            range("ZBW-ADDON-", 115, 123)));
            addM10(definitions, "compass", new String[] {"track", "communications"},
                    "compass", false, range("ZBW-ADDON-", 131, 140));
            addM10(definitions, "team-selector", new String[] {"open", "select", "clear", "status"},
                    "team.selector", false, range("ZBW-ADDON-", 155, 163));
            addM10(definitions, "admin-matchmaking", new String[] {"diagnostics", "reservations"},
                    "admin.matchmaking", false, "ZBW-GAME-007");
            addM10(definitions, "admin-mode", new String[] {"list", "validate", "enable", "disable"},
                    "admin.mode", true, merge(new String[] {"ZBW-GAME-004", "ZBW-GAME-005",
                            "ZBW-CONTENT-003"}, range("ZBW-ADDON-", 236, 244)));
            final Map<ActionId, Definition> unique = new LinkedHashMap<ActionId, Definition>();
            for (Definition definition : definitions) {
                if (unique.put(definition.id(), definition) != null) {
                    throw new IllegalStateException("duplicate M10 action " + definition.id());
                }
            }
            return Collections.unmodifiableList(new ArrayList<Definition>(unique.values()));
        }

        /** @return immutable M09 baseline followed by additive M10 actions */
        public static List<Definition> throughM10() {
            final List<Definition> result = new ArrayList<Definition>(standard());
            result.addAll(m10());
            return Collections.unmodifiableList(result);
        }

        /** @return additive immutable M11 shop, generator, upgrade and utility actions */
        public static List<Definition> m11() {
            final List<Definition> definitions = new ArrayList<Definition>();
            addM11(definitions, "shop", new String[] {"open", "quick-buy", "favourites", "history",
                    "rotation", "inspect"}, "shop", false,
                    "ZBW-SHOP-001", "ZBW-SHOP-002", "ZBW-SHOP-003", "ZBW-SHOP-004");
            addM11(definitions, "upgrade", new String[] {"open", "purchase", "status", "inspect"},
                    "upgrade", false, "ZBW-SHOP-005");
            addM11(definitions, "generator", new String[] {"status", "validate", "inspect"},
                    "generator", false, "ZBW-SHOP-006");
            addM11(definitions, "utility", new String[] {"use", "status", "inspect"},
                    "item", false, "ZBW-SHOP-007");
            addM11(definitions, "popup-towers", new String[] {"preview", "validate", "inspect"},
                    "popup.towers", false, range("ZBW-ADDON-", 184, 193));
            addM11(definitions, "sponge-effects", new String[] {"preview", "validate", "inspect"},
                    "sponge.effects", false, range("ZBW-ADDON-", 141, 147));
            addM11(definitions, "item-rotation", new String[] {"status", "validate", "inspect"},
                    "item.rotation", false, range("ZBW-ADDON-", 379, 388));
            return Collections.unmodifiableList(definitions);
        }

        /** @return immutable presentation catalogue through M11 */
        public static List<Definition> throughM11() {
            final List<Definition> result = new ArrayList<Definition>(throughM10());
            result.addAll(m11());
            return Collections.unmodifiableList(result);
        }

        /** @return additive immutable M12 progression, economy and reward actions */
        public static List<Definition> m12() {
            final List<Definition> definitions = new ArrayList<Definition>();
            addM12(definitions, "progression", new String[] {"view", "level", "xp", "prestige"},
                    "progression", false, "ZBW-PROG-001", "ZBW-PROG-002",
                    "ZBW-PROG-003", "ZBW-PROG-004");
            addM12(definitions, "currency", new String[] {"balances"}, "currency", false,
                    "ZBW-PROG-005");
            addM12(definitions, "reward", new String[] {"list", "claim", "unlocks"},
                    "reward", false, "ZBW-PROG-005", "ZBW-PROG-011");
            addM12(definitions, "admin-progression", new String[] {"inspect", "grant-xp",
                    "adjust", "recalculate"}, "progression", true,
                    "ZBW-PROG-001", "ZBW-PROG-002", "ZBW-PROG-003", "ZBW-PROG-004");
            addM12(definitions, "admin-reward", new String[] {"inspect", "grant",
                    "failures"}, "reward", true, "ZBW-PROG-005", "ZBW-PROG-011");
            addM12(definitions, "admin-currency", new String[] {"inspect", "ledger"},
                    "currency", true, "ZBW-PROG-005");
            return Collections.unmodifiableList(definitions);
        }

        /** @return immutable presentation catalogue through M12 */
        public static List<Definition> throughM12() {
            final List<Definition> result = new ArrayList<Definition>(throughM11());
            result.addAll(m12());
            return Collections.unmodifiableList(result);
        }

        /** @return additive immutable M13 objective and seasonal-progression actions */
        public static List<Definition> m13() {
            final List<Definition> definitions = new ArrayList<Definition>();
            addM13(definitions, "objectives", new String[] {"view"}, "objectives.view", false,
                    "ZBW-PROG-010");
            addM13(definitions, "quests", new String[] {"active", "completed"}, "quests.view", false,
                    "ZBW-PROG-009");
            addM13(definitions, "achievements", new String[] {"view"}, "achievements.view", false,
                    "ZBW-PROG-012");
            addM13(definitions, "challenges", new String[] {"view"}, "challenges.view", false,
                    "ZBW-PROG-013");
            addM13(definitions, "battlepass", new String[] {"view", "tiers"}, "battlepass.view", false,
                    "ZBW-PROG-013");
            addM13(definitions, "rewards", new String[] {"claim"}, "rewards.claim", true,
                    "ZBW-PROG-009", "ZBW-PROG-011", "ZBW-PROG-012", "ZBW-PROG-013");
            addM13(definitions, "admin progression", new String[] {"inspect"}, "objectives.manage", false,
                    "ZBW-PROG-010");
            addM13(definitions, "admin quest", new String[] {"assign", "remove"}, "quests.manage", true,
                    "ZBW-PROG-009");
            addM13(definitions, "admin achievement", new String[] {"grant", "inspect"}, "achievements.manage", true,
                    "ZBW-PROG-012");
            addM13(definitions, "admin challenge", new String[] {"modify"}, "challenges.manage", true,
                    "ZBW-PROG-013");
            addM13(definitions, "admin season", new String[] {"inspect", "manage"}, "battlepass.manage", true,
                    "ZBW-PROG-013");
            addM13(definitions, "admin progression", new String[] {"audit"}, "progression.audit", false,
                    "ZBW-PROG-009", "ZBW-PROG-010", "ZBW-PROG-012", "ZBW-PROG-013");
            return Collections.unmodifiableList(definitions);
        }

        /** @return immutable presentation catalogue through M13 */
        public static List<Definition> throughM13() {
            final List<Definition> result = new ArrayList<Definition>(throughM12());
            result.addAll(m13());
            return Collections.unmodifiableList(result);
        }

        /** @return additive immutable M14 cosmetics, profile and calendar actions */
        public static List<Definition> m14() {
            final List<Definition> definitions = new ArrayList<Definition>();
            addM14(definitions, "cosmetics", new String[] {"view", "categories", "rarities", "equip"},
                    "cosmetics", "ZBW-PROG-006", "ZBW-PROG-007", "ZBW-PROG-008");
            addM14(definitions, "profile", new String[] {"view", "settings", "privacy"},
                    "profile", "ZBW-PROG-014");
            addM14(definitions, "campaigns", new String[] {"active"}, "campaigns",
                    "ZBW-PROG-014");
            addM14(definitions, "admin cosmetics", new String[] {"inspect", "grant", "manage"},
                    "cosmetics.manage", "ZBW-PROG-006", "ZBW-PROG-007");
            addM14(definitions, "admin campaigns", new String[] {"manage"}, "campaigns.manage",
                    "ZBW-PROG-014");
            addM14(definitions, "admin profile", new String[] {"inspect", "audit"}, "profiles.manage",
                    "ZBW-PROG-014");
            return Collections.unmodifiableList(definitions);
        }

        /** @return immutable presentation catalogue through M14 */
        public static List<Definition> throughM14() {
            final List<Definition> result = new ArrayList<Definition>(throughM13());
            result.addAll(m14());
            return Collections.unmodifiableList(result);
        }

        private static void add(final List<Definition> result, final String family,
                                final String[] operations, final String permissionFamily,
                                final String... requirementIds) {
            for (String operation : operations) {
                final boolean destructive = operation.matches("delete|restore|rollback|stop|reset|apply");
                final String command = family.equals("deposit") && (operation.equals("hand")
                        || operation.equals("resources") || operation.equals("all"))
                        ? "/deposit " + operation : "/zbw " + family + " " + operation;
                final PermissionNode permission = PermissionNode.of("zartrabedwars."
                        + permissionFamily + "." + operation.replace('-', '.'));
                result.add(new Definition(ActionId.of(family + "/" + operation), command,
                        GuiPageId.of("zartra", "m09/" + family + "/" + operation), permission,
                        destructive, java.util.Arrays.asList(requirementIds)));
            }
        }

        private static void addM10(final List<Definition> result, final String family,
                                   final String[] operations, final String permissionFamily,
                                   final boolean administrative, final String... requirementIds) {
            for (String operation : operations) {
                final boolean destructive = administrative && operation.matches("enable|disable");
                final String commandFamily = family.startsWith("admin-")
                        ? "admin " + family.substring("admin-".length()) : family;
                result.add(new Definition(ActionId.of("m10/" + family + "/" + operation),
                        "/zbw " + commandFamily + " " + operation,
                        GuiPageId.of("zartra", "m10/" + family + "/" + operation),
                        PermissionNode.of("zartrabedwars." + permissionFamily + "."
                                + operation.replace('-', '.')),
                        destructive, java.util.Arrays.asList(requirementIds)));
            }
        }

        private static void addM11(final List<Definition> result, final String family,
                                   final String[] operations, final String permissionFamily,
                                   final boolean administrative, final String... requirementIds) {
            for (String operation : operations) {
                final boolean destructive = administrative && operation.matches("enable|disable|reset");
                result.add(new Definition(ActionId.of("m11/" + family + "/" + operation),
                        "/zbw " + family + " " + operation,
                        GuiPageId.of("zartra", "m11/" + family + "/" + operation),
                        PermissionNode.of("zartrabedwars." + permissionFamily + "."
                                + operation.replace('-', '.')),
                        destructive, java.util.Arrays.asList(requirementIds)));
            }
        }

        private static void addM12(final List<Definition> result, final String family,
                                   final String[] operations, final String permissionFamily,
                                   final boolean administrative, final String... requirementIds) {
            for (String operation : operations) {
                final boolean destructive = operation.matches("claim|grant|grant-xp|adjust|recalculate");
                final String commandFamily = family.startsWith("admin-")
                        ? "admin " + family.substring("admin-".length()) : family;
                result.add(new Definition(ActionId.of("m12/" + family + "/" + operation),
                        "/zbw " + commandFamily + " " + operation,
                        GuiPageId.of("zartra", "m12/" + family + "/" + operation),
                        PermissionNode.of("zartrabedwars." + permissionFamily + "."
                                + (administrative ? "admin." : "")
                                + operation.replace('-', '.')),
                        destructive, java.util.Arrays.asList(requirementIds)));
            }
        }

        private static void addM13(final List<Definition> result, final String commandFamily,
                                   final String[] operations, final String permission,
                                   final boolean mutations, final String... requirementIds) {
            for (String operation : operations) {
                final String family = commandFamily.replace(' ', '-');
                final boolean destructive = mutations && !operation.matches("inspect");
                result.add(new Definition(ActionId.of("m13/" + family + "/" + operation),
                        "/zbw " + commandFamily + " " + operation,
                        GuiPageId.of("zartra", "m13/" + family + "/" + operation),
                        PermissionNode.of("zartrabedwars." + permission), destructive,
                        java.util.Arrays.asList(requirementIds)));
            }
        }

        private static void addM14(final List<Definition> result, final String commandFamily,
                                   final String[] operations, final String permission,
                                   final String... requirementIds) {
            for (String operation : operations) {
                final String family = commandFamily.replace(' ', '-');
                final boolean destructive = operation.matches("equip|settings|privacy|grant|manage");
                result.add(new Definition(ActionId.of("m14/" + family + "/" + operation),
                        "/zbw " + commandFamily + " " + operation,
                        GuiPageId.of("zartra", "m14/" + family + "/" + operation),
                        PermissionNode.of("zartrabedwars." + permission), destructive,
                        java.util.Arrays.asList(requirementIds)));
            }
        }

        private static String[] range(final String prefix, final int first, final int last) {
            final String[] values = new String[last - first + 1];
            for (int value = first; value <= last; value++) {
                values[value - first] = String.format(java.util.Locale.ROOT, "%s%03d", prefix, value);
            }
            return values;
        }

        private static String[] merge(final String[]... groups) {
            int size = 0;
            for (String[] group : groups) { size += group.length; }
            final String[] result = new String[size];
            int index = 0;
            for (String[] group : groups) {
                System.arraycopy(group, 0, result, index, group.length);
                index += group.length;
            }
            return result;
        }
    }
}
