# M05 observability and failure guide

`BoundedHealthRegistry` has a hard source capacity, deterministic ordering and duplicate-ID
rejection. A throwing source is isolated as UNAVAILABLE with a stable reason; no exception text is
exported. `BoundedMetricRegistry` caps series and dimensions, accepts only 1..64-character safe
labels and produces sorted immutable samples. Metric labels must use bounded state/category values,
never player, match, replay, case, endpoint or free-form values.

`FailureReport` carries only a namespaced code, taxonomy, correlation ID, localization key,
retryability and observation time. Raw exception text, stack trace, credentials and caller input
are absent. Retry policy permits only explicitly idempotent UNAVAILABLE, TIMEOUT or CONFLICT work,
caps attempts at sixteen and exposes bounded exponential delays without hidden sleeps.
`CircuitBreaker` provides closed/open/half-open states and one recovery probe.

`SafeDiagnosticExporter` accepts only PUBLIC fields on an explicit allowlist, bounds contributors,
fields and value length, applies an injected sanitizer and rejects an export if a sensitive seed
remains. Its exact-seed sanitizer redacts configured seeds and common token/password forms and
zeroizes copied seed characters on close. PRIVATE and SECRET fields never enter standard exports.

Configuration keys cap metric series, health sources, diagnostic contributors/export fields and
Doctor checks/timeouts. M24 will add operator dashboards and platform/provider-specific sources;
M05 supplies the safe substrate only.
