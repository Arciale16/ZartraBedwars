package io.zartra.bedwars.replay.playback;

import io.zartra.bedwars.replay.api.ReplayEvent;
import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.replay.api.ReplayState;
import io.zartra.bedwars.replay.api.ReplayTimeline;
import java.util.List;
import java.util.Objects;

/** Deterministic, immutable and platform-neutral playback engine (ZBW-REPLAY-003/004/005/009). */
public final class ReplayPlaybackEngine {
    private static final String CORRUPTED_EVENT = "corrupted-event";
    private final ReplayEventApplier eventApplier;

    /** Creates an engine with a version-neutral attribute state reducer. */
    public ReplayPlaybackEngine() {
        this(new AttributeReplayEventApplier());
    }

    /** Creates an engine with an application-specific pure event applier. */
    public ReplayPlaybackEngine(final ReplayEventApplier eventApplier) {
        this.eventApplier = Objects.requireNonNull(eventApplier, "eventApplier");
    }

    /** Loads one completed or archived recording at the start position. */
    public PlaybackSession load(final ReplaySession replay) {
        Objects.requireNonNull(replay, "replay");
        final PlaybackSession loading = PlaybackSession.loading(replay.metadata().replayId());
        if (replay.state() != ReplayState.COMPLETED && replay.state() != ReplayState.ARCHIVED) {
            return failed(loading, replay.timeline(), ReplaySnapshot.empty(), "replay-not-playable");
        }
        return loading.with(PlaybackState.READY, replay.timeline(), ReplaySnapshot.empty(),
                PlaybackSpeed.NORMAL, null);
    }

    /** Starts or resumes forward playback. */
    public PlaybackSession play(final PlaybackSession session) {
        requireState(session, PlaybackState.READY, PlaybackState.PAUSED);
        if (session.cursor().nextEventIndex() >= session.timeline().events().size()) {
            return session.with(PlaybackState.COMPLETED, session.timeline(), session.snapshot(),
                    session.speed(), null);
        }
        return session.with(PlaybackState.PLAYING, session.timeline(), session.snapshot(),
                session.speed(), null);
    }

    /** Pauses active playback without moving its cursor. */
    public PlaybackSession pause(final PlaybackSession session) {
        requireState(session, PlaybackState.PLAYING);
        return session.with(PlaybackState.PAUSED, session.timeline(), session.snapshot(),
                session.speed(), null);
    }

    /** Changes speed without changing timeline position. */
    public PlaybackSession changeSpeed(final PlaybackSession session, final PlaybackSpeed speed) {
        requireUsable(session);
        return session.with(session.state(), session.timeline(), session.snapshot(),
                Objects.requireNonNull(speed, "speed"), null);
    }

    /** Applies exactly one next ordered event while playing. */
    public PlaybackSession advance(final PlaybackSession session) {
        requireState(session, PlaybackState.PLAYING);
        final int nextIndex = session.cursor().nextEventIndex();
        final List<ReplayEvent> events = session.timeline().events();
        if (nextIndex >= events.size()) {
            return session.with(PlaybackState.COMPLETED, session.timeline(), session.snapshot(),
                    session.speed(), null);
        }
        try {
            final ReplaySnapshot snapshot = eventApplier.apply(session.snapshot(), events.get(nextIndex));
            final PlaybackState state = nextIndex + 1 == events.size()
                    ? PlaybackState.COMPLETED : PlaybackState.PLAYING;
            return session.with(state, session.timeline(), snapshot, session.speed(), null);
        } catch (RuntimeException failure) {
            return failed(session, session.timeline(), session.snapshot(), CORRUPTED_EVENT);
        }
    }

    /** Rebuilds deterministic state through the inclusive event index; -1 seeks to start. */
    public PlaybackSession seekToEvent(final PlaybackSession session, final int eventIndex) {
        requireSeekable(session);
        if (eventIndex < -1 || eventIndex >= session.timeline().events().size()) {
            throw new IllegalArgumentException("event index is outside timeline");
        }
        return rebuild(session, eventIndex);
    }

    /** Rebuilds through the last event at or before the supplied replay-relative timestamp. */
    public PlaybackSession seekToTimestamp(final PlaybackSession session, final long offsetMillis) {
        requireSeekable(session);
        if (offsetMillis < 0L) {
            throw new IllegalArgumentException("timestamp must be non-negative");
        }
        int target = -1;
        final List<ReplayEvent> events = session.timeline().events();
        for (int index = 0; index < events.size(); index++) {
            if (events.get(index).offsetMillis() > offsetMillis) {
                break;
            }
            target = index;
        }
        return rebuild(session, target);
    }

    /** Restores a previously produced snapshot after verifying it by deterministic rebuild. */
    public PlaybackSession restoreSnapshot(final PlaybackSession session,
                                           final ReplaySnapshot snapshot) {
        requireSeekable(session);
        Objects.requireNonNull(snapshot, "snapshot");
        final int target = snapshot.cursor().position().eventIndex();
        if (target < -1 || target >= session.timeline().events().size()) {
            throw new IllegalArgumentException("snapshot cursor is outside timeline");
        }
        final PlaybackSession rebuilt = rebuild(session, target);
        if (rebuilt.state() == PlaybackState.FAILED) {
            return rebuilt;
        }
        if (!rebuilt.snapshot().equals(snapshot)) {
            throw new IllegalArgumentException("snapshot does not match deterministic timeline state");
        }
        return session.with(PlaybackState.PAUSED, session.timeline(), snapshot,
                session.speed(), null);
    }

    private PlaybackSession rebuild(final PlaybackSession session, final int eventIndex) {
        final PlaybackSession seeking = session.with(PlaybackState.SEEKING, session.timeline(),
                session.snapshot(), session.speed(), null);
        ReplaySnapshot snapshot = ReplaySnapshot.empty();
        try {
            for (int index = 0; index <= eventIndex; index++) {
                snapshot = eventApplier.apply(snapshot, session.timeline().events().get(index));
            }
            return seeking.with(PlaybackState.PAUSED, session.timeline(), snapshot,
                    session.speed(), null);
        } catch (RuntimeException failure) {
            return failed(seeking, session.timeline(), snapshot, CORRUPTED_EVENT);
        }
    }

    private static PlaybackSession failed(final PlaybackSession session,
                                          final ReplayTimeline timeline,
                                          final ReplaySnapshot snapshot,
                                          final String reason) {
        return session.with(PlaybackState.FAILED, timeline, snapshot, session.speed(), reason);
    }

    private static void requireUsable(final PlaybackSession session) {
        Objects.requireNonNull(session, "session");
        if (session.state() == PlaybackState.CREATED || session.state() == PlaybackState.LOADING
                || session.state() == PlaybackState.FAILED) {
            throw new IllegalStateException("playback is not ready");
        }
    }

    private static void requireSeekable(final PlaybackSession session) {
        requireUsable(session);
        if (session.state() == PlaybackState.SEEKING) {
            throw new IllegalStateException("playback is already seeking");
        }
    }

    private static void requireState(final PlaybackSession session,
                                     final PlaybackState first,
                                     final PlaybackState... additional) {
        Objects.requireNonNull(session, "session");
        if (session.state() == first) {
            return;
        }
        for (PlaybackState candidate : additional) {
            if (session.state() == candidate) {
                return;
            }
        }
        throw new IllegalStateException("invalid playback state " + session.state());
    }
}
