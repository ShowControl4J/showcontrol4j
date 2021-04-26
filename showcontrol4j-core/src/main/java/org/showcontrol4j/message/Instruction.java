package org.showcontrol4j.message;

/**
 * Instructions for a {@link SCFJMessage}.
 *
 * @author James Hare
 */
public enum Instruction {
    /**
     * Instruction to start the show sequence.
     */
    GO,
    /**
     * Instruction to start the idle loop.
     */
    IDLE,
    /**
     * Instruction to shutdown the show element.
     */
    SHUTDOWN
}
