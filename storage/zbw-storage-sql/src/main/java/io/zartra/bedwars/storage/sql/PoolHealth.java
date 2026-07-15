package io.zartra.bedwars.storage.sql;

/** Immutable sanitized Hikari pool health snapshot. */
public final class PoolHealth {
    private final int active;
    private final int idle;
    private final int total;
    private final int waiting;

    private PoolHealth(final int active, final int idle, final int total, final int waiting) {
        if (active < 0 || idle < 0 || total < 0 || waiting < 0 || active + idle > total) {
            throw new IllegalArgumentException("invalid pool counters");
        }
        this.active = active;
        this.idle = idle;
        this.total = total;
        this.waiting = waiting;
    }
    /** @return validated health snapshot */
    public static PoolHealth of(final int active, final int idle, final int total, final int waiting) {
        return new PoolHealth(active, idle, total, waiting);
    }
    /** @return active connections */ public int active() { return active; }
    /** @return idle connections */ public int idle() { return idle; }
    /** @return total connections */ public int total() { return total; }
    /** @return threads awaiting a connection */ public int waiting() { return waiting; }
    /** @return whether demand is waiting on a saturated pool */
    public boolean saturated() { return waiting > 0 && idle == 0; }
}
