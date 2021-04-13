package org.showcontrol4j.exchange;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for the {@link MessageExchange} class.
 *
 * @author James Hare
 */
public class MessageExchangeTest {

  @Test
  public void testBuilder() {
    String name = "test_namme";
    MessageExchange messageExchange = MessageExchange.builder().name(name).build();
    assertEquals(name, messageExchange.getName());
  }

}
