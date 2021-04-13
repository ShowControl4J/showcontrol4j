package org.showcontrol4j.message;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Tests for the {@link SCFJMessage} class.
 *
 * @author James Hare
 */
public class SCFJMessageTest {

  @Test
  public void testBuilder() {
    final long testTime = 1234567891234L;
    SCFJMessage scfjMessage = SCFJMessage.builder().instruction(Instruction.GO).startTime(testTime).build();
    assertThat(scfjMessage, instanceOf(SCFJMessage.class));
    assertEquals(Instruction.GO, scfjMessage.getInstruction());
    assertEquals(testTime, scfjMessage.getStartTime());
  }

  @Test
  public void testConstructor_nullStartTime() {
    SCFJMessage scfjMessage = SCFJMessage.builder().build();
    assertThat(scfjMessage.getStartTime(), instanceOf(Long.class));
    assertEquals(0L, scfjMessage.getStartTime());
  }

  @Test
  public void testGetInstruction() {
    SCFJMessage scfjMessage = SCFJMessage.builder().instruction(Instruction.GO).build();
    assertEquals(Instruction.GO, scfjMessage.getInstruction());
  }

}