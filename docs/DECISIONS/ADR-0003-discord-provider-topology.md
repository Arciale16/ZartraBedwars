# ADR-0003: Discord Provider Topology

- **Status:** Accepted
- **Date:** 2026-07-14
- **Owner:** Project owner
- **Affected requirements:** `ZBW-DISCORD-001..008`, `ZBW-OPS-007`
- **Resolves:** RC-074

## Context

Discord notifications, commands, statistics, leaderboards and account linking have different security and deployment needs. The Minecraft plugin must remain playable without Discord and must not own a bot token when a safer external process is available.

## Decision

Adopt the provider architecture in `docs/DISCORD_ARCHITECTURE.md`: a secure internal query/event gateway, disabled provider by default, embedded outbound webhook provider, external bot transport/provider and custom provider SPI.

Gameplay commits before integration delivery. A bounded outbox, timeout, circuit, rate and dead-letter policy isolates all provider failures. The external process owns the bot token; normal plugin configuration contains secret references only.

## Alternatives considered

| Alternative | Benefit | Rejection reason |
|---|---|---|
| Mandatory embedded bot | One process | Couples startup/gameplay to Discord and exposes tokens in Minecraft JVM |
| Webhooks only | Simple | Cannot support commands, linking and interactive statistics |
| Provider gateway with optional adapters | Flexible and failure-isolated | Selected |

## Consequences

- External bot protocol credentials must be selected before M16.
- Custom providers use only public scoped contracts.
- Discord outage changes integration health only.
- Additional process deployment is required for interactive Discord features, not for plugin gameplay.

## Acceptance evidence

Provider contracts, no-provider full E2E, outage/backpressure tests, scoped query/link security, replay protection, secret redaction, thread guards and rolling protocol compatibility tests.

## Updated documents

PRD, architecture, Discord architecture, milestones, traceability, risks and coverage report.
