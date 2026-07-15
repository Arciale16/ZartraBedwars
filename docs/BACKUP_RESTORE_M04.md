# M04 backup and restore runbook

## Safety contract

SQL business state targets zero loss of acknowledged transactions and service restoration within 15 minutes (`ZBW-READY-016`). Backups must use authenticated encryption, a secret-provider key, separate backup credentials and off-host storage. A provider is accepted only when it returns a SHA-256, encryption confirmation and independent validation confirmation; paths and credentials never enter public evidence.

`SqlRecoveryCoordinator` supplies the invariant enforcement. Database-specific mechanics are implemented through its constructor-injected `BackupDriver`: `createEncrypted`, read-only `validate` and `restoreQuiescent`. This is an operational provider boundary, not permission to acknowledge a backup before the provider has finished and verified it.

## Backup procedure

1. Allocate a typed backup ID and record the requested UTC instant.
2. For a migration/restore drill, quiesce writes and verify no active Hikari transaction remains. Ordinary scheduled backups may use engine-native consistent snapshots/binlogs.
3. Use least-privilege backup credentials and a key lease from the M03 secrets boundary.
4. Create an encrypted full/incremental artifact off-host; never serialize a password into configuration, evidence or logs.
5. Read/validate the completed artifact, calculate SHA-256 and verify the encryption envelope.
6. Return provider evidence and record it in the restricted backup history. Failed or incomplete evidence is a typed failure and is never advertised as a backup.
7. Enforce 35 daily plus 12 monthly copies and legal-hold indexing before deletion.

## Restore procedure

1. Authorize the operation with separate restore permission and credentials.
2. Stop admissions and quiesce all writes; retain the old database until reconciliation passes.
3. Validate checksum, encryption, retention/hold eligibility and key availability before mutation.
4. Restore into an isolated target, run schema checksum validation and integrity checks, then run application smoke/reconciliation tests.
5. Compare outbox/inbox IDs and committed business records to prevent duplicate outcomes.
6. Promote atomically, reopen bounded traffic and record measured RPO/RTO, versions and checksums.
7. On failure keep writes paused, retain both targets and escalate; never continue on an unvalidated partial restore.

## Drills and evidence

Automated validation is monthly; isolated SQL restore is quarterly; full disaster recovery is annual. Evidence records scenario, immutable artifact/config/schema checksums, start/end, measured RPO/RTO, lost/reconciled operations, security observations, owner and corrective action. Release/expansion is blocked after a failed drill until correction and rerun.

M04 unit tests prove coordinator rejection of unencrypted, unvalidated and provider-failed evidence. A real off-host provider and measured drill remain deployment/release evidence rather than repository secrets.
