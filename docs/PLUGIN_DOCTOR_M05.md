# M05 Plugin Doctor extension guide

Extensions and later adapters register unique `PluginDoctor.Check` implementations. Registration
is capacity bounded. Each check runs through `SchedulerPort` with its own task ID, correlation,
timeout and idempotent classification. Rejection, timeout or exception becomes an UNAVAILABLE check
result; it cannot fail the whole report or gameplay.

Checks receive `TaskContext`, must poll cancellation during long work, must perform no owner-thread
mutation and must return bounded evidence. The engine drops PRIVATE/SECRET evidence, sanitizes every
PUBLIC value at the final boundary and converts sanitization or evidence-capacity failure into an
isolated failed check. Results are sorted by stable check ID.

M05 provides the SPI and engine. Server/JDK/plugin/world/database/Redis/proxy/CloudNet checks,
operator command/GUI surfaces and downloadable support bundles are integrated by their owning
milestones. A Doctor provider must remain optional and cannot become a gameplay dependency.
