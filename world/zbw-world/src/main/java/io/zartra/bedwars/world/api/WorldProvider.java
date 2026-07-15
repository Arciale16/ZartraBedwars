package io.zartra.bedwars.world.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.scheduler.CancellationToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Provider SPI for bounded, rollback-capable world lifecycle plans. */
public interface WorldProvider {
    /** @return stable provider ID */ ProviderId id();
    /** @return a bounded immutable plan; plan construction performs no I/O */ Plan plan(WorldOperation operation);
    /** @return fast, owner-safe resource snapshot with no platform objects */ ResourceSnapshot snapshot(WorldKey world);

    /** Immutable bounded operation plan. */
    final class Plan {
        private final WorldOperation operation;
        private final List<Step> steps;
        /** Creates a plan with one to sixteen ordered steps. */
        public Plan(final WorldOperation operation, final List<Step> steps) {
            this.operation = Objects.requireNonNull(operation, "operation");
            final List<Step> copy = new ArrayList<Step>(Objects.requireNonNull(steps, "steps"));
            if (copy.isEmpty() || copy.size() > 16 || copy.contains(null)) {
                throw new IllegalArgumentException("world plan requires one to sixteen steps");
            }
            this.steps = Collections.unmodifiableList(copy);
        }
        /** @return original request */ public WorldOperation operation() { return operation; }
        /** @return immutable ordered steps */ public List<Step> steps() { return steps; }
    }

    /** One provider-owned step with explicit thread affinity and compensation. */
    interface Step {
        /** @return stable step ID */ DefinitionId id();
        /** @return required execution context */ Affinity affinity();
        /** @return execution outcome; never null */ StepResult execute(CancellationToken cancellation);
        /** @return idempotent compensation outcome; never null */ StepResult rollback(CancellationToken cancellation);
    }

    /** Thread context for a world step. */ enum Affinity { WORKER, OWNER }

    /** Immutable typed step result. */
    final class StepResult {
        private final boolean success;
        private final DefinitionId reason;
        private StepResult(final boolean success, final DefinitionId reason) {
            this.success = success;
            this.reason = Objects.requireNonNull(reason, "reason");
        }
        /** @return successful result */ public static StepResult success() { return new StepResult(true, DefinitionId.of("zartra", "world/step_success")); }
        /** @return failed result with stable reason */ public static StepResult failure(final DefinitionId reason) { return new StepResult(false, reason); }
        /** @return whether the step completed */ public boolean isSuccess() { return success; }
        /** @return stable reason */ public DefinitionId reason() { return reason; }
    }

    /** Immutable leak-detection and resource-accounting snapshot. */
    final class ResourceSnapshot {
        private final boolean loaded;
        private final int loadedChunks;
        private final int entities;
        private final int retainedHandles;
        /** Creates non-negative resource evidence. */
        public ResourceSnapshot(final boolean loaded, final int loadedChunks, final int entities,
                                final int retainedHandles) {
            if (loadedChunks < 0 || entities < 0 || retainedHandles < 0) {
                throw new IllegalArgumentException("resource counters cannot be negative");
            }
            this.loaded = loaded;
            this.loadedChunks = loadedChunks;
            this.entities = entities;
            this.retainedHandles = retainedHandles;
        }
        /** @return loaded state */ public boolean loaded() { return loaded; }
        /** @return loaded chunk count */ public int loadedChunks() { return loadedChunks; }
        /** @return live entity count */ public int entities() { return entities; }
        /** @return provider-owned retained resource count */ public int retainedHandles() { return retainedHandles; }
        /** @return true only when an unloaded world has no retained resources */
        public boolean leakFreeAfterUnload() {
            return !loaded && loadedChunks == 0 && entities == 0 && retainedHandles == 0;
        }
    }
}
