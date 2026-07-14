# Network Security and Distributed Consistency

**Status:** Accepted
**Decisions:** RC-046, RC-050
**Requirements:** `ZBW-READY-013`, `ZBW-READY-014`

## Trust and authority model

| Component | Authoritative for | Never authoritative for |
|---|---|---|
| Match backend holding current epoch lease | Live match state and admission for that lease epoch | Durable profile/reward/stat truth after commit |
| MySQL/MariaDB writer | Identity, progression, currency, stats, reward ledger, reservations, replay metadata, cases, outbox/inbox, audit | Live entity/world state |
| SQLite | Same durable classes only inside one shared-server JVM with one serialized writer | Any multi-JVM or network deployment |
| Redis | Presence, health, cache invalidation, short reservations, streams and fenced coordination | Durable money/reward/stat/case/replay truth |
| Proxy | Routing and possession of a single-use transfer token | Match rules, rewards or player eligibility truth |
| Discord/external service | Display/query and explicitly scoped requests | Direct gameplay mutation or unrestricted staff authority |

`SCALABLE_PROXY` startup fails closed unless a reachable approved MySQL or MariaDB writer and authenticated Redis namespace are configured. It may never silently fall back to SQLite.

## Message envelope

Every Redis stream/pub-sub record, proxy plugin message, backend heartbeat, reservation, CloudNet callback and external-bot event uses:

- protocol name and semantic schema version;
- installation ID, environment and destination audience;
- UUIDv7 message ID and separate idempotency/operation ID;
- producer node ID and boot epoch; aggregate ID/version where applicable;
- UTC issued-at, absolute deadline and monotonic local duration metadata;
- cryptographically random 128-bit nonce;
- payload type, content type and byte length;
- key ID and HMAC-SHA-256 signature over canonical header+payload, or mutually authenticated TLS identity for a protocol that provides equivalent per-peer integrity;
- optional trace ID with no player/private data.

Receivers authenticate before deserialization, enforce exact destination and schema, reject >30 seconds future/past skew or expired deadlines, cache nonces for five minutes, retain message/operation dedupe for 24 hours and apply per-type size/rate limits. Default limits are 64 KiB for proxy/plugin messages, 256 KiB for Redis records, 1 MiB for external control requests and 100 messages/second/peer with a burst of 200; replay payload chunks use a separate authenticated object-store channel.

## Key and transport policy

- Production SQL, Redis, proxy control and external connections use TLS when their transport supports it; certificate/hostname verification is mandatory.
- HMAC keys are 256 random bits, never passwords. Normal config stores only `SecretRef`; logs/diagnostics show key ID, not material.
- At least two key slots permit rotation. New key signs immediately; old key verifies for a maximum 24-hour overlap; emergency revoke is immediate. Rotate every 90 days and after suspected exposure.
- Each peer/service receives a distinct least-privilege credential and allowlisted message types/destinations. Development shared keys are forbidden in production.
- Clock offset >15 seconds alerts; >30 seconds blocks new cross-node mutations while local safe play follows the degradation policy.

## Consistency operations

| Operation | Protocol and invariant |
|---|---|
| Reward/currency/stat mutation | SQL transaction writes ledger/domain row + outbox with unique operation ID. Consumer inbox/unique constraint makes re-delivery a no-op. Acknowledgement means durable commit, not Redis publication. |
| Arena ownership | SQL/coordination lease has monotonically increasing epoch/fencing token and expiry. Backend presents token on admission/finalization; stale epochs cannot commit. |
| Player transfer | Proxy requests reservation; backend stores single-use token hash, player/target/epoch/expiry. Token expires after 15 seconds and consuming it is atomic/idempotent. |
| Party/private-game change | Authoritative SQL aggregate version and operation ID; stale concurrent write returns conflict and caller reloads. |
| Cache invalidation | Redis is at-least-once and disposable; versioned cache entries detect stale data. Cache loss causes bounded async reload, never loss of durable state. |
| Singleton schedule | Fenced lease only; work records execution key in SQL. Lock loss stops work before side effects. |

Retries use exponential jitter and only operations proven idempotent are retried automatically. Defaults: three attempts, 50/150/450 ms for Redis; three attempts, 100/300/900 ms for transient SQL deadlock; no blind retry after an ambiguous external side effect. Compensating transactions are explicit ledger entries.

## Partition and degradation behavior

- SQL unavailable: active matches may continue in memory for up to 60 seconds only if their outcome can be durably journaled to the encrypted bounded recovery spool; new paid purchases, claims, admissions requiring eligibility and match finalization pause. At spool saturation, admission stops.
- Redis unavailable in shared-server mode: local matches continue; cross-node queue/party/proxy/private-game actions reject as temporarily unavailable. In proxy mode new reservations stop; existing owned matches continue.
- Proxy/backend uncertainty: do not admit a player twice. Expired reservation returns player to configured lobby; stale backend epoch drains.
- Duplicate/out-of-order messages: dedupe or reject by aggregate version; never “last timestamp wins” for balances, rewards, ownership or moderation.
- Unsupported schema/key: quarantine bounded metadata, emit sanitized alert and reject; never deserialize permissively.

## Verification

Security tests forge signatures, substitute destination/environment, replay nonce/message/operation IDs, skew clocks, send expired/oversized/unknown-schema payloads, exhaust rate limits, rotate/revoke keys and fuzz parsers. Consistency tests duplicate and reorder every operation, kill processes before/after commits, partition SQL/Redis/proxy, expire leases and perform rolling mixed-version upgrades. Acceptance is zero unauthorized acceptance, zero duplicate business effect, zero concurrent owner and a documented safe user result for every fault.
