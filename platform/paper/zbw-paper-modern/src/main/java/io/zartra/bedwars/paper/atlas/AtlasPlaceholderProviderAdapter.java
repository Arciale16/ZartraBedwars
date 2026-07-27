package io.zartra.bedwars.paper.atlas;

import io.zartra.bedwars.atlas.core.AtlasIntegrationContracts.AtlasQueryProvider;
import io.zartra.bedwars.integration.placeholderapi.PlaceholderDataProvider;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Non-blocking Atlas data providers for the existing M16 provider boundary. */
public final class AtlasPlaceholderProviderAdapter {
    private static final int MAX_ENTRIES = 2_048;
    private final AtlasQueryProvider source;
    private final Map<UUID, String> status = boundedMap();
    private final Map<UUID, String> reputation = boundedMap();
    public AtlasPlaceholderProviderAdapter(final AtlasQueryProvider source) { this.source = source; }

    public void refresh(final UUID playerId) {
        source.reviewerStatus(playerId).thenAccept(value -> update(status, playerId, value));
        source.reputationSummary(playerId).thenAccept(value -> update(reputation, playerId, value));
    }
    public PlaceholderDataProvider<String> reviewerStatus() {
        return id -> Optional.ofNullable(status.get(id));
    }
    public PlaceholderDataProvider<String> reputationSummary() {
        return id -> Optional.ofNullable(reputation.get(id));
    }
    private static Map<UUID, String> boundedMap() {
        return Collections.synchronizedMap(new LinkedHashMap<UUID, String>() {
            @Override protected boolean removeEldestEntry(
                    final Map.Entry<UUID, String> eldest) {
                return size() > MAX_ENTRIES;
            }
        });
    }
    private static void update(final Map<UUID, String> values, final UUID id,
                               final Optional<String> value) {
        if (value.isPresent()) {
            values.put(id, value.get());
        } else {
            values.remove(id);
        }
    }
}
