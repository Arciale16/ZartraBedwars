# M09 command framework

`CommandModel.Node` is an immutable tree of stable IDs, aliases, children, typed argument specs,
sender rules, message keys, permission, target resolver, timeout and executor. `CommandFramework`
parses without platform state, rejects missing/extra/malformed arguments, revalidates authorization
immediately before submission, applies per-subject cooldown, publishes sanitized audit lifecycle
records and returns cancellable structured execution results.

`BoundedCommandSupervisor` uses fixed workers, a bounded rejection queue, deadline scheduler,
observable in-flight/queued counts and bounded drain. No database, filesystem or network load runs
on the owner thread. `PaperCommandAdapter` translates a sender to the M03 authorization subject,
dispatches through the neutral framework and schedules localized output on the owner thread.

`UnifiedCommandTreeFactory` generates the `/zbw` tree and `/deposit` family from the same 87-action
catalogue used by GUIs. Complete paths and permissions are generated in `COMMANDS.md` and
`PERMISSIONS.md`; their JSON counterparts are the machine-readable source for operations tooling.
