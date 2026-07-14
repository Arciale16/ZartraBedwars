# Repository instructions for Codex and contributors

1. Before every task, read `docs/PRD/PRD.md` and `docs/REQUIREMENTS_TRACEABILITY.md`; read the relevant architecture, milestone, risk and ADR sections.
2. Cite affected Requirement IDs in plans, changes, tests, commits and pull requests. Do not change/remove/weakening an ID without explicit owner approval and corresponding PRD/ADR/traceability updates.
3. Preserve module boundaries: domain/core must not import Bukkit/Paper/NMS, storage, Redis, proxy or provider implementations. Use public ports/adapters and no circular dependencies.
4. Never add TODO, FIXME, stub, placeholder implementation, fake/mock-only production behavior or claim completion without evidence. Do not silently reduce scope.
5. Do not block a Minecraft owner/tick thread with database, Redis, network, filesystem, compression or large calculations. Use bounded queues/executors, backpressure, timeouts and documented thread ownership.
6. Treat commands, permissions, GUI, API/events, PlaceholderAPI, configuration, localization, migration, performance, security, compatibility, tests and documentation as part of each feature—not follow-up work.
7. Use original code/assets only. Protect secrets and private moderation/replay data; sensitive actions require least privilege and audit.
8. Every change must leave a clean build and pass relevant unit, integration, regression, compatibility, migration, security and performance checks. Add regression coverage for bug fixes.
9. Update PRD/traceability, ADRs, risks, milestones, configuration/reference docs and release notes whenever the change affects them.
10. A feature is complete only when its PRD acceptance criteria and global Definition of Done are met. Report unresolved decisions or blocked criteria honestly.
