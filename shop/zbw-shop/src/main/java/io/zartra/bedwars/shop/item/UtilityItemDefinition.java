package io.zartra.bedwars.shop.item;

import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.shop.api.ShopCatalog;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Immutable platform-neutral definition of a bounded utility-item action. */
public final class UtilityItemDefinition {
    /** Supported M11 action families; concrete platform effects remain adapter-owned. */
    public enum Kind {
        /** Animated defensive structure. */ POPUP_TOWER,
        /** Accelerated bridge/defence action. */ RUSH,
        /** Selected class-like ability. */ ULTIMATE,
        /** Bed token, upgrade or combat action. */ BEDSTEAL,
        /** Voidless fall recovery or defence action. */ VOIDLESS,
        /** Sponge animation/status action. */ SPONGE,
        /** Local generator behavior adjustment. */ GENERATOR,
        /** Local item-rotation activation. */ ITEM_ROTATION
    }

    /** Target validation rule. */
    public enum TargetRule {
        /** No target is accepted. */ NONE,
        /** Target must be the actor. */ SELF,
        /** Target must be a living teammate. */ TEAMMATE,
        /** Target must be an eligible enemy. */ ENEMY,
        /** Target is a validated arena location. */ LOCATION,
        /** Target is an owned active bed. */ OWN_BED,
        /** Target is an eligible enemy bed. */ ENEMY_BED,
        /** Target is a local generator. */ GENERATOR
    }

    private final DefinitionId id;
    private final DefinitionId semanticItem;
    private final Kind kind;
    private final PermissionNode permission;
    private final ShopCatalog.Price cost;
    private final Duration cooldown;
    private final TargetRule targetRule;
    private final int perMatchLimit;
    private final Map<String, Long> parameters;

    /** Creates a fully validated definition with bounded numeric parameters. */
    public UtilityItemDefinition(final DefinitionId id, final DefinitionId semanticItem,
                                 final Kind kind, final PermissionNode permission,
                                 final ShopCatalog.Price cost, final Duration cooldown,
                                 final TargetRule targetRule, final int perMatchLimit,
                                 final Map<String, Long> parameters) {
        this.id = Objects.requireNonNull(id, "id");
        this.semanticItem = Objects.requireNonNull(semanticItem, "semanticItem");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.permission = Objects.requireNonNull(permission, "permission");
        this.cost = Objects.requireNonNull(cost, "cost");
        this.cooldown = Objects.requireNonNull(cooldown, "cooldown");
        this.targetRule = Objects.requireNonNull(targetRule, "targetRule");
        if (cooldown.isNegative() || cooldown.compareTo(Duration.ofDays(1)) > 0
                || perMatchLimit < 1 || perMatchLimit > 10_000) {
            throw new IllegalArgumentException("cooldown or match limit is out of range");
        }
        this.perMatchLimit = perMatchLimit;
        final Map<String, Long> copy = new TreeMap<String, Long>();
        for (Map.Entry<String, Long> entry : Objects.requireNonNull(parameters, "parameters").entrySet()) {
            if (!entry.getKey().matches("[a-z][a-z0-9_.-]{1,63}") || entry.getValue() == null
                    || entry.getValue() < 0L || entry.getValue() > 1_000_000L) {
                throw new IllegalArgumentException("invalid action parameter");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        if (copy.size() > 32) { throw new IllegalArgumentException("too many action parameters"); }
        this.parameters = Collections.unmodifiableMap(copy);
    }

    /** @return action identity */ public DefinitionId id() { return id; }
    /** @return semantic inventory item */ public DefinitionId semanticItem() { return semanticItem; }
    /** @return action family */ public Kind kind() { return kind; }
    /** @return exact permission */ public PermissionNode permission() { return permission; }
    /** @return atomic match-resource cost */ public ShopCatalog.Price cost() { return cost; }
    /** @return action cooldown */ public Duration cooldown() { return cooldown; }
    /** @return required target relationship */ public TargetRule targetRule() { return targetRule; }
    /** @return maximum successful executions per match */ public int perMatchLimit() { return perMatchLimit; }
    /** @return immutable bounded parameters */ public Map<String, Long> parameters() { return parameters; }
    /** @return named parameter if configured */
    public Optional<Long> parameter(final String name) {
        return Optional.ofNullable(parameters.get(Objects.requireNonNull(name, "name")));
    }
}
