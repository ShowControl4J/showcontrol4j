package org.showcontrol4j.timeline;

import lombok.Getter;

/**
 * A single entry in a {@link Timeline}: an {@link TimelineAction} and the offset, in milliseconds
 * after the timeline starts, at which it should run.
 *
 * @author James Hare
 */
@Getter
final class TimelineEvent {

    private final long offsetMillis;
    private final TimelineAction action;

    TimelineEvent(final long offsetMillis, final TimelineAction action) {
        this.offsetMillis = offsetMillis;
        this.action = action;
    }

}
