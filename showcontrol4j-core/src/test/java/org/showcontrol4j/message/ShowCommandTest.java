package org.showcontrol4j.message;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the {@link ShowCommand} class.
 *
 * @author James Hare
 */
public class ShowCommandTest {

    @Test
    public void testGo() {
        final long currentTime = System.currentTimeMillis();
        final SCFJMessage testCommand = ShowCommand.GO(10000L);
        assertEquals(Instruction.GO, testCommand.getInstruction());
        assertTrue(testCommand.getStartTime() > (currentTime + 10000) - 100);
        assertTrue(testCommand.getStartTime() < (currentTime + 10000) + 100);
    }

    @Test
    public void testGo_nullSyncTimeout() {
        final long currentTime = System.currentTimeMillis();
        final SCFJMessage testCommand = ShowCommand.GO(null);
        assertEquals(Instruction.GO, testCommand.getInstruction());
        assertTrue(testCommand.getStartTime() > currentTime - 100);
        assertTrue(testCommand.getStartTime() < currentTime + 100);
    }

    @Test
    public void testIdle() {
        final long currentTime = System.currentTimeMillis();
        final SCFJMessage testCommand = ShowCommand.IDLE(10000L);
        assertEquals(Instruction.IDLE, testCommand.getInstruction());
        assertTrue(testCommand.getStartTime() > (currentTime + 10000) - 100);
        assertTrue(testCommand.getStartTime() < (currentTime + 10000) + 100);
    }

    @Test
    public void testIdle_nullSyncTimeout() {
        final long currentTime = System.currentTimeMillis();
        final SCFJMessage testCommand = ShowCommand.IDLE(null);
        assertEquals(Instruction.IDLE, testCommand.getInstruction());
        assertTrue(testCommand.getStartTime() > currentTime - 100);
        assertTrue(testCommand.getStartTime() < currentTime + 100);
    }

    @Test
    public void testShutdown() {
        final long currentTime = System.currentTimeMillis();
        final SCFJMessage testCommand = ShowCommand.SHUTDOWN();
        assertEquals(Instruction.SHUTDOWN, testCommand.getInstruction());
        assertTrue(testCommand.getStartTime() > currentTime - 100);
        assertTrue(testCommand.getStartTime() < currentTime + 100);
    }

}