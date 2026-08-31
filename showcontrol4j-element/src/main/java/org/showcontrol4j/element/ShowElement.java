package org.showcontrol4j.element;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.exceptions.ConnectionFailedException;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.exchange.MessageExchange;
import org.showcontrol4j.message.Instruction;
import org.showcontrol4j.message.SCFJMessage;
import org.showcontrol4j.timeline.Timeline;

import java.io.IOException;
import java.util.Map;
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
    private final Map<String, Timeline> cues = new ConcurrentHashMap<>();
    private Future runningFuture;

    public ShowElement(final String name, final Long id, final MessageExchange messageExchange,
                       final BrokerConnectionFactory brokerConnectionFactory) {
        this.name = name;
        this.id = id;
        this.messageExchange = messageExchange;
        this.brokerConnectionFactory = brokerConnectionFactory;
        executor = Executors.newFixedThreadPool(5);
    }

    public void init() {
        try {
            registerShowElement();
        } catch (final ConnectionFailedException e) {
            log.error("An error occurred while registering the Show Element={}. {}", this.toString(), e.getMessage());
        }
        log.info("Initialized Show Element={}", this.toString());
        handleMessage(SCFJMessage.builder().instruction(Instruction.IDLE).build());
    }

    /**
     * Registers a {@link Timeline} under a cue id. When a GO message naming this cue id is received,
     * this timeline is played instead of {@link #showSequence()} - see {@link #runShowLoop(String)}.
     * Typically called from a subclass constructor to set up every cue it supports.
     *
     * @param cueId the cue id a GO message must carry to select this timeline.
     * @param timeline the {@link Timeline} to play for this cue.
     */
    protected final void registerCue(final String cueId, final Timeline timeline) {
        cues.put(cueId, timeline);
    }

    private void registerShowElement() {
        final Mqtt3Client client = brokerConnectionFactory.newConnection(name + "-" + id, this::defaultToIdleOnUnexpectedDisconnect);
        client.toBlocking().connect();
        client.toAsync().subscribeWith()
                .topicFilter(messageExchange.getName())
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(publish -> {
                    try {
                        final SCFJMessage message = SCFJMessage.deserialize(publish.getPayloadAsBytes());
                        log.trace("The following message has been received=" + message.toString());
                        handleMessage(message);
                    } catch (final IOException e) {
                        log.error("Failed to deserialize an incoming message on Show Element={}. {}", this.toString(), e.getMessage());
                    }
                })
                .send();
    }

    /**
     * Fail-safe watchdog: defaults this Show Element to idle when the broker connection is lost
     * unexpectedly, rather than leaving it frozen mid-state while automatic reconnection is in
     * progress. Not triggered by a disconnect this client itself initiated.
     */
    private void defaultToIdleOnUnexpectedDisconnect(final MqttClientDisconnectedContext context) {
        if (context.getSource() != MqttDisconnectSource.USER) {
            log.error("Lost connection to the broker for Show Element={}, defaulting to idle until it reconnects. {}",
                    this.toString(), context.getCause() != null ? context.getCause().getMessage() : "unknown cause");
            handleMessage(SCFJMessage.builder().instruction(Instruction.IDLE).build());
        }
    }

    protected void handleMessage(final SCFJMessage message) {
        while (message.getStartTime() > System.currentTimeMillis()) {
            try {
                pause(100);
            } catch (final InterruptedException e) {
                log.error("Sleeping was interrupted while handling message on Show Element={}. {}", this.toString(), e.getStackTrace());
            }
        }
        if (runningFuture != null) {
            runningFuture.cancel(true);
        }
        runningFuture = executor.submit(new MessageTask(message));
    }

    private void analyzeMessage(final SCFJMessage message) {
        if (message.getInstruction() == Instruction.GO) {
            runShowLoop(message.getCueId());
        } else if (message.getInstruction() == Instruction.IDLE) {
            runIdleLoop();
        } else if (message.getInstruction() == Instruction.SHUTDOWN) {
            runShutdown();
        }
    }

    /**
     * Runs the cue named by {@code cueId} if one was registered via {@link #registerCue(String, Timeline)},
     * otherwise falls back to {@link #showSequence()} - so a Show Element that never registers named cues
     * behaves exactly as before, and a GO naming an unrecognized cue id degrades to the default sequence
     * rather than doing nothing.
     */
    private void runShowLoop(final String cueId) {
        final Timeline timeline = cueId != null ? cues.get(cueId) : null;
        log.info("Starting show loop for Show Element={}{}", this.toString(),
                timeline != null ? " (cue=" + cueId + ")" : "");
        try {
            if (timeline != null) {
                timeline.play();
            } else {
                showSequence();
            }
            runIdleLoop();
        } catch (final InterruptedException e) {
            log.trace("Thread is complete because a new Show Command was received for Show Element={}", this.toString());
        }
    }

    private void runIdleLoop() {
        log.info("Starting idle loop for Show Element={}", this.toString());
        try {
            while (true) {
                idleLoop();
            }
        } catch (final InterruptedException e) {
            log.trace("Thread is complete because a new Show Command was received for Show Element={}", this.toString());
        }
    }

    private void runShutdown() {
        runningFuture = null;
        executor.shutdownNow();
        shutdownProcedure();
        log.info("Shutdown was completed for Show Element={}", this.toString());
        exitJvm(0);
    }

    /**
     * Exits the JVM. A JDK 24+ SecurityManager can no longer intercept {@link System#exit(int)}
     * (JEP 486), so this exists as a testable seam - override it in a test subclass to verify
     * shutdown behavior without actually terminating the test JVM.
     *
     * @param status the exit status to pass to {@link System#exit(int)}.
     */
    protected void exitJvm(final int status) {
        System.exit(status);
    }

    /**
     * Pauses a thread for a tenth of a second.
     *
     * @param milliseconds the time to sleep in milliseconds
     * @throws InterruptedException
     */
    protected final void pause(final long milliseconds) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(milliseconds);
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
