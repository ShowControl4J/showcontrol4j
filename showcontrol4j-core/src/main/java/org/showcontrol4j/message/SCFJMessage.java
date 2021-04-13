package org.showcontrol4j.message;

import lombok.Builder;
import lombok.Data;

/**
 * Serves as a class for a SCFJMessage POJO.
 *
 * @author James Hare
 */
@Builder
@Data
public class SCFJMessage {

  private final Instruction instruction;
  private final long startTime;

}
