package io.zartra.bedwars.paper.replay.staff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Sanitized immutable outcome for replay staff operations. */
public final class ReplayStaffResult {
    /** Stable staff result classification. */
    public enum Status {
        SUCCESS, FORBIDDEN, NOT_FOUND, INVALID_STATE, FAILED
    }

    private final Status status;
    private final List<ReplayStaffRecord> records;
    private final ReplayStaffRecord record;

    private ReplayStaffResult(final Status status, final List<ReplayStaffRecord> records,
                              final ReplayStaffRecord record) {
        this.status = Objects.requireNonNull(status, "status");
        this.records = Collections.unmodifiableList(new ArrayList<ReplayStaffRecord>(
                Objects.requireNonNull(records, "records")));
        if (this.records.contains(null)) {
            throw new IllegalArgumentException("records cannot contain null");
        }
        this.record = record;
    }

    /** Creates a status-only outcome. */
    public static ReplayStaffResult of(final Status status) {
        return new ReplayStaffResult(status, Collections.emptyList(), null);
    }
    /** Creates a successful search outcome. */
    public static ReplayStaffResult search(final List<ReplayStaffRecord> records) {
        return new ReplayStaffResult(Status.SUCCESS, records, null);
    }
    /** Creates a successful single-record outcome. */
    public static ReplayStaffResult record(final ReplayStaffRecord record) {
        return new ReplayStaffResult(Status.SUCCESS, Collections.emptyList(),
                Objects.requireNonNull(record, "record"));
    }

    /** @return outcome status */ public Status status() { return status; }
    /** @return immutable search rows */ public List<ReplayStaffRecord> records() {
        return records;
    }
    /** @return inspected record when present */ public Optional<ReplayStaffRecord> record() {
        return Optional.ofNullable(record);
    }
}
