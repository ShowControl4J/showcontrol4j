package org.showcontrol4j.timeline;

/**
 * A single step run by a {@link Timeline} - typically a hardware call on a Show Element
 * (turn a pin on, move a servo, trigger a DMX cue, etc).
 *
 * @author James Hare
 */
@FunctionalInterface
public interface TimelineAction {

    /**
     * Runs this action.
     *
     * @throws InterruptedException if the calling thread is interrupted while this action is running.
     */
    void run() throws InterruptedException;

}
