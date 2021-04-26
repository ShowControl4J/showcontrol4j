package org.showcontrol4j.element.raspberrypi;

import com.pi4j.io.gpio.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.impl.AMQImpl;
import org.hamcrest.CoreMatchers;
import org.junit.*;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.element.ShowElement;
import org.showcontrol4j.exchange.MessageExchange;
import org.showcontrol4j.message.SCFJMessage;
import org.showcontrol4j.message.ShowCommand;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for the {@link GeneralPurposeIOShowElement} class.
 *
 * @author James Hare
 */
public class GeneralPurposeIOShowElementTest {

    private final String name = "Test Element Name";
    private final Long id = 123456L;
    private ExecutorService executor;
    static final SimulatedGpioProvider simulator = new SimulatedGpioProvider();

    @Mock
    private MessageExchange mockMessageExchange;
    @Mock
    private BrokerConnectionFactory mockBrokerConnectionFactory;
    @Mock
    private Connection mockConnection;
    @Mock
    private Channel mockChannel;
    @Mock
    private AMQImpl.Exchange.DeclareOk mockExchangeDeclareOk;
    @Mock
    private AMQImpl.Queue.BindOk mockBindOk;
    @Mock
    private AMQImpl.Queue.DeclareOk mockQueueDeclareOk;
    @Mock
    private Pin mockIOPin;
    @Mock
    private EnumSet<PinMode> mockPinModeEnumSet;
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @BeforeClass
    public static void before_all() {
        GpioFactory.setDefaultProvider(simulator);
    }

    @Before
    public void init() throws Exception {
        MockitoAnnotations.openMocks(this);
        executor = Executors.newFixedThreadPool(5);
        when(mockBrokerConnectionFactory.newConnection()).thenReturn(mockConnection);
        when(mockConnection.createChannel()).thenReturn(mockChannel);
        when(mockChannel.exchangeDeclare(anyString(), anyString())).thenReturn(mockExchangeDeclareOk);
        when(mockChannel.queueDeclare()).thenReturn(mockQueueDeclareOk);
        when(mockQueueDeclareOk.getQueue()).thenReturn("test");
        when(mockChannel.queueBind(anyString(), anyString(), anyString())).thenReturn(mockBindOk);
        when(mockIOPin.getProvider()).thenReturn("RaspberryPi GPIO Provider");
        when(mockIOPin.getSupportedPinModes()).thenReturn(mockPinModeEnumSet);
        when(mockPinModeEnumSet.contains(any(PinMode.class))).thenReturn(true);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testConstructor() {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, mockIOPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                // do nothing.
            }
        };

