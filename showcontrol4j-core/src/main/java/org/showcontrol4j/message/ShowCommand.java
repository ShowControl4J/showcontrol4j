package org.showcontrol4j.message;

/**
 * Serves as a helper class to create standard {@link SCFJMessage} objects.
 *
 * @author James Hare
 */
public class ShowCommand {

    // private constructor so that the class cannot be instantiated.
    private ShowCommand() {
    }

    /**
     * Creates a {@link SCFJMessage} object with the GO show command. Accepts a parameter to specify a time
     * in milliseconds to wait before the show loop is started. This allows for better show syncing if a
     * co-element experiences network lag when accepting messages. All show elements will wait for the time
     * specified before starting allowing the lag to catch up.
     *
     * @param syncTimeout milliseconds to wait before starting show loop.
     * @return a {@link SCFJMessage} with the GO show command.
     */
    public static SCFJMessage GO(final Long syncTimeout) {
        return SCFJMessage.builder()
                .instruction(Instruction.GO)
                .startTime(System.currentTimeMillis() + (syncTimeout != null ? syncTimeout : 0L))
                .build();
    }

    /**
     * Creates a {@link SCFJMessage} object with the IDLE show command. Accepts a parameter to specify a time
     * in milliseconds to wait before the show loop is started. This allows for better show syncing if a
     * co-element experiences network lag when accepting messages. All show elements will wait for the time
     * specified before starting allowing the lag to catch up.
     *
     * @param syncTimeout milliseconds to wait before starting show loop.
     * @return a {@link SCFJMessage} String with the IDLE show command.
     */
    public static SCFJMessage IDLE(final Long syncTimeout) {
        return SCFJMessage.builder()
                .instruction(Instruction.IDLE)
                .startTime(System.currentTimeMillis() + (syncTimeout != null ? syncTimeout : 0L))
                .build();
    }

    /**
     * Creates a {@link SCFJMessage} object with the SHUTDOWN show command.
     *
     * @return a {@link SCFJMessage} object with the SHUTDOWN show command.
     */
    public static SCFJMessage SHUTDOWN() {
        return SCFJMessage.builder()
                .instruction(Instruction.SHUTDOWN)
                .startTime(System.currentTimeMillis())
                .build();
    }

}
