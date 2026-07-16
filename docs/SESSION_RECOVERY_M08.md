# M08 session and crash recovery

Disconnect records the prior session status and a timestamp. Reconnect is accepted only
inside the configured grace interval and restores that exact status. Rehydration starts
from an immutable persisted snapshot; revision conflicts and overlapping writes are
typed retryable failures rather than last-write-wins mutation.

If persistence rejects or fails, `GameEngineService` restores the prior aggregate and
does not project the failed transition. A committed completion resumes restoration;
an uncommitted completion resumes the same idempotency fence. Reset cannot reopen
admission until every recorded session is restored.
