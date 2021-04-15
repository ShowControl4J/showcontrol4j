package org.showcontrol4j.trigger;

import com.rabbitmq.client.Channel;
import lombok.Getter;
import lombok.Setter;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.exchange.MessageExchange;
import org.showcontrol4j.message.ShowCommand;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Serves as the parent class for a ShowTrigger. The ShowTrigger must define a triggerElements method
 * at the time of instantiation.
 *
 * @author James Hare
 */
@Getter
@Setter
public abstract class ShowTrigger {

    protected final String name;
    protected final Long id;
    private final MessageExchange messageExchange;
    private final BrokerConnectionFactory brokerConnectionFactory;
    private final Long syncTimeout;
    private Channel channel;

    public ShowTrigger(final String showTriggerName, final Long showTriggerId, final Long syncTimeout,
                       final MessageExchange messageExchange, final BrokerConnectionFactory brokerConnectionFactory) {
        this.name = showTriggerName;
        this.id = showTriggerId;
        this.messageExchange = messageExchange;
        this.brokerConnectionFactory = brokerConnectionFactory;
        this.syncTimeout = syncTimeout;
        try {
            registerShowTrigger();
        } catch (final IOException | TimeoutException e) {
            System.out.println("An error occurred while registering the show element. " + e.getCause());
        }
    }

    private void registerShowTrigger() throws IOException, TimeoutException {
        channel = brokerConnectionFactory.newConnection().createChannel();
        channel.exchangeDeclare(messageExchange.getName(), "fanout");
    }

    /**
     * A method to setup a listener for the show trigger action. Must be implemented by child classes.
     */
    protected abstract void startListener();

    protected void sendGoMessage() throws IOException {
        channel.basicPublish(messageExchange.getName(), "", null,
                ShowCommand.GO(syncTimeout != null ? syncTimeout : 0L).serialize());
    }

    protected void sendIdleMessage() throws IOException {
        channel.basicPublish(messageExchange.getName(), "", null,
                ShowCommand.IDLE(syncTimeout != null ? syncTimeout : 0L).serialize());
    }

    protected void sendStopMessage() throws IOException {
        channel.basicPublish(messageExchange.getName(), "", null, ShowCommand.STOP().serialize());
    }
}