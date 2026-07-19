# M12 Phase 1 API

All exported Phase 1 types are Java 8 neutral and live under `io.zartra.bedwars.progression`.

- `model`: typed identifiers, immutable progression/economy definitions and snapshots, audit and
  append-only registration/ledger values.
- `repository`: eight storage-neutral repository ports. Calls accept an active M04 `UnitOfWork`,
  return typed `Result` values and may block; callers must keep them off Minecraft owner threads.
- `projection`: immutable M08 input envelopes, checkpoint/result/recovery values, an idempotency
  port and the application projector contract.

Null inputs are rejected at construction. Numeric values are range checked; collections and byte
payloads are defensively copied. Repository implementations, SQL schemas, migrations, runtime
reward delivery and presentation APIs are intentionally absent from Phase 1.

M11 match resources and tenders are not persistent `CurrencyId` accounts. Future M12 transaction
services may consume the stable M11 tender SPI without moving match-resource ownership.
