package org.showcontrol4j.timeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An ordered sequence of timed {@link TimelineAction}s that make up a single show cue - real show
 * control needs sequenced, timed cues rather than a single opaque {@code GO} broadcast. A Show
 * Element builds one Timeline per cue (typically in its constructor, via {@link #builder()}) and
 * registers it under a cue id; {@link #play()} then runs each action at its offset relative to
 * when the cue starts.
 *
 * <p>Instances are immutable and safe to replay across multiple {@link #play()} calls.</p>
 *
 * @author James Hare
 */
public final class Timeline {

    private final List<TimelineEvent> events;

    private Timeline(final List<TimelineEvent> events) {
        this.events = events;
    }

    /**
     * Creates a new {@link Builder} for constructing a {@link Timeline}.
     *
     * @return a new {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Runs every action in this timeline in order, waiting for each action's offset to elapse
     * (relative to when this method was called) before running it. Interruptible: if the calling
     * thread is interrupted while waiting or while an action is running, the {@link InterruptedException}
     * propagates immediately, consistent with how a Show Element cancels an in-progress cue when a new
     * command arrives.
     *
     * @throws InterruptedException if the calling thread is interrupted before this timeline finishes.
     */
    public void play() throws InterruptedException {
        final long start = System.currentTimeMillis();
        for (final TimelineEvent event : events) {
            final long wait = event.getOffsetMillis() - (System.currentTimeMillis() - start);
            if (wait > 0) {
                TimeUnit.MILLISECONDS.sleep(wait);
            }
            event.getAction().run();
        }
    }

    /**
     * Builder for a {@link Timeline}. Actions may be added in any order - they are sorted by offset
     * when {@link #build()} is called.
     */
    public static final class Builder {

        private final List<TimelineEvent> events = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds an action to run at the given offset after the timeline starts.
         *
         * @param offsetMillis milliseconds after the timeline starts to run {@code action}.
         * @param action the action to run.
         * @return this builder.
         */
        public Builder at(final long offsetMillis, final TimelineAction action) {
            events.add(new TimelineEvent(offsetMillis, action));
            return this;
        }

        /**
         * Builds the {@link Timeline}, sorting its actions by offset.
         *
         * @return the built {@link Timeline}.
         */
        public Timeline build() {
            final List<TimelineEvent> sorted = new ArrayList<>(events);
            sorted.sort(Comparator.comparingLong(TimelineEvent::getOffsetMillis));
            return new Timeline(Collections.unmodifiableList(sorted));
        }

    }

}
