# ADR-0001: RESOURCE SCARCITY Private Games Modifier

- **Status:** Accepted
- **Date:** 2026-07-14
- **Owner:** Project owner
- **Affected requirements:** `ZBW-ADDON-464..473`, `ZBW-CONTENT-008`
- **Resolves:** RC-072

## Context

The source reference advertised eleven Private Games modifiers while its public description named ten. Copying or guessing hidden proprietary behavior was not acceptable. The owner selected an original eleventh modifier.

## Decision

RESOURCE SCARCITY independently multiplies iron, gold, diamond, emerald and each registered custom-resource generator rate. It provides versioned Scarce, Reduced, Normal, Abundant and Extreme presets plus per-resource overrides. Authorized hosts manage it in the private-game settings GUI; configuration, command, permission, API/event and placeholder surfaces are mandatory.

The generator service remains the single scheduler. The modifier supplies an effective rate policy to native/custom providers. Changes lock at countdown start by default; dynamic mutation is allowed only for providers declaring `DYNAMIC_RATE_SAFE` and must apply transactionally.

## Alternatives considered

| Alternative | Benefit | Rejection reason |
|---|---|---|
| Guess the missing third-party modifier | Superficial catalogue similarity | Proprietary/unknown behavior and no owner intent |
| Generic unnamed modifier slot | Extensible | Does not satisfy an original built-in eleventh capability |
| RESOURCE SCARCITY | Original, useful and generator-neutral | Selected |

## Consequences

- Generator providers must expose rate-policy capability and stable custom resource IDs.
- Invalid multipliers, unknown resources and unsafe mid-game changes are rejected without rescheduling duplicates.
- Proxy/rejoin persistence stores preset ID plus explicit overrides.
- Balance values live in `docs/ORIGINAL_STARTER_CATALOG.md` and later validated configuration.

## Acceptance evidence

Multiplier/preset unit tests, native/custom provider contracts, permission/GUI E2E, reconnect/proxy persistence, scheduler duplication and dynamic-rate rollback tests are required before M20 exits.

## Updated documents

PRD, architecture, milestones, traceability, risk register, addon catalogue, starter catalogue and combined coverage report.
