package org.showcontrol4j.element.raspberrypi;

import com.pi4j.io.gpio.*;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.element.ShowElement;
import org.showcontrol4j.exchange.MessageExchange;

import java.util.concurrent.TimeUnit;

/**
 * A class for a basic GPIO Show Element for the Raspberry Pi. The loop and idle methods should remain abstract so that
 * they can be implemented at the time of instantiation.
 *
 * @author James Hare
 */
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Slf4j
public abstract class GeneralPurposeIOShowElement extends ShowElement {

    @ToString.Include
    private final GpioPinDigitalOutput pinOutput;
    private final GpioController gpioController;

    public GeneralPurposeIOShowElement(final String name, final long id, final MessageExchange messageExchange,
                                       final BrokerConnectionFactory brokerConnectionFactory, final Pin pin) {
        super(name, id, messageExchange, brokerConnectionFactory);
        gpioController = GpioFactory.getInstance();
        pinOutput = gpioController.provisionDigitalOutputPin(pin, name, PinState.LOW);
        pinOutput.setShutdownOptions(true, PinState.LOW);
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
        gpioController.shutdown();
    }

    /**
     * Sets the LED to high.
     */
    protected void turnOn() {
        pinOutput.high();
    }

    /**
     * Sets the LED to low.
     */
    protected void turnOff() {
        pinOutput.low();
    }

    /**
     * Toggles the LED state. If it is currently high, it will be set to low. If it is currently
     * low, it will be set to high.
     */
    protected void toggle() {
        if (pinOutput.isHigh()) {
            pinOutput.low();
        } else {
            pinOutput.high();
        }
    }

    /**
     * Pulses the LED for a given amount of milliseconds.
     *
     * @param milliseconds the amount of time to pulse in milliseconds.
     * @param blockThread  if the thread should be blocked from other calls.
     */
    protected void pulse(final long milliseconds, final boolean blockThread) {
        pinOutput.pulse(milliseconds, blockThread);
    }

    /**
     * Pulses the LED for a given amount of milliseconds.
     *
     * @param milliseconds the amount of time to pulse in milliseconds.
     */
    protected void pulse(final long milliseconds) {
        pinOutput.pulse(milliseconds);
    }

    /**
     * Pulses the LED for a given amount of time.
     *
     * @param duration the duration of the pulse period.
     * @param timeUnit the time unit of the duration.
     */
    protected void pulse(final long duration, final TimeUnit timeUnit) {
        pinOutput.pulse(duration, timeUnit);
    }

    /**
     * Returns the pin state.
     *
     * @return the pin state.
     */
    protected PinState getPinState() {
        return pinOutput.getState();
    }

}
