package org.showcontrol4j.element.raspberrypi;

import com.pi4j.context.Context;
import com.pi4j.exception.ShutdownException;
import com.pi4j.io.exception.IOException;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.element.ShowElement;
import org.showcontrol4j.exchange.MessageExchange;

import java.util.concurrent.TimeUnit;

/**
 * A class for a basic GPIO Show Element for the Raspberry Pi, built on Pi4J V2. The loop and idle methods should
 * remain abstract so that they can be implemented at the time of instantiation.
 *
 * <p>The {@link Context} passed to this class should be built once per application - typically via
 * {@code Pi4J.newAutoContext()} - and shared across every Show Element running in the same process, not created
 * per element. Pi4J's context owns platform/provider registration for the whole JVM, and {@link #shutdownProcedure()}
 * shuts that shared context down.</p>
 *
 * @author James Hare
 */
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Slf4j
public abstract class GeneralPurposeIOShowElement extends ShowElement {

    @ToString.Include
    private final DigitalOutput pinOutput;
    private final Context pi4j;

    /**
     * Constructor.
     *
     * @param name the name of the Show Element.
     * @param id the id of the Show Element.
     * @param messageExchange the {@link MessageExchange} this Show Element listens on.
     * @param brokerConnectionFactory the {@link BrokerConnectionFactory} used to connect to the broker.
     * @param pi4j the application's shared Pi4J {@link Context} - see the class Javadoc.
     * @param bcmPin the BCM GPIO pin number this Show Element controls.
     */
    public GeneralPurposeIOShowElement(final String name, final long id, final MessageExchange messageExchange,
                                       final BrokerConnectionFactory brokerConnectionFactory, final Context pi4j,
                                       final int bcmPin) {
        super(name, id, messageExchange, brokerConnectionFactory);
        this.pi4j = pi4j;
        final DigitalOutputConfig pinConfig = DigitalOutput.newConfigBuilder(pi4j)
                .id(name)
                .name(name)
                .bcm(bcmPin)
                .shutdown(DigitalState.LOW)
                .initial(DigitalState.LOW)
                .build();
        this.pinOutput = pi4j.create(pinConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected abstract void showSequence() throws InterruptedException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected abstract void idleLoop() throws InterruptedException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void shutdownProcedure() {
        try {
            pi4j.shutdown();
        } catch (final ShutdownException e) {
            log.error("An error occurred while shutting down the Pi4J context for Show Element={}. {}", this, e.getMessage());
        }
    }

    /**
     * Sets the LED to high.
     */
    protected void turnOn() {
        execute(pinOutput::high, "turning on");
    }

    /**
     * Sets the LED to low.
     */
    protected void turnOff() {
        execute(pinOutput::low, "turning off");
    }

    /**
     * Toggles the LED state. If it is currently high, it will be set to low. If it is currently
     * low, it will be set to high.
     */
    protected void toggle() {
        execute(pinOutput::toggle, "toggling");
    }

    /**
     * Pulses the LED for a given amount of milliseconds.
     *
     * @param milliseconds the amount of time to pulse in milliseconds.
     * @param blockThread  if true, blocks the calling thread until the pulse completes; if false, the pulse
     *                     runs asynchronously and this method returns immediately.
     */
    protected void pulse(final long milliseconds, final boolean blockThread) {
        if (blockThread) {
            execute(() -> pinOutput.pulse((int) milliseconds, TimeUnit.MILLISECONDS), "pulsing");
        } else {
            pinOutput.pulseAsync((int) milliseconds, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Pulses the LED for a given amount of milliseconds. Runs asynchronously; this method returns immediately.
     *
     * @param milliseconds the amount of time to pulse in milliseconds.
     */
    protected void pulse(final long milliseconds) {
        pulse(milliseconds, false);
    }

    /**
     * Pulses the LED for a given amount of time. Blocks the calling thread until the pulse completes.
     *
     * @param duration the duration of the pulse period.
     * @param timeUnit the time unit of the duration.
     */
    protected void pulse(final long duration, final TimeUnit timeUnit) {
        execute(() -> pinOutput.pulse((int) duration, timeUnit), "pulsing");
    }

    /**
     * Returns the pin state.
     *
     * @return the pin state.
     */
    protected DigitalState getPinState() {
        return pinOutput.state();
    }

    /**
     * Runs a Pi4J GPIO call, logging (rather than propagating) any {@link IOException} it raises - consistent
     * with how the rest of this class reports hardware/broker errors.
     */
    private void execute(final GpioAction action, final String description) {
        try {
            action.run();
        } catch (final IOException e) {
            log.error("An error occurred while {} the GPIO pin on Show Element={}. {}", description, this, e.getMessage());
        }
    }

    @FunctionalInterface
    private interface GpioAction {
        void run() throws IOException;
    }

}
