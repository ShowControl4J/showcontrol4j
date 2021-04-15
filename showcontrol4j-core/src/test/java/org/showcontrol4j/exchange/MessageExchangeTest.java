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
    String name = "test_name";
    MessageExchange messageExchange = MessageExchange.builder().name(name).build();
    assertEquals(name, messageExchange.getName());
  }

}
