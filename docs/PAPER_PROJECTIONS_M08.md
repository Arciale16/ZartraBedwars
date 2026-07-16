# M08 primary Paper projections

The certified target is Paper 1.21.1 build 133 on Java 21. Closed adapters translate
quit, movement, damage and death into typed game inputs and apply player state, hotbar,
deposit, scoreboard, tab-list and native boss-bar effects. They contain no game policy.

All mutation methods require `Bukkit.isPrimaryThread()`. Inputs are immutable and fully
resolved before projection; no database, filesystem, network or placeholder query runs
inside a renderer. Scoreboards and tab entries are replaced only when owned, viewers are
privacy-filtered, stale entries/bars/listeners are removed on exit/disable, and bounded
diff/cadence policy is computed in `zbw-game`.

The approved Paper API mirror intentionally exposes no transitive Adventure libraries.
ADR-0020 therefore uses a narrow validated reflection bridge for server-bound values;
semantic mapping and all rules remain typed. The exact-runtime certification prevents
compile-only success from being treated as runtime support. M22 retains all non-primary
runtime adapters and fallback certification.

Direct Bukkit-bound classes cannot execute safely in the unit-test JVM and are excluded
from its JaCoCo denominator, as are the existing M06 bootstrap/provider boundaries. The
Paper module's remaining code must still meet 80% line/70% branch coverage, while the
excluded M08 classes must pass `m08_paper_e2e.py` on the locked real server. Either gate
failing fails M08 verification.
