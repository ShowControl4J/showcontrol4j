package org.showcontrol4j.element;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.exchange.MessageExchange;
import org.showcontrol4j.message.Instruction;
import org.showcontrol4j.message.SCFJMessage;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Serves as a standard implementation of the {@link ShowElement} interface. It is recommended that
 * all show elements extend the ShowElementBase class and implement data members and function members
 * to interact with that particular show element. The loop and idle methods should remain abstract so
 * that they can be implemented at the time of instantiation.
 *
 * @author James Hare
 */
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Slf4j
public abstract class ShowElement {

    @ToString.Include
    protected final String name;
    @ToString.Include
    protected final Long id;
    private final MessageExchange messageExchange;
    private final BrokerConnectionFactory brokerConnectionFactory;
    private final ExecutorService executor;
    private Future runningFuture;

    public ShowElement(final String name, final Long id, final MessageExchange messageExchange,
                       final BrokerConnectionFactory brokerConnectionFactory) {
        this.name = name;
        this.id = id;
        this.messageExchange = messageExchange;
        this.brokerConnectionFactory = brokerConnectionFactory;
        executor = Executors.newFixedThreadPool(5);
        try {
            registerShowElement();
        } catch (final IOException | TimeoutException e) {
            log.error("An error occurred while registering the show element={}. {}", toString(), e.getStackTrace());
        }
    }

    private void registerShowElement() throws IOException, TimeoutException {
        final Channel channel = brokerConnectionFactory.newConnection().createChannel();
        channel.exchangeDeclare(messageExchange.getName(), "fanout");
        final String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, messageExchange.getName(), "");

        final DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            final SCFJMessage message = SCFJMessage.deserialize(delivery.getBody());
            log.trace("The following message has been received=" + message.toString());
            handleMessage(message);
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });
    }

    protected void handleMessage(final SCFJMessage message) {
        while (message.getStartTime() > System.currentTimeMillis()) {
            try {
                pause();
            } catch (final InterruptedException e) {
                log.error("Sleeping was interrupted while handling message on Show Element={}. {}", toString(), e.getStackTrace());
            }
        }
        if (runningFuture != null) {
            runningFuture.cancel(true);
        }
        runningFuture = executor.submit(new MessageTask(message));
    }

    private void analyzeMessage(final SCFJMessage message) {
        if (message.getInstruction() == Instruction.STOP) {
            runStop();
        } else if (message.getInstruction() == Instruction.GO) {
            runShowLoop();
        } else if (message.getInstruction() == Instruction.IDLE) {
            runIdleLoop();
        }
    }

    private void runShowLoop() {
        try {
            showSequence();
            runIdleLoop();
        } catch (final InterruptedException e) {
            log.trace("Thread is complete because a new Show Command was received for Show Element={}", toString());
        }
    }

    private void runIdleLoop() {
        try {
            while (true) {
                idleLoop();
            }
        } catch (final InterruptedException e) {
            log.trace("Thread is complete because a new Show Command was received for Show Element={}", toString());
        }
    }

    private void runStop() {
        runningFuture = null;
        executor.shutdownNow();
        shutdownProcedure();
    }

    /**
     * Pauses a thread for a tenth of a second.
     *
     * @throws InterruptedException
     */
    protected final void pause() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
    }

    private class MessageTask implements Runnable {

        private final SCFJMessage message;

        public MessageTask(final SCFJMessage message) {
            this.message = message;
        }

        @Override
        public void run() {
            analyzeMessage(message);
        }
    }

    /**
     * The abstract show element loop method. Must remain abstract and be implemented at the time
     * of instantiation.
     */
    protected abstract void showSequence() throws InterruptedException;

    /**
     * The abstract show element idle method. Must remain abstract and be implemented at the time
     * of instantiation.
     */
    protected abstract void idleLoop() throws InterruptedException;

    /**
     * The abstract show element shutdown method. Must be implemented by child classes before the
     * time of instantiation.
     */
    protected abstract void shutdownProcedure();

}
