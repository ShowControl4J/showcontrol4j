package org.showcontrol4j.trigger.keyboard;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.impl.AMQImpl;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.exchange.MessageExchange;

import java.lang.reflect.Field;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
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
    private AMQImpl.Queue.DeclareOk mockQueueDeclareOk;

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
        assertEquals(mockChannel, keyboardShowTrigger.getChannel());
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
        verify(mockChannel, atLeast(1)).basicPublish(eq("test"), eq(""), eq(null), any());
    }

    @Test
    public void testStartListener_stopMessage() throws Exception {
        final KeyboardShowTrigger keyboardShowTrigger = new KeyboardShowTrigger(triggerKey, name, id, syncTimeout,
                mockMessageExchange, mockBrokerConnectionFactory);
        final Scanner mockScanner = mock(Scanner.class);
        when(mockScanner.next()).thenReturn("STOP");

        final Field scannerField = keyboardShowTrigger.getClass().getDeclaredField("scanner");
        scannerField.setAccessible(true);
        scannerField.set(keyboardShowTrigger, mockScanner);

        executor.submit(new TestTask(keyboardShowTrigger));
        Thread.sleep(1000);
        verify(mockChannel, atLeast(1)).basicPublish(eq("test"), eq(""), eq(null), any());
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
        verify(mockChannel, atLeast(1)).basicPublish(eq("test"), eq(""), eq(null), any());
    }

    //------------------------------------ HELPER METHODS ------------------------------------//

    private void setupMockRules() throws Exception {
        when(mockBrokerConnectionFactory.newConnection()).thenReturn(mockConnection);
        when(mockConnection.createChannel()).thenReturn(mockChannel);
        when(mockChannel.exchangeDeclare(anyString(), anyString())).thenReturn(mockExchangeDeclareOk);
        when(mockChannel.queueDeclare()).thenReturn(mockQueueDeclareOk);
        when(mockMessageExchange.getName()).thenReturn("test");
    }

    private class TestTask implements Runnable {

        private final KeyboardShowTrigger trigger;

        public TestTask(final KeyboardShowTrigger trigger) {
            this.trigger = trigger;
        }

        @Override
        public void run() {
            trigger.startListener();
        }
    }

}
