package org.showcontrol4j.element;

import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.exchange.MessageExchange;
import org.showcontrol4j.message.Instruction;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link ShowElement} class.
 *
 * @author James Hare
 */
public class ShowElementTest {

    private final String testElementName = "Test Element Name";
    private final Long testElementId = 123456L;
    private ExecutorService executor;
    final SCFJMessage testGoSCFJMessage = SCFJMessage.builder().instruction(Instruction.GO).build();
    final SCFJMessage testIdleSCFJMessage = SCFJMessage.builder().instruction(Instruction.IDLE).build();
    final SCFJMessage testShutdownSCFJMessage = SCFJMessage.builder().instruction(Instruction.SHUTDOWN).build();

    @Mock
    private MessageExchange mockMessageExchange;
    @Mock
    private BrokerConnectionFactory mockBrokerConnectionFactory;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
        executor = Executors.newFixedThreadPool(5);
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdownNow();
    }

    @Test
    public void testConstructor() throws Exception {
        setupMockRules();

        final ShowElement showElement = new ShowElement(testElementName, testElementId, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            public void showSequence() throws InterruptedException {
                // do nothing
            }

            @Override
            public void idleLoop() throws InterruptedException {
                // do nothing
            }

            @Override
            public void shutdownProcedure() {
                // do nothing
            }
        };

        assertThat(showElement, CoreMatchers.instanceOf(ShowElement.class));
        assertEquals(testElementName, showElement.getName());
        assertEquals(testElementId, showElement.getId());
        assertEquals(mockMessageExchange, showElement.getMessageExchange());
        assertEquals(mockBrokerConnectionFactory, showElement.getBrokerConnectionFactory());
    }

    /**
     * A test to ensure that the show element is paused for a given time of 1 second with a fault
     * tolerance of 1/10th of a second.
     */
    @Test
    public void testPause() throws Exception {
        setupMockRules();

        final ShowElement showElement = new ShowElement(testElementName, testElementId, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            public void showSequence() throws InterruptedException {
                // do nothing
            }

            @Override
            public void idleLoop() throws InterruptedException {
                // do nothing
            }

            @Override
            public void shutdownProcedure() {
                // do nothing
            }
        };

        showElement.init();

        final long timeStampOne = System.currentTimeMillis();
        showElement.pause(100);
        final long timeStampTwo = System.currentTimeMillis();
        final long totalPaused = timeStampTwo - timeStampOne;

        TestCase.assertTrue(totalPaused > 0);
        TestCase.assertTrue(totalPaused < 200);
    }

    @Test
    public void testHandleMessage_goMessage() throws Exception {
        setupMockRules();

        final boolean[] ranShowSequence = {false};
        final boolean[] ranIdleLoop = {false};

        final ShowElement showElement = new ShowElement(testElementName, testElementId, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            public void showSequence() throws InterruptedException {
                ranShowSequence[0] = true;
            }

            @Override
            public void idleLoop() throws InterruptedException {
                ranIdleLoop[0] = true;
            }

            @Override
            public void shutdownProcedure() {
                // do nothing
            }
        };

        showElement.init();

        executor.submit(new TestTask(showElement, testGoSCFJMessage));
        TimeUnit.MILLISECONDS.sleep(1000);

        assertTrue(ranShowSequence[0]);
        assertTrue(ranIdleLoop[0]);

        shutdownExecutorOnShowElementBase(showElement);
    }

    @Test
    public void testHandleMessage_goMessageTwoMessages() throws Exception {
        setupMockRules();
        final int[] showSequenceCounter = {0};

        final ShowElement showElement = new ShowElement(testElementName, testElementId, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            public void showSequence() throws InterruptedException {
                int count = showSequenceCounter[0];
                count++;
                showSequenceCounter[0] = count;
            }

            @Override
            public void idleLoop() throws InterruptedException {
                // do nothing
            }

            @Override
            public void shutdownProcedure() {
                // do nothing
            }
        };

        showElement.init();

        executor.submit(new TestTask(showElement, testGoSCFJMessage));
        TimeUnit.MILLISECONDS.sleep(1000);

        assertEquals(1, showSequenceCounter[0]);

        executor.submit(new TestTask(showElement, testGoSCFJMessage));
        TimeUnit.MILLISECONDS.sleep(1000);

        assertEquals(2, showSequenceCounter[0]);

        shutdownExecutorOnShowElementBase(showElement);
    }

    @Test
    public void testHandleMessage_goMessageWithStartTime() throws Exception {
        setupMockRules();
        final boolean[] ranShowSequence = {false};
        final SCFJMessage testGoSCFJMessageWithStartTime = ShowCommand.GO(5000L);

        final ShowElement showElement = new ShowElement(testElementName, testElementId, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            public void showSequence() throws InterruptedException {
                ranShowSequence[0] = true;
            }

            @Override
            public void idleLoop() throws InterruptedException {
                // do nothing
            }

            @Override
            public void shutdownProcedure() {
                // do nothing
            }
        };

        showElement.init();

        executor.submit(new TestTask(showElement, testGoSCFJMessageWithStartTime));
        TimeUnit.MILLISECONDS.sleep(1000);
        assertFalse(ranShowSequence[0]);
        TimeUnit.MILLISECONDS.sleep(5000); // wait for start time
        assertTrue(ranShowSequence[0]);

        shutdownExecutorOnShowElementBase(showElement);
    }

    @Test
    public void testHandleMessage_idleMessage() throws Exception {
        setupMockRules();
        final boolean[] ranIdleLoop = {false};

        final ShowElement showElement = new ShowElement(testElementName, testElementId, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            public void showSequence() throws InterruptedException {
                // do nothing
            }

            @Override
            public void idleLoop() throws InterruptedException {
                ranIdleLoop[0] = true;
            }

            @Override
            public void shutdownProcedure() {
                // do nothing
            }
        };

        showElement.init();

        executor.submit(new TestTask(showElement, testIdleSCFJMessage));
        TimeUnit.MILLISECONDS.sleep(1000);

        assertTrue(ranIdleLoop[0]);

        shutdownExecutorOnShowElementBase(showElement);
    }

    @Test
    public void testHandleMessage_shutdownMessage() throws Exception {
        setupMockRules();
        final boolean[] ranShutdownProcedure = {false};
        final int[] exitStatus = {-1};

        final ShowElement showElement = new ShowElement(testElementName, testElementId, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            public void showSequence() throws InterruptedException {
                // do nothing
            }

            @Override
            public void idleLoop() throws InterruptedException {
                // do nothing
            }

            @Override
            public void shutdownProcedure() {
                ranShutdownProcedure[0] = true;
            }

            @Override
            protected void exitJvm(final int status) {
                exitStatus[0] = status;
            }
        };

        showElement.init();

        executor.submit(new TestTask(showElement, testShutdownSCFJMessage));
        TimeUnit.MILLISECONDS.sleep(1000);

        assertTrue(ranShutdownProcedure[0]);
        assertEquals(0, exitStatus[0]);

        shutdownExecutorOnShowElementBase(showElement);
    }

    @Test
    public void testConnectionWatchdog_unexpectedDisconnect_defaultsToIdle() throws Exception {
        setupMockRules();
        final boolean[] ranShowSequence = {false};
        final boolean[] ranIdleLoop = {false};

        final ShowElement showElement = new ShowElement(testElementName, testElementId, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            public void showSequence() throws InterruptedException {
                ranShowSequence[0] = true;
                while (true) {
                    // hold in the show state until interrupted, so the watchdog has something to interrupt.
                }
            }

            @Override
            public void idleLoop() throws InterruptedException {
                ranIdleLoop[0] = true;
                while (true) {
                    // hold in the idle state so the assertion below isn't racing a second call.
                }
            }

            @Override
            public void shutdownProcedure() {
                // do nothing
            }
        };

        showElement.init();
        executor.submit(new TestTask(showElement, testGoSCFJMessage));
        TimeUnit.MILLISECONDS.sleep(500);
        assertTrue(ranShowSequence[0]);
        ranIdleLoop[0] = false; // reset the IDLE run() already did as part of init()

        final MqttClientDisconnectedListener disconnectedListener = capturedDisconnectedListener(mockBrokerConnectionFactory);
        disconnectedListener.onDisconnected(mockDisconnectedContext(MqttDisconnectSource.SERVER));
        TimeUnit.MILLISECONDS.sleep(500);

        assertTrue(ranIdleLoop[0]);

        shutdownExecutorOnShowElementBase(showElement);
    }

    @Test
    public void testConnectionWatchdog_userInitiatedDisconnect_doesNotTriggerFailSafe() throws Exception {
        setupMockRules();
        final boolean[] ranShowSequence = {false};
        final boolean[] ranIdleLoop = {false};

        final ShowElement showElement = new ShowElement(testElementName, testElementId, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            public void showSequence() throws InterruptedException {
                ranShowSequence[0] = true;
                while (true) {
                    // hold in the show state - a USER-sourced disconnect should not interrupt this.
                }
            }

            @Override
            public void idleLoop() throws InterruptedException {
                ranIdleLoop[0] = true;
                while (true) {
                    // hold in the idle state so a later, spurious re-entry would be detectable -
                    // the initial IDLE run from init() is expected to land here and stay.
                }
            }

            @Override
            public void shutdownProcedure() {
                // do nothing
            }
        };

        showElement.init();
        TimeUnit.MILLISECONDS.sleep(200); // let the initial IDLE run from init() land in idleLoop()
        executor.submit(new TestTask(showElement, testGoSCFJMessage));
        TimeUnit.MILLISECONDS.sleep(500);
        assertTrue(ranShowSequence[0]);
        ranIdleLoop[0] = false; // reset - only a fresh idleLoop() entry should flip this back to true

        final MqttClientDisconnectedListener disconnectedListener = capturedDisconnectedListener(mockBrokerConnectionFactory);
        disconnectedListener.onDisconnected(mockDisconnectedContext(MqttDisconnectSource.USER));
        TimeUnit.MILLISECONDS.sleep(500);

        assertFalse(ranIdleLoop[0]);

        shutdownExecutorOnShowElementBase(showElement);
    }

    @Test
    public void testToString() throws Exception {
        setupMockRules();

        final ShowElement showElement = new ShowElement(testElementName, testElementId, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            public void showSequence() throws InterruptedException {
                // do nothing
            }

            @Override
            public void idleLoop() throws InterruptedException {
                // do nothing
            }

            @Override
            public void shutdownProcedure() {
                // do nothing
            }
        };

        final String expected = "ShowElement(name=Test Element Name, id=123456)";
        assertEquals(expected, showElement.toString());
    }

    //------------------------------------ HELPER METHODS ------------------------------------//

    /**
     * Stubs {@link BrokerConnectionFactory#newConnection(String, MqttClientDisconnectedListener)} to
     * return a deep-stubbed MQTT client mock, so that {@link ShowElement#init()}'s connect/subscribe
     * chain succeeds without needing to hand-mock every stage of HiveMQ's fluent builder API.
     */
    private void setupMockRules() {
        final Mqtt3Client mockMqttClient = mock(Mqtt3Client.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockBrokerConnectionFactory.newConnection(anyString(), any())).thenReturn(mockMqttClient);
        when(mockMessageExchange.getName()).thenReturn("test");
    }

    /**
     * Captures the {@link MqttClientDisconnectedListener} that {@code showElement} registered via
     * {@link BrokerConnectionFactory#newConnection(String, MqttClientDisconnectedListener)} during
     * {@link ShowElement#init()}, so a test can simulate a broker disconnect by invoking it directly.
     */
    private static MqttClientDisconnectedListener capturedDisconnectedListener(final BrokerConnectionFactory mockBrokerConnectionFactory) {
        final ArgumentCaptor<MqttClientDisconnectedListener> listenerCaptor = ArgumentCaptor.forClass(MqttClientDisconnectedListener.class);
        verify(mockBrokerConnectionFactory).newConnection(anyString(), listenerCaptor.capture());
        return listenerCaptor.getValue();
    }

    private static MqttClientDisconnectedContext mockDisconnectedContext(final MqttDisconnectSource source) {
        final MqttClientDisconnectedContext mockContext = mock(MqttClientDisconnectedContext.class);
        when(mockContext.getSource()).thenReturn(source);
        when(mockContext.getCause()).thenReturn(new RuntimeException("simulated disconnect"));
        return mockContext;
    }

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

    private static void shutdownExecutorOnShowElementBase(final ShowElement showElementBase) throws Exception {
        final Field executorField = ShowElement.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        final ExecutorService executorService = (ExecutorService) executorField.get(showElementBase);
        executorService.shutdownNow();
    }

    private static class TestTask implements Runnable {

        private final ShowElement element;
        private final SCFJMessage message;

        public TestTask(final ShowElement element, final SCFJMessage message) {
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
