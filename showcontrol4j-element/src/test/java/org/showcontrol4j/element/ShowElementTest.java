package org.showcontrol4j.element;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.impl.AMQImpl;
import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.exchange.MessageExchange;
import org.showcontrol4j.message.Instruction;
import org.showcontrol4j.message.SCFJMessage;
import org.showcontrol4j.message.ShowCommand;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
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
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

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
        };

        showElement.init();

        exit.expectSystemExitWithStatus(0);
        executor.submit(new TestTask(showElement, testShutdownSCFJMessage));
        TimeUnit.MILLISECONDS.sleep(1000);

        assertTrue(ranShutdownProcedure[0]);

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

    private void setupMockRules() throws IOException, TimeoutException {
        when(mockBrokerConnectionFactory.newConnection()).thenReturn(mockConnection);
        when(mockConnection.createChannel()).thenReturn(mockChannel);
        when(mockChannel.exchangeDeclare(anyString(), anyString())).thenReturn(mockExchangeDeclareOk);
        when(mockChannel.queueDeclare()).thenReturn(mockQueueDeclareOk);
        when(mockQueueDeclareOk.getQueue()).thenReturn("test");
        when(mockChannel.queueBind(anyString(), anyString(), anyString())).thenReturn(mockBindOk);
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
