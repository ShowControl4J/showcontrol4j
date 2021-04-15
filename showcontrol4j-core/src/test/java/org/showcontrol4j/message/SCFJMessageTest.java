package org.showcontrol4j.message;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

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
  public void testSerialization() throws IOException {
    final long testTime = 1234567891234L;
    SCFJMessage scfjMessage = SCFJMessage.builder().instruction(Instruction.GO).startTime(testTime).build();
    final byte[] serialized = scfjMessage.serialize();
    final SCFJMessage deserialized = SCFJMessage.deserialize(serialized);

    assertThat(deserialized, instanceOf(SCFJMessage.class));
    assertEquals(Instruction.GO, deserialized.getInstruction());
    assertEquals(testTime, deserialized.getStartTime());
  }

}