package org.showcontrol4j.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.Serializable;

/**
 * Serves as a class for a SCFJMessage POJO.
 *
 * @author James Hare
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SCFJMessage implements Serializable {

  private Instruction instruction;
  private long startTime;
  /**
   * Identifies which cue a GO instruction should run. Null means "the default cue" - a Show
   * Element that never registers named cues just runs its single {@code showSequence()}, so
   * this is optional and backward compatible with a single-sequence show.
   */
  private String cueId;

  public byte[] serialize() throws JsonProcessingException {
    return new ObjectMapper().writeValueAsBytes(this);
  }

  public static SCFJMessage deserialize(final byte[] input) throws IOException {
    return new ObjectMapper().readValue(input, SCFJMessage.class);
  }

}
