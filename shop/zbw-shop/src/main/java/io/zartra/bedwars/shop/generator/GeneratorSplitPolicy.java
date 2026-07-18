package io.zartra.bedwars.shop.generator;

import io.zartra.bedwars.api.identity.PlayerId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic fair allocation for indivisible generator-split amounts. */
public final class GeneratorSplitPolicy {
    private GeneratorSplitPolicy() { }
    /** Splits an amount among sorted recipients and rotates remainders by sequence. */
    public static Map<PlayerId, Integer> split(final int amount, final long sequence,
                                               final List<PlayerId> eligible) {
        if (amount < 1 || sequence < 1L) { throw new IllegalArgumentException("amount and sequence must be positive"); }
        final List<PlayerId> players = new ArrayList<PlayerId>(Objects.requireNonNull(eligible, "eligible"));
        if (players.contains(null)) { throw new IllegalArgumentException("eligible contains null"); }
        Collections.sort(players, new Comparator<PlayerId>() {
            @Override public int compare(final PlayerId left, final PlayerId right) { return left.toString().compareTo(right.toString()); }
        });
        for (int index = 1; index < players.size(); index++) {
            if (players.get(index - 1).equals(players.get(index))) { throw new IllegalArgumentException("eligible contains duplicate player"); }
        }
        final Map<PlayerId, Integer> result = new LinkedHashMap<PlayerId, Integer>();
        if (players.isEmpty()) { return Collections.unmodifiableMap(result); }
        final int base = amount / players.size();
        final int remainder = amount % players.size();
        final int offset = (int) ((sequence - 1L) % players.size());
        for (int index = 0; index < players.size(); index++) {
            final int rotated = (index - offset + players.size()) % players.size();
            final int share = base + (rotated < remainder ? 1 : 0);
            if (share > 0) { result.put(players.get(index), share); }
        }
        return Collections.unmodifiableMap(result);
    }
}
