package io.zartra.bedwars.shop.api;

import io.zartra.bedwars.api.authorization.PermissionNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** Immutable price, grant and optimistic-state quote accepted by atomic commit. */
public final class PurchaseQuote {
    private final PurchaseRequest request;
    private final long catalogRevision;
    private final long stateRevision;
    private final ShopCatalog.Price price;
    private final int grantQuantity;
    private final List<PermissionNode> requiredPermissions;
    private final Instant issuedAt;
    private final Instant expiresAt;

    /** Creates an internally validated quote. */
    public PurchaseQuote(final PurchaseRequest request, final long catalogRevision,
                         final long stateRevision, final ShopCatalog.Price price,
                         final int grantQuantity,
                         final Collection<PermissionNode> requiredPermissions,
                         final Instant issuedAt,
                         final Instant expiresAt) {
        this.request = Objects.requireNonNull(request, "request");
        if (catalogRevision < 1 || stateRevision < 0) {
            throw new IllegalArgumentException("quote revisions are invalid");
        }
        this.catalogRevision = catalogRevision;
        this.stateRevision = stateRevision;
        this.price = Objects.requireNonNull(price, "price");
        if (grantQuantity < 1) { throw new IllegalArgumentException("grantQuantity must be positive"); }
        this.grantQuantity = grantQuantity;
        final TreeSet<PermissionNode> permissions = new TreeSet<PermissionNode>();
        for (PermissionNode permission
                : Objects.requireNonNull(requiredPermissions, "requiredPermissions")) {
            permissions.add(Objects.requireNonNull(permission, "permission"));
        }
        if (permissions.isEmpty()) {
            throw new IllegalArgumentException("requiredPermissions must not be empty");
        }
        this.requiredPermissions = Collections.unmodifiableList(
                new ArrayList<PermissionNode>(permissions));
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) { throw new IllegalArgumentException("quote expiry must follow issue time"); }
    }

    /** @return original request */ public PurchaseRequest request() { return request; }
    /** @return catalog revision used for validation */ public long catalogRevision() { return catalogRevision; }
    /** @return inventory state revision used for validation */ public long stateRevision() { return stateRevision; }
    /** @return exact multi-resource charge */ public ShopCatalog.Price price() { return price; }
    /** @return exact item units granted */ public int grantQuantity() { return grantQuantity; }
    /** @return immutable authorization nodes that must be revalidated at execution */
    public List<PermissionNode> requiredPermissions() { return requiredPermissions; }
    /** @return quote issue instant */ public Instant issuedAt() { return issuedAt; }
    /** @return exclusive expiry instant */ public Instant expiresAt() { return expiresAt; }
}
