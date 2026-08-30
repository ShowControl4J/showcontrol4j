package org.showcontrol4j.trigger.keyboard;

import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.exchange.MessageExchange;

import java.lang.reflect.Field;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for the {@link KeyboardShowTrigger} class.
 *
 * @author James Hare
 */
public class KeyboardShowTriggerTest {

    private final String name = "Test Trigger";
    private final Long id = 123456L;
    private final Long syncTimeout = 5000L;
    private final String triggerKey = "a";
    private ExecutorService executor;
    private Mqtt3BlockingClient mockBlockingClient;

    @Mock
    private MessageExchange mockMessageExchange;
    @Mock
    private BrokerConnectionFactory mockBrokerConnectionFactory;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.openMocks(this);
        executor = Executors.newFixedThreadPool(5);
        setupMockRules();
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testConstructor() {
        final KeyboardShowTrigger keyboardShowTrigger = new KeyboardShowTrigger(triggerKey, name, id, syncTimeout,
                mockMessageExchange, mockBrokerConnectionFactory);

        assertThat(keyboardShowTrigger, CoreMatchers.instanceOf(KeyboardShowTrigger.class));
        assertEquals(triggerKey, keyboardShowTrigger.getTriggerKey());
        assertEquals(name, keyboardShowTrigger.getName());
        assertEquals(id, keyboardShowTrigger.getId());
        assertEquals(syncTimeout, keyboardShowTrigger.getSyncTimeout());
        assertEquals(mockMessageExchange, keyboardShowTrigger.getMessageExchange());
        assertEquals(mockBrokerConnectionFactory, keyboardShowTrigger.getBrokerConnectionFactory());
        assertEquals(mockBlockingClient, keyboardShowTrigger.getClient());
        assertThat(keyboardShowTrigger.getScanner(), CoreMatchers.instanceOf(Scanner.class));
    }

    @Test
    public void testStartListener_goMessage() throws Exception {
        final KeyboardShowTrigger keyboardShowTrigger = new KeyboardShowTrigger(triggerKey, name, id, syncTimeout,
                mockMessageExchange, mockBrokerConnectionFactory);
        final Scanner mockScanner = mock(Scanner.class);
        when(mockScanner.next()).thenReturn(triggerKey);

        final Field scannerField = keyboardShowTrigger.getClass().getDeclaredField("scanner");
        scannerField.setAccessible(true);
        scannerField.set(keyboardShowTrigger, mockScanner);

        executor.submit(new TestTask(keyboardShowTrigger));
        Thread.sleep(500);
        verify(mockBlockingClient.publishWith(), atLeast(1)).topic("test");
    }

    @Test
    public void testStartListener_idleMessage() throws Exception {
        final KeyboardShowTrigger keyboardShowTrigger = new KeyboardShowTrigger(triggerKey, name, id, syncTimeout,
                mockMessageExchange, mockBrokerConnectionFactory);
        final Scanner mockScanner = mock(Scanner.class);
        when(mockScanner.next()).thenReturn("IDLE");

        final Field scannerField = keyboardShowTrigger.getClass().getDeclaredField("scanner");
        scannerField.setAccessible(true);
        scannerField.set(keyboardShowTrigger, mockScanner);

        executor.submit(new TestTask(keyboardShowTrigger));
        Thread.sleep(500);
        verify(mockBlockingClient.publishWith(), atLeast(1)).topic("test");
    }

    @Test
    public void testStartListener_shutdownMessage() throws Exception {
        final KeyboardShowTrigger keyboardShowTrigger = new KeyboardShowTrigger(triggerKey, name, id, syncTimeout,
                mockMessageExchange, mockBrokerConnectionFactory);
        final Scanner mockScanner = mock(Scanner.class);
        when(mockScanner.next()).thenReturn("SHUTDOWN");

        final Field scannerField = keyboardShowTrigger.getClass().getDeclaredField("scanner");
        scannerField.setAccessible(true);
        scannerField.set(keyboardShowTrigger, mockScanner);

        executor.submit(new TestTask(keyboardShowTrigger));
        Thread.sleep(1000);
        verify(mockBlockingClient.publishWith(), atLeast(1)).topic("test");
    }

    @Test
    public void testStartListener_exit() throws Exception {
        final boolean[] exited = {false};
        final int[] exitStatus = {-1};

        final KeyboardShowTrigger keyboardShowTrigger = new KeyboardShowTrigger(triggerKey, name, id, syncTimeout,
                mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            protected void exitJvm(final int status) {
                exited[0] = true;
                exitStatus[0] = status;
                // Prevent startListener()'s infinite loop from spinning after "exit" - mirrors
                // the JVM actually terminating here, without needing a real System.exit(int).
                throw new StopListenerLoop();
            }
        };
        final Scanner mockScanner = mock(Scanner.class);
        when(mockScanner.next()).thenReturn("EXIT");

        final Field scannerField = KeyboardShowTrigger.class.getDeclaredField("scanner");
        scannerField.setAccessible(true);
        scannerField.set(keyboardShowTrigger, mockScanner);

        executor.submit(new TestTask(keyboardShowTrigger));
        Thread.sleep(1000); // pause to give the listener a chance to process "EXIT"

        assertTrue(exited[0]);
        assertEquals(0, exitStatus[0]);
    }

    @Test
    public void testToString() throws Exception {
        final KeyboardShowTrigger keyboardShowTrigger = new KeyboardShowTrigger(triggerKey, name, id, syncTimeout,
                mockMessageExchange, mockBrokerConnectionFactory);

        final String expected = "KeyboardShowTrigger(super=ShowTrigger(name=Test Trigger, id=123456, syncTimeout=5000)," +
                " triggerKey=a)";
        assertEquals(expected, keyboardShowTrigger.toString());
    }

    //------------------------------------ HELPER METHODS ------------------------------------//

    private void setupMockRules() {
        final Mqtt3Client mockMqttClient = mock(Mqtt3Client.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockBrokerConnectionFactory.newConnection(anyString())).thenReturn(mockMqttClient);
        mockBlockingClient = mockMqttClient.toBlocking();
        when(mockMessageExchange.getName()).thenReturn("test");
    }

    /**
     * Thrown from the {@code exitJvm} override in {@code testStartListener_exit} to unwind out of
     * {@link KeyboardShowTrigger#startListener()}'s infinite loop, standing in for the JVM
     * termination a real {@code System.exit()} would cause.
     */
    private static class StopListenerLoop extends RuntimeException {
    }

    private class TestTask implements Runnable {

        private final KeyboardShowTrigger trigger;

        public TestTask(final KeyboardShowTrigger trigger) {
            this.trigger = trigger;
        }

        @Override
        public void run() {
            try {
                trigger.startListener();
            } catch (final StopListenerLoop e) {
                // expected - see StopListenerLoop's javadoc.
            }
        }
    }

}
