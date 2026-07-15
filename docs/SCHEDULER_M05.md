# M05 scheduler and threading guide

`BoundedTaskScheduler` uses a fixed worker count, `ArrayBlockingQueue`, named daemon threads and
an explicit abort policy. Every submission has a unique `TaskId`, stable operation/owner IDs,
correlation ID, idempotency declaration and queue-plus-execution deadline.

Admission has two outcomes: accepted work receives a handle, while stopped or saturated admission
receives an immediately completed structured `REJECTED` outcome. Work is never silently discarded.
Accepted tasks complete with a value or a secret-safe `FailureReport`; an exception in the task or
failure sink does not terminate the executor or cascade into gameplay. Runtime deadlines are
cooperative; the token also expires while queued and is checked before and after operation
execution. Cancellation removes queued work when possible and interrupts running work.

Shutdown first stops admission, calls graceful drain for the configured budget, then interrupts
remaining work and explicitly completes every returned queued task as cancelled. The immutable
shutdown report records whether escalation occurred, how many queued tasks were force-cancelled
and whether all workers terminated. Blocking shutdown must never run on a Minecraft owner thread.

`StrictThreadGuard` delegates ownership detection to `OwnerThreadDispatcher`. It fails fast when
a mutation is attempted off its owner context or blocking work is attempted on that context.
M05 defines and tests this boundary but does not implement a Minecraft dispatcher.

Configuration keys in `performance.yml`: `scheduler.worker-count`,
`scheduler.queue-capacity`, `scheduler.default-task-timeout`,
`lifecycle.graceful-drain-budget` and `lifecycle.force-stop-budget`.
