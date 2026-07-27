package io.zartra.bedwars.paper.replay.staff;

import java.util.concurrent.CompletionStage;

/** Non-blocking authoritative sink for replay staff audit records. */
public interface ReplayStaffAuditSink {
    /** Appends one immutable record in sequence order. */
    CompletionStage<Void> append(ReplayStaffAuditRecord record);
}
