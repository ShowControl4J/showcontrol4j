package org.showcontrol4j.trigger.keyboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.exchange.MessageExchange;
import org.showcontrol4j.trigger.ShowTrigger;

import java.io.IOException;
import java.util.Scanner;

/**
 * Serves as a Show Trigger for a computer keyboard.
 *
 * @author James Hare
 */
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Slf4j
public class KeyboardShowTrigger extends ShowTrigger {

    @ToString.Include
    private final String triggerKey;
    private Scanner scanner;

    public KeyboardShowTrigger(final String triggerKey, final String showTriggerName, final Long showTriggerId,
                               final Long syncTimeout, final MessageExchange messageExchange, final BrokerConnectionFactory brokerConnectionFactory) {
        super(showTriggerName, showTriggerId, syncTimeout, messageExchange, brokerConnectionFactory);
        this.triggerKey = triggerKey;
        scanner = new Scanner(System.in);
    }

    @Override
    public void startListener() {
        while (true) {
            try {
                final String entry = scanner.next();
                if (entry.equalsIgnoreCase(triggerKey)) {
                    sendGoMessage();
                } else if (entry.equalsIgnoreCase("STOP")) {
                    sendStopMessage();
                } else if (entry.equalsIgnoreCase("IDLE")) {
                    sendIdleMessage();
                }
            } catch (final IOException e) {
                log.error("An exception occurred while running Keyboard Show Trigger {}: {}",
                        this.toString(), e);
                try {
                    sendStopMessage();
                } catch (final Exception ex) {
                    log.error("An exception occurred while attempting to stop Keyboard Show Trigger {}: {}",
                            this.toString(), ex);
                }
            }
        }
    }
}