        assertThat(generalPurposeIOShowElement, CoreMatchers.instanceOf(GeneralPurposeIOShowElement.class));
        assertEquals(name, generalPurposeIOShowElement.getName());
        assertEquals(id, generalPurposeIOShowElement.getId());
        assertEquals(mockMessageExchange, generalPurposeIOShowElement.getMessageExchange());
        assertEquals(mockBrokerConnectionFactory, generalPurposeIOShowElement.getBrokerConnectionFactory());
        assertEquals(PinState.LOW, generalPurposeIOShowElement.getPinState());
    }

    @Test
    public void testShowSequence() throws Exception {
        final boolean[] ranShowSequence = {false};
        final SCFJMessage testGoSCFJMessage = ShowCommand.GO(0L);

        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, mockIOPin) {
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
                mockMessageExchange, mockBrokerConnectionFactory, mockIOPin) {
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
                mockMessageExchange, mockBrokerConnectionFactory, mockIOPin) {
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

        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id, mockMessageExchange, mockBrokerConnectionFactory, mockIOPin) {
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

        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, mockIOPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                // do nothing.
            }
        };

        generalPurposeIOShowElement.init();

        final GpioController mockGpioController = mock(GpioController.class);

        final Field gpioControllerField = GeneralPurposeIOShowElement.class.getDeclaredField("gpioController");
        gpioControllerField.setAccessible(true);
        final Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(gpioControllerField, gpioControllerField.getModifiers() & ~Modifier.FINAL);
        gpioControllerField.set(generalPurposeIOShowElement, mockGpioController);

        exit.expectSystemExitWithStatus(0);
        executor.submit(new TestTask(generalPurposeIOShowElement, testShutdownSCFJMessage));
        TimeUnit.MILLISECONDS.sleep(1000); // pause to give the system a chance to exit
        verify(mockGpioController, times(1)).shutdown();
    }

    @Test
    public void testTurnOn() {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, mockIOPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                // do nothing.
            }
        };

        generalPurposeIOShowElement.init();

        generalPurposeIOShowElement.turnOn();
        assertTrue(generalPurposeIOShowElement.getPinState().isHigh());
    }

    @Test
    public void testTurnOff() {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, mockIOPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                // do nothing.
            }
        };

        generalPurposeIOShowElement.init();

        generalPurposeIOShowElement.turnOff();
        assertTrue(generalPurposeIOShowElement.getPinState().isLow());
    }

    @Test
    public void testToggle() {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, mockIOPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                // do nothing.
            }
        };

        generalPurposeIOShowElement.init();

        assertTrue(generalPurposeIOShowElement.getPinState().isLow());
        generalPurposeIOShowElement.toggle();
        assertTrue(generalPurposeIOShowElement.getPinState().isHigh());
        generalPurposeIOShowElement.toggle();
        assertTrue(generalPurposeIOShowElement.getPinState().isLow());
    }

    @Test
    public void testPulse() throws Exception {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, mockIOPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                // do nothing.
            }
        };

        generalPurposeIOShowElement.init();

        final GpioPinDigitalOutput mockGpioPinDigitalOutput = mock(GpioPinDigitalOutput.class);

        final Field field = GeneralPurposeIOShowElement.class.getDeclaredField("pinOutput");
        field.setAccessible(true);
        final Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(generalPurposeIOShowElement, mockGpioPinDigitalOutput);

        generalPurposeIOShowElement.pulse(2000L, true);
        verify(mockGpioPinDigitalOutput, times(1)).pulse(2000L, true);
        generalPurposeIOShowElement.pulse(2000L, false);
        verify(mockGpioPinDigitalOutput, times(1)).pulse(2000L, false);
        generalPurposeIOShowElement.pulse(2000L);
        verify(mockGpioPinDigitalOutput, times(1)).pulse(2000L);
        generalPurposeIOShowElement.pulse(2000L, TimeUnit.MILLISECONDS);
        verify(mockGpioPinDigitalOutput, times(1)).pulse(2000L, TimeUnit.MILLISECONDS);
        generalPurposeIOShowElement.pulse(2000L, TimeUnit.MINUTES);
        verify(mockGpioPinDigitalOutput, times(1)).pulse(2000L, TimeUnit.MINUTES);
        generalPurposeIOShowElement.pulse(2000L, TimeUnit.HOURS);
        verify(mockGpioPinDigitalOutput, times(1)).pulse(2000L, TimeUnit.HOURS);
    }

    @Test
    public void testGetPinState() {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, mockIOPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                // do nothing.
            }
        };

        generalPurposeIOShowElement.init();

        assertEquals(PinState.LOW, generalPurposeIOShowElement.getPinState());
        generalPurposeIOShowElement.toggle();
        assertEquals(PinState.HIGH, generalPurposeIOShowElement.getPinState());
    }

    @Test
    public void testToString() {
        final GeneralPurposeIOShowElement generalPurposeIOShowElement = new GeneralPurposeIOShowElement(name, id,
                mockMessageExchange, mockBrokerConnectionFactory, mockIOPin) {
            @Override
            protected void showSequence() throws InterruptedException {
                // do nothing.
            }

            @Override
            protected void idleLoop() throws InterruptedException {
                // do nothing.
            }
        };

        generalPurposeIOShowElement.init();

        assertEquals("GeneralPurposeIOShowElement(super=ShowElement(name=Test Element Name, id=123456)," +
                " pinOutput=\"Test Element Name\" <mockIOPin>)", generalPurposeIOShowElement.toString());
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
