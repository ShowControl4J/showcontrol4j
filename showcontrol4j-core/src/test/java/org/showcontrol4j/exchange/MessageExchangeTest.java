package org.showcontrol4j.exchange;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the {@link MessageExchange} class.
 *
 * @author James Hare
 */
public class MessageExchangeTest {

    @Test
    public void testBuilder() {
        final String name = "test_name";
        final MessageExchange messageExchange = MessageExchange.builder().name(name).build();
        assertEquals(name, messageExchange.getName());
    }

    @Test
    public void testToString() {
        final MessageExchange messageExchange = MessageExchange.builder().name("test_name").build();
        assertEquals("MessageExchange(name=test_name)", messageExchange.toString());
    }

}
