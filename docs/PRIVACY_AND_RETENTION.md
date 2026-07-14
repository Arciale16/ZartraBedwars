# Privacy, Replay and Retention Policy

**Status:** Accepted product baseline; deployment owner must publish jurisdiction-specific notice
**Decisions:** RC-040, RC-041, RC-065
**Requirements:** `ZBW-READY-010`, `ZBW-READY-011`, `ZBW-READY-018`

This is privacy-by-default engineering policy, not legal advice. No deployment may enable a more invasive profile without documented lawful basis, age policy, operator identity and jurisdictional review.

An authorized **legal hold** is the only policy that may delay destruction of its specifically scoped evidence; it never preserves unrelated data.

## Collection profiles

| Data class | Default collection | Content excluded by default | Purpose | Default visibility |
|---|---|---|---|---|
| Live match state | Player UUID, display-name snapshot, team/actions, blocks, combat, items, positions and server timestamps needed to run/recover the match | IP, Discord identity, private messages | Gameplay and crash recovery | Active participants and authorized operations |
| Ordinary replay | Pseudonymous participant IDs, gameplay events and sampled movement required for playback | Global/party/team chat content, voice, IP, device fingerprint, Discord messages | Participant playback and quality/debug | Participants; operator may disable participant access |
| Reported replay | Ordinary replay plus report reason, reporter ID and bounded annotations | Unrelated account/link data and all chat unless explicitly approved for the case type | Moderation evidence | Assigned staff only; participant export subject to third-party redaction |
| Anticheat evidence | Normalized alert code, confidence, timestamps and approved telemetry fields | Raw proprietary provider internals and unrelated behavior | Investigate suspected cheating | Authorized anticheat/moderation staff |
| Atlas case | Anonymized case projection, review interactions and verdict; real identity in separate restricted record | Reviewer-to-subject identity, network identifiers | Community review with staff decision | Reserved reviewer sees anonymized case; staff sees identity only when required |
| Profile/statistics | UUID identity, chosen public display snapshot, gameplay aggregates, progression, unlocks and privacy choices | IP, private provider data | Player service and leaderboards | Owner only except explicitly public aggregate leaderboard/display fields |
| Audit/security | Actor/subject stable IDs, action, reason, result, timestamp, correlation ID and redacted network/security metadata | Secrets, tokens, replay content, raw command arguments containing secrets | Accountability and incident response | Security/audit staff only |

Chat capture is `OFF` in default configuration and in every shipped profile. Enabling any chat-content capture requires a separate config acknowledgement containing policy version, lawful-purpose code, allowed channel(s), maximum retention, access role and notice URL. Direct/private messages remain unsupported for replay capture.

## Retention schedule

| Class | Retention trigger and duration | Quota response | Deletion result |
|---|---|---|---|
| Ordinary replay payload + metadata | 30 days from match end | Evict oldest ordinary payload first; retain minimal aggregate stats | Manifest/payload deleted; non-personal aggregate stats remain |
| Reported replay/evidence | 90 days from report closure, or until hold release if later | Never silently evict; alarm and pause new non-evidence replay capture | Delete or pseudonymize according to active case/hold |
| Anticheat evidence | 180 days from case closure | Reserved capacity; alarm/escalate | Delete normalized evidence unless held |
| Atlas case and reviewer interaction | 365 days from final closure/appeal | Archive encrypted; no public history | Delete case payload and identity links; retain anonymous aggregate quality metrics |
| Audit/security logs | 365 days from event | Archive encrypted and access-restricted | Delete event detail; retain non-identifying counts |
| Account link/consent | Active link plus 30 days after unlink | Reject duplicate; no content cache | Delete token/link; retain minimal anti-fraud revocation hash for 90 days |
| Player operational profile | Account lifetime; delete within 30 days of verified request unless held/contractually required | N/A | Delete or anonymize personal identity; preserve non-identifying aggregate integrity |
| Legal/administrative hold | Until written authorized release; reviewed every 90 days | Separate protected quota; alert owner | Evidence retained but identity-separated/pseudonymized where possible |
| Encrypted backups | 35 daily + 12 monthly copies; held-case index follows hold | Stop new non-critical backups before overwriting required hold copies | Expired backup destroyed by cryptographic erasure/verified deletion |

Expiry evaluation runs at least daily. All durations are UTC and start at the event specified, not last access. Operators may configure shorter ordinary periods; longer periods require a documented policy version and owner/legal approval.

## Access and visibility defaults

- Profiles, detailed histories, quest progress, cosmetics ownership and private-game history are owner-only. The player may separately expose a display profile; consent is revocable without removing leaderboard integrity.
- Leaderboards show configured display name, rank and aggregate value only. Hidden players use an anonymized label where competition integrity requires retaining the row.
- Replays are not public-by-ID. Participants receive only their eligible matches; staff requires `zartra.staff.replay.view` plus purpose/reason. Evidence download requires a separate export permission.
- Atlas reviewers receive anonymous case aliases and only evidence needed for the question. Staff identity reveal and case-history access are separate permissions and audited.
- Administrative GUIs never expose IP, secret, raw provider tokens or legal-hold reason to general staff.

## Rights workflow

1. Authenticate requester and create an immutable request ID; never accept identity proof inside ordinary chat/logs.
2. Export within 30 days in documented JSON plus human-readable index, with third-party data redacted and checksums recorded.
3. Delete ordinary personal data within 30 days. Propagate tombstones through cache, Redis, replicas, replay indexes and Discord/account links.
4. If an active hold applies, delete unrelated data, separate real identity from evidence, replace operational IDs with case-scoped pseudonyms and provide the requester a lawful-hold response without exposing investigation details.
5. Release of the hold triggers deletion within seven days unless another valid retention class applies.
6. Audit requester, approver, scope, records affected, exceptions and completion; audit text contains no exported content.

## Security and acceptance

Evidence payloads and backups use authenticated encryption at rest; transport uses TLS; keys come from the secrets provider and rotate at least every 180 days. Export is encrypted or delivered through a short-lived single-use channel. Every sensitive read/export/delete/hold/release is least-privilege, rate-limited and audited.

Tests must prove: default replay contains no chat strings; unauthorized roles cannot infer existence; case identity separation; retention boundary timestamps; hold wins over deletion without preserving unrelated data; hold release deletion; backup expiry; export completeness/redaction; cache/tombstone propagation; secret/log redaction; and visibility defaults for every GUI, command, API and placeholder.
