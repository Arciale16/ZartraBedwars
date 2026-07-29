package io.zartra.bedwars.api.integration.permission;

import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import java.util.concurrent.CompletionStage;

/** Context-aware permission and metadata provider without profile ownership. */
public interface PermissionProvider extends Provider {
    /**
     * Evaluates a canonical permission.
     *
     * @param query immutable player/context query
     * @param permission canonical permission node
     * @return asynchronous authorization result
     */
    CompletionStage<Result<Boolean>> hasPermission(ContextQuery query, PermissionNode permission);

    /**
     * Reads an allow-listed metadata projection.
     *
     * @param query immutable player/context query
     * @return asynchronous metadata result
     */
    CompletionStage<Result<MetaSnapshot>> metadata(ContextQuery query);
}
