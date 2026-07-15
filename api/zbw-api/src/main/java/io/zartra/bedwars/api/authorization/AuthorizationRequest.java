package io.zartra.bedwars.api.authorization;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Immutable authorization request for one subject, action and protected target. */
public final class AuthorizationRequest {
    private final AuthorizationSubject subject;
    private final PermissionNode action;
    private final DefinitionId target;

    private AuthorizationRequest(final AuthorizationSubject subject, final PermissionNode action,
                                 final DefinitionId target) {
        this.subject = Objects.requireNonNull(subject, "subject");
        this.action = Objects.requireNonNull(action, "action");
        this.target = Objects.requireNonNull(target, "target");
    }

    /** @return request containing no ambient role or platform state */
    public static AuthorizationRequest of(final AuthorizationSubject subject,
                                          final PermissionNode action,
                                          final DefinitionId target) {
        return new AuthorizationRequest(subject, action, target);
    }
    /** @return authenticated caller */ public AuthorizationSubject subject() { return subject; }
    /** @return exact canonical action node */ public PermissionNode action() { return action; }
    /** @return protected target identity */ public DefinitionId target() { return target; }
}
