# M04 SQL query and index plan

## Hot-path plans

| Operation | Predicate/order | Required key/index | Bound |
|---|---|---|---:|
| Record lookup | `aggregate_type = ? AND aggregate_id = ?` | `zbw_records` primary key | 1 row |
| Optimistic update/delete | primary key plus `revision = ?` | primary key; revision checked on selected row | 1 row |
| Outbox claim | undelivered, available, lease absent/expired; order sequence/time/ID | `idx_zbw_outbox_claim(delivered_at, available_at, claimed_until, sequence_no)` | 1..1,000 |
| Outbox acknowledge | `operation_id = ? AND delivered_at IS NULL` | outbox primary key | 1 row |
| Inbox dedupe | insert `operation_id` | inbox primary key/unique | 1 row |
| Retention/tombstone | aggregate type plus ID | respective primary key | 1 row |
| Legal hold/release | `case_id = ?` | hold primary key | 1 row |
| Migration history | `version = ?` | history primary key | 1 row |

All user/data values are bound parameters. Batch/claim counts are validated before SQL. Statement timeouts derive from the positive transaction timeout; retry is limited to configured deadlock/serialization states and at most 16 attempts.

## Validation commands

SQLite contract setup executes:

```sql
EXPLAIN QUERY PLAN
SELECT operation_id, event_id, event_type, correlation_id, occurred_at,
       sequence_no, schema_version, thread_context, payload, available_at
FROM zbw_outbox
WHERE delivered_at IS NULL AND available_at <= ?
  AND (claimed_until IS NULL OR claimed_until < ?)
ORDER BY sequence_no, available_at, operation_id;
```

The contract accepts only a plan that uses the outbox claim index for filtering; a bounded temporary sort may remain because final ordering includes operation ID. MySQL/MariaDB CI must capture `EXPLAIN FORMAT=JSON` for the same query and reject a full-table access at the certified non-trivial fixture size. It must also prove primary-key access for record and idempotency paths.

## RC-077 evidence set

The workflow captures `EXPLAIN FORMAT=JSON` for seven representative paths on each engine: record lookup, outbox claim, inbox dedupe lookup, retention lookup, legal-hold lookup, tombstone lookup and migration-history lookup. The fixture contains 2,064 outbox rows plus representative records, inbox operations, retention rows, holds, tombstones and migration history. Certification rejects full-table access, a missing expected primary/unique/claim index or an absent plan.

Raw server JSON and a sanitized manifest are written below `target/m04-external/<database>/` and uploaded by the workflow. The first certified PR #5 run remains pending; RC-077 cannot close until both database artifacts pass review.

## Review thresholds

Regression review is required when examined rows exceed the requested batch by more than 10×, a primary-key query scans more than one row, an outbox plan drops `idx_zbw_outbox_claim`, or p95 query/pool wait exceeds the operational budget. Plans are re-captured after schema, server-version or cardinality changes. Raw player/case IDs and SQL parameters are never attached to plan telemetry.

Local SQLite plan and functional suites pass. MySQL/MariaDB plan evidence remains tied to the digest-gated workflow run because no Docker-compatible runtime exists on the current workstation.
