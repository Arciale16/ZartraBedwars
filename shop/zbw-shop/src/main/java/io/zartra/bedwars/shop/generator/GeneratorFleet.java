package io.zartra.bedwars.shop.generator;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.game.model.MatchSnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Deterministically ordered fleet of independently timed and bounded generators. */
public final class GeneratorFleet {
    private final List<GeneratorRuntime> generators;
    /** Creates one runtime for every arena-plan configuration. */
    public GeneratorFleet(final MatchId matchId, final ArenaGeneratorPlan plan) {
        final List<GeneratorRuntime> values = new ArrayList<GeneratorRuntime>();
        for (GeneratorConfiguration configuration : Objects.requireNonNull(plan, "plan").configurations()) {
            values.add(new GeneratorRuntime(matchId, configuration));
        }
        generators = Collections.unmodifiableList(values);
    }
    /** Starts enabled generators when the M08 snapshot permits gameplay. */
    public void start(final MatchSnapshot match, final Instant now) {
        for (GeneratorRuntime generator : generators) { if (generator.configuration().enabled()) { generator.start(match, now); } }
    }
    /** Ticks generators in stable ID order. */
    public int tick(final MatchSnapshot match, final Instant now, final ResourceDeliveryPort delivery) {
        int generated = 0;
        for (GeneratorRuntime generator : generators) { generated += generator.tick(match, now, delivery); }
        return generated;
    }
    /** Cleans all runtimes at completion/reset. */ public void cleanup() { for (GeneratorRuntime generator : generators) { generator.cleanup(); } }
    /** @return immutable runtimes */ public List<GeneratorRuntime> generators() { return generators; }
}
