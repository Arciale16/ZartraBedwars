package io.zartra.bedwars.application.capability;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import java.util.Objects;

/** Stateless policy that accepts a provider only when every required capability is declared. */
public final class ProviderCapabilityPolicy {
    private static final ApiError MISSING = ApiError.of(DefinitionId.of("zartra", "provider/missing_capability"),
            "provider.capability.missing", ApiError.RetryDisposition.PERMANENT);

    /**
     * Evaluates required capabilities without mutating either set.
     *
     * @return offered capabilities on success, or a typed missing-capability failure
     */
    public Result<CapabilitySet> evaluate(final CapabilitySet required, final CapabilitySet offered) {
        Objects.requireNonNull(required, "required");
        Objects.requireNonNull(offered, "offered");
        return offered.containsAll(required) ? Result.success(offered) : Result.failure(MISSING);
    }
}
