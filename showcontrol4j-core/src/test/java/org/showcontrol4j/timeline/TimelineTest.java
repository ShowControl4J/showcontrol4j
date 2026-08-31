package org.showcontrol4j.timeline;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the {@link Timeline} class.
 *
 * @author James Hare
 */
public class TimelineTest {

    @Test
    public void testPlay_runsActionsInOffsetOrder() throws InterruptedException {
        final List<String> ranActions = new ArrayList<>();
        final Timeline timeline = Timeline.builder()
                .at(200L, () -> ranActions.add("second"))
                .at(0L, () -> ranActions.add("first"))
                .at(400L, () -> ranActions.add("third"))
                .build();

        timeline.play();

        assertEquals(List.of("first", "second", "third"), ranActions);
    }

    @Test
    public void testPlay_waitsForEachOffset() throws InterruptedException {
        final Timeline timeline = Timeline.builder()
                .at(300L, () -> { })
                .build();

        final long start = System.currentTimeMillis();
        timeline.play();
        final long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 300L);
        assertTrue(elapsed < 500L);
    }

    @Test
    public void testPlay_emptyTimelineReturnsImmediately() throws InterruptedException {
        final Timeline timeline = Timeline.builder().build();

        final long start = System.currentTimeMillis();
        timeline.play();
        final long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 100L);
    }

    @Test
    public void testPlay_isInterruptible() throws InterruptedException {
        final Timeline timeline = Timeline.builder()
                .at(5000L, () -> { })
                .build();

        final List<Boolean> caughtInterrupt = new CopyOnWriteArrayList<>();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Thread[] playingThread = new Thread[1];

        executor.submit(() -> {
            playingThread[0] = Thread.currentThread();
            try {
                timeline.play();
            } catch (final InterruptedException e) {
                caughtInterrupt.add(true);
            }
        });

        TimeUnit.MILLISECONDS.sleep(200);
        playingThread[0].interrupt();
        TimeUnit.MILLISECONDS.sleep(200);

        assertTrue(caughtInterrupt.contains(true));
        executor.shutdownNow();
    }

    @Test
    public void testPlay_replayable() throws InterruptedException {
        final List<String> ranActions = new ArrayList<>();
        final Timeline timeline = Timeline.builder()
                .at(0L, () -> ranActions.add("action"))
                .build();

        timeline.play();
        timeline.play();

        assertEquals(2, ranActions.size());
    }

}
