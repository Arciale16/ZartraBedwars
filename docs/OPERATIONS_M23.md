# M23 operator operations and troubleshooting

## Plugin Doctor

`OperationalReadinessCheck` requires one registered probe for each category: compatibility,
provider, dependency, configuration and migration. Probe IDs are unique and results are sorted.
Warnings produce `DEGRADED`; any blocked prerequisite produces `UNAVAILABLE`. Evidence is bounded,
public and passes through the existing final diagnostic sanitizer.

Recommended probes verify the exact runtime row, provider compatibility, dependency lock, validated
configuration, lawful provenance, backup capacity and journal availability. Probes inspect only;
they do not load plugins, rewrite configuration or run migrations.

Migration is disabled by default. Before enabling it, retain the `FAIL` conflict policy unless an
operator has reviewed the dry-run report, keep backup-required enabled, and never raise the one-MiB
source or 10,000-record limits beyond the compiled safety ceiling.

## Common failures

- **Unsupported record:** install an approved conversion provider or remove the record from the
  operator-owned export; never coerce it silently.
- **Existing target:** rerun the plan with an explicitly reviewed conflict policy.
- **Duplicate output:** correct the source ID mapping; output IDs must be unique.
- **Ambiguous provider:** remove one provider registration. Selection is never load-order based.
- **Apply failed and restored:** inspect the sanitized failure class, destination health and backup
  evidence before retrying with a new migration identity.
- **Migration not applied:** rollback was requested for an absent, failed or already rolled-back
  journal entry; no mutation occurred.
- **Doctor unavailable:** resolve every blocked probe before apply. Optional provider absence may
  remain a warning only when the owning feature documents a safe fallback.

Run migrations off the Minecraft owner thread. Executors and queues must be bounded, shutdown must
drain or reject work explicitly, and exported diagnostics must contain no secrets or private data.
