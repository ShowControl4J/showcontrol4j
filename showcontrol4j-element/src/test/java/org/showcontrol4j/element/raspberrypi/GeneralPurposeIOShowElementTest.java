package org.showcontrol4j.element.raspberrypi;

import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalOutputProvider;
import org.hamcrest.CoreMatchers;
import org.junit.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.element.ShowElement;
import org.showcontrol4j.exchange.MessageExchange;
import org.showcontrol4j.message.SCFJMessage;
import org.showcontrol4j.message.ShowCommand;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for the {@link GeneralPurposeIOShowElement} class. Uses Pi4J's mock digital output provider
 * (pi4j-plugin-mock) so these run without real Raspberry Pi hardware.
 *
 * @author James Hare
 */
public class GeneralPurposeIOShowElementTest {

    private final String name = "Test Element Name";
    private final Long id = 123456L;
    private final int bcmPin = 4;
    private ExecutorService executor;
    private Context pi4j;

    @Mock
    private MessageExchange mockMessageExchange;
    @Mock
    private BrokerConnectionFactory mockBrokerConnectionFactory;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.openMocks(this);
        executor = Executors.newFixedThreadPool(5);
        pi4j = Pi4J.newContextBuilder()
                .noAutoDetect()
                .add(MockDigitalOutputProvider.newInstance())
                .build();
        final Mqtt3Client mockMqttClient = mock(Mqtt3Client.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockBrokerConnectionFactory.newConnection(anyString())).thenReturn(mockMqttClient);
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdownNow();
        if (!pi4j.isShutdown()) {
            pi4j.shutdown();
        }
    }

    private GeneralPurposeIOShowElement newElement() {
        return new GeneralPurposeIOShowElement(name, id, mockMessageExchange, mockBrokerConnectionFactory, pi4j, bcmPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                // do nothing.
            }
        };
    }

    @Test
    public void testConstructor() {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = newElement();

        assertThat(generalPurposeIOShowElement, CoreMatchers.instanceOf(GeneralPurposeIOShowElement.class));
        assertEquals(name, generalPurposeIOShowElement.getName());
        assertEquals(id, generalPurposeIOShowElement.getId());
        assertEquals(mockMessageExchange, generalPurposeIOShowElement.getMessageExchange());
        assertEquals(mockBrokerConnectionFactory, generalPurposeIOShowElement.getBrokerConnectionFactory());
        assertTrue(generalPurposeIOShowElement.getPinState().isLow());
    }

    @Test
    public void testShowSequence() throws Exception {
        final boolean[] ranShowSequence = {false};
        final SCFJMessage testGoSCFJMessage = ShowCommand.GO(0L);

        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, pi4j, bcmPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                ranShowSequence[0] = true;
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                // do nothing.
            }
        };

        generalPurposeIOShowElement.init();

        executor.submit(new TestTask(generalPurposeIOShowElement, testGoSCFJMessage));
        TimeUnit.MILLISECONDS.sleep(1000);
        assertTrue(ranShowSequence[0]);

        shutdownExecutorOnShowElement(generalPurposeIOShowElement);
    }

    @Test
    public void testShowSequence_withStartTime() throws Exception {
        final boolean[] ranShowSequence = {false};
        final SCFJMessage testGoSCFJMessageWithStartTime = ShowCommand.GO(5000L);

        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, pi4j, bcmPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                ranShowSequence[0] = true;
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                // do nothing.
            }
        };

        generalPurposeIOShowElement.init();

        executor.submit(new TestTask(generalPurposeIOShowElement, testGoSCFJMessageWithStartTime));
        TimeUnit.MILLISECONDS.sleep(1000);
        assertFalse(ranShowSequence[0]);
        TimeUnit.MILLISECONDS.sleep(5000);
        assertTrue(ranShowSequence[0]);

        shutdownExecutorOnShowElement(generalPurposeIOShowElement);
    }

    @Test
    public void testIdleLoop() throws Exception {
        final int[] idleLoopCounter = {0};
        final SCFJMessage testIdleSCFJMessage = ShowCommand.IDLE(0L);

        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, pi4j, bcmPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                int count = idleLoopCounter[0];
                count++;
                idleLoopCounter[0] = count;

                // Idle loop is designed to run continuously after constructor.
                // This is a way to control how many times idleLoop() gets called during this test for asserting.
                // This thread will end when element receives a new valid message.
                while (true) {
                    // wait for a new message.
                }
            }
        };

        generalPurposeIOShowElement.init();

        TimeUnit.MILLISECONDS.sleep(1000);

        // idleLoop() will have been called once from the constructor.
        assertEquals(1, idleLoopCounter[0]);

        executor.submit(new TestTask(generalPurposeIOShowElement, testIdleSCFJMessage));
        TimeUnit.MILLISECONDS.sleep(1000);

        // idleLoop() will have been called twice after receiving the idle message.
        assertEquals(2, idleLoopCounter[0]);

        shutdownExecutorOnShowElement(generalPurposeIOShowElement);
    }

    @Test
    public void testIdleLoop_withStartTime() throws Exception {
        final int[] idleLoopCounter = {0};
        final SCFJMessage testIdleSCFJMessageWithStartTime = ShowCommand.IDLE(5000L);

        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, pi4j, bcmPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                int count = idleLoopCounter[0];
                count++;
                idleLoopCounter[0] = count;

                // Idle loop is designed to run continuously after constructor.
                // This is a way to control how many times idleLoop() gets called during this test for asserting.
                // This thread will end when element receives a new valid message.
                while (true) {
                    // wait for a new message.
                }
            }
        };

        generalPurposeIOShowElement.init();

        TimeUnit.MILLISECONDS.sleep(1000);

        // idleLoop() will have been called once from the constructor.
        assertEquals(1, idleLoopCounter[0]);

        executor.submit(new TestTask(generalPurposeIOShowElement, testIdleSCFJMessageWithStartTime));
        TimeUnit.MILLISECONDS.sleep(5000);

        // idleLoop() will have been called twice after receiving the idle message.
        assertEquals(2, idleLoopCounter[0]);

        shutdownExecutorOnShowElement(generalPurposeIOShowElement);
    }

    @Test
    public void testShutdownProcedure() throws Exception {
        final SCFJMessage testShutdownSCFJMessage = ShowCommand.SHUTDOWN();
        final int[] exitStatus = {-1};

        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, pi4j, bcmPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void exitJvm(final int status) {
                exitStatus[0] = status;
            }
        };

        generalPurposeIOShowElement.init();

        final Context mockContext = mock(Context.class);

        final Field contextField = GeneralPurposeIOShowElement.class.getDeclaredField("pi4j");
        contextField.setAccessible(true);
        contextField.set(generalPurposeIOShowElement, mockContext);

        executor.submit(new TestTask(generalPurposeIOShowElement, testShutdownSCFJMessage));
        TimeUnit.MILLISECONDS.sleep(1000); // pause to give the system a chance to exit
        verify(mockContext, times(1)).shutdown();
        assertEquals(0, exitStatus[0]);
    }

    @Test
    public void testTurnOn() {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = newElement();

        generalPurposeIOShowElement.init();

        generalPurposeIOShowElement.turnOn();
        assertTrue(generalPurposeIOShowElement.getPinState().isHigh());
    }

    @Test
    public void testTurnOff() {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = newElement();

        generalPurposeIOShowElement.init();

        generalPurposeIOShowElement.turnOff();
        assertTrue(generalPurposeIOShowElement.getPinState().isLow());
    }

    @Test
    public void testToggle() {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = newElement();

        generalPurposeIOShowElement.init();

        assertTrue(generalPurposeIOShowElement.getPinState().isLow());
        generalPurposeIOShowElement.toggle();
        assertTrue(generalPurposeIOShowElement.getPinState().isHigh());
        generalPurposeIOShowElement.toggle();
        assertTrue(generalPurposeIOShowElement.getPinState().isLow());
    }

    @Test
    public void testPulse() throws Exception {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = newElement();

        generalPurposeIOShowElement.init();

        final DigitalOutput mockDigitalOutput = mock(DigitalOutput.class);

        final Field field = GeneralPurposeIOShowElement.class.getDeclaredField("pinOutput");
        field.setAccessible(true);
        field.set(generalPurposeIOShowElement, mockDigitalOutput);

        generalPurposeIOShowElement.pulse(2000L, true);
        verify(mockDigitalOutput, times(1)).pulse(2000, TimeUnit.MILLISECONDS);

        generalPurposeIOShowElement.pulse(3000L, false);
        verify(mockDigitalOutput, times(1)).pulseAsync(3000, TimeUnit.MILLISECONDS);

        generalPurposeIOShowElement.pulse(4000L);
        verify(mockDigitalOutput, times(1)).pulseAsync(4000, TimeUnit.MILLISECONDS);

        generalPurposeIOShowElement.pulse(5000L, TimeUnit.MILLISECONDS);
        verify(mockDigitalOutput, times(1)).pulse(5000, TimeUnit.MILLISECONDS);

        generalPurposeIOShowElement.pulse(6000L, TimeUnit.MINUTES);
        verify(mockDigitalOutput, times(1)).pulse(6000, TimeUnit.MINUTES);

        generalPurposeIOShowElement.pulse(7000L, TimeUnit.HOURS);
        verify(mockDigitalOutput, times(1)).pulse(7000, TimeUnit.HOURS);
    }

    @Test
    public void testGetPinState() {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = newElement();

        generalPurposeIOShowElement.init();

        assertTrue(generalPurposeIOShowElement.getPinState().isLow());
        generalPurposeIOShowElement.toggle();
        assertTrue(generalPurposeIOShowElement.getPinState().isHigh());
    }

    @Test
    public void testToString() {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = newElement();

        generalPurposeIOShowElement.init();

        assertTrue(generalPurposeIOShowElement.toString()
                .startsWith("GeneralPurposeIOShowElement(super=ShowElement(name=Test Element Name, id=123456), pinOutput="));
    }

    //------------------------------------ HELPER METHODS ------------------------------------//

    private static Method getHandleMessageMethod() {
        Method handleMessageMethod = null;
        try {
            handleMessageMethod = ShowElement.class.getDeclaredMethod("handleMessage", SCFJMessage.class);
        } catch (final NoSuchMethodException e) {
            e.printStackTrace();
        }
        assert handleMessageMethod != null;
        handleMessageMethod.setAccessible(true);
        return handleMessageMethod;
    }

    private static void shutdownExecutorOnShowElement(final GeneralPurposeIOShowElement generalPurposeIOShowElement) throws Exception {
        final Field executorField = ShowElement.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        final ExecutorService executorService = (ExecutorService) executorField.get(generalPurposeIOShowElement);
        executorService.shutdownNow();
    }

    private static class TestTask implements Runnable {

        private final GeneralPurposeIOShowElement element;
        private final SCFJMessage message;

        public TestTask(final GeneralPurposeIOShowElement element, final SCFJMessage message) {
            this.element = element;
            this.message = message;
        }

        @Override
        public void run() {
            final Method handleMessageMethod = getHandleMessageMethod();
            try {
                handleMessageMethod.invoke(element, message);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
