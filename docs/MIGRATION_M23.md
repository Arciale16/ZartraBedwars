# Migration guide

## Safe workflow

1. Confirm that the input was lawfully obtained and record an operator provenance acknowledgement.
2. Confine the input path outside the migration provider and open it read-only.
3. Run `validate` and `plan` in `DRY_RUN` mode.
4. Review mapped, lossy, unsupported, duplicate and existing-target findings.
5. Select `FAIL`, `KEEP_EXISTING` or `REPLACE` explicitly; the default operational policy is `FAIL`.
6. Verify backup capacity and the migration journal.
7. Apply on a bounded worker while the destination owner provides an atomic target operation.
8. Archive the report and backup identity. Roll back through the recorded migration identity.

The presentation adapter routes these actions through `/zbw admin migrate-layout` with one
canonical permission per action:

- `zartrabedwars.admin.migrate-layout.validate`
- `zartrabedwars.admin.migrate-layout.plan`
- `zartrabedwars.admin.migrate-layout.apply`
- `zartrabedwars.admin.migrate-layout.rollback`
- `zartrabedwars.admin.migrate-layout.report`

Every invocation requires an actor, reason and migration identity and emits bounded before/after
audit records. Apply additionally requires `APPLY` mode; validation and planning remain dry-run.

Dry-run performs no mutation. Apply is rejected when an unsupported record, ambiguous provider,
duplicate output or unresolved conflict exists. A failed target operation triggers immediate
restore of the validated pre-apply snapshot. Source input is never modified.

## Neutral layout format

Each non-comment line is:

```text
id|kind|key=value;key=value
```

Supported kinds are `external-layout-category`, `external-layout-item` and
`external-layout-hotbar`. Categories declare `display-key` and `slot`. Items declare `category`,
`slot`, `material`, `price` and an allowlisted action. Hotbar entries declare slots 0–8 and an
allowlisted intent. Unknown metadata is reported as lossy and
omitted.

No external brand, protected layout, executable command, script or arbitrary serialized object is
accepted. Operators must retain the provenance acknowledgement and must not redistribute source
exports.

## Recovery

Do not edit journal or backup identifiers. If apply fails, verify that the returned status is
`FAILED` and that `apply-failed-restored` is present. If the process stops after a prepared journal
entry, keep the destination quiescent and restore its recorded backup before retrying. A rollback
against a missing or non-applied identity is rejected without mutation.
