package org.showcontrol4j.trigger;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.exceptions.ConnectionFailedException;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.exchange.MessageExchange;
import org.showcontrol4j.message.SCFJMessage;
import org.showcontrol4j.message.ShowCommand;

import java.io.IOException;

/**
 * Serves as the parent class for all Show Triggers. When creating a child class, the {@link ShowTrigger#startListener()}
 * method must be implemented to describe how the trigger will send messages to the message broker to start/ shutdown a Show
 * Element.
 *
 * @author James Hare
 */
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public abstract class ShowTrigger {

    @ToString.Include
    protected final String name;
    @ToString.Include
    protected final Long id;
    private final MessageExchange messageExchange;
    private final BrokerConnectionFactory brokerConnectionFactory;
    @ToString.Include
    private final Long syncTimeout;
    private Mqtt3BlockingClient client;

    public ShowTrigger(final String showTriggerName, final Long showTriggerId, final Long syncTimeout,
                       final MessageExchange messageExchange, final BrokerConnectionFactory brokerConnectionFactory) {
        this.name = showTriggerName;
        this.id = showTriggerId;
        this.messageExchange = messageExchange;
        this.brokerConnectionFactory = brokerConnectionFactory;
        this.syncTimeout = syncTimeout;
        try {
            registerShowTrigger();
        } catch (final ConnectionFailedException e) {
            System.out.println("An error occurred while registering the show element. " + e.getMessage());
        }
    }

    private void registerShowTrigger() {
        client = brokerConnectionFactory.newConnection(name + "-" + id).toBlocking();
        client.connect();
    }

    /**
     * A method to setup a listener for the show trigger action. Must be implemented by child classes.
     */
    protected abstract void startListener();

    protected void sendGoMessage() throws IOException {
        publish(ShowCommand.GO(syncTimeout != null ? syncTimeout : 0L));
    }

    protected void sendIdleMessage() throws IOException {
        publish(ShowCommand.IDLE(syncTimeout != null ? syncTimeout : 0L));
    }

    protected void sendShutdownMessage() throws IOException {
        publish(ShowCommand.SHUTDOWN());
    }

    private void publish(final SCFJMessage message) throws IOException {
        client.publishWith()
                .topic(messageExchange.getName())
                .payload(message.serialize())
                .qos(MqttQos.AT_LEAST_ONCE)
                .send();
    }

    /**
     * Exits the JVM. A JDK 24+ SecurityManager can no longer intercept {@link System#exit(int)}
     * (JEP 486), so this exists as a testable seam - override it in a test subclass to verify
     * exit behavior without actually terminating the test JVM.
     *
     * @param status the exit status to pass to {@link System#exit(int)}.
     */
    protected void exitJvm(final int status) {
        System.exit(status);
    }
}
