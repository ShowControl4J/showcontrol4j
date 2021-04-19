package org.showcontrol4j.trigger;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.impl.AMQImpl;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.showcontrol4j.broker.BrokerConnectionFactory;
import org.showcontrol4j.exchange.MessageExchange;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for the {@link ShowTrigger} class.
 *
 * @author James Hare
 */
public class ShowTriggerTest {

    private final String name = "Test Trigger Name";
    private final Long id = 123456L;
    private final Long syncTimeout = 5000L;

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
        setupMockRules();
    }

    @Test
    public void testConstructor() throws Exception {
        final ShowTrigger showTrigger = new ShowTrigger(name, id, syncTimeout, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            protected void startListener() {
                // do nothing
            }
        };

        assertThat(showTrigger, CoreMatchers.instanceOf(ShowTrigger.class));
        verify(mockBrokerConnectionFactory, times(1)).newConnection();
        verify(mockChannel, times(1)).exchangeDeclare("test", "fanout");
        verify(mockMessageExchange, times(1)).getName();
        assertEquals(name, showTrigger.getName());
        assertEquals(id, showTrigger.getId());
        assertEquals(mockMessageExchange, showTrigger.getMessageExchange());
        assertEquals(mockBrokerConnectionFactory, showTrigger.getBrokerConnectionFactory());
        assertEquals(mockChannel, showTrigger.getChannel());
        assertEquals(syncTimeout, showTrigger.getSyncTimeout());
    }

    @Test
    public void testSetName() throws Exception {
        final ShowTrigger showTrigger = new ShowTrigger(name, id, syncTimeout, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            protected void startListener() {
                // do nothing
            }
        };

        final Channel mockChannel2 = mock(Channel.class);
        showTrigger.setChannel(mockChannel2);
        assertEquals(mockChannel2, showTrigger.getChannel());
    }

    @Test
    public void testSendGoMessage() throws Exception {
        final ShowTrigger showTrigger = new ShowTrigger(name, id, syncTimeout, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            protected void startListener() {
                // do nothing
            }
        };

        showTrigger.sendGoMessage();
        verify(mockChannel, times(1)).basicPublish(eq("test"), eq(""), eq(null), any());
    }

    @Test
    public void testSendIdleMessage() throws Exception {
        final ShowTrigger showTrigger = new ShowTrigger(name, id, syncTimeout, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            protected void startListener() {
                // do nothing
            }
        };

        showTrigger.sendIdleMessage();
        verify(mockChannel, times(1)).basicPublish(eq("test"), eq(""), eq(null), any());
    }

    @Test
    public void testSendStopMessage() throws Exception {
        final ShowTrigger showTrigger = new ShowTrigger(name, id, syncTimeout, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            protected void startListener() {
                // do nothing
            }
        };

        showTrigger.sendStopMessage();
        verify(mockChannel, times(1)).basicPublish(eq("test"), eq(""), eq(null), any());
    }

    @Test
    public void testToString() throws Exception {
        final ShowTrigger showTrigger = new ShowTrigger(name, id, syncTimeout, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            protected void startListener() {
                // do nothing
            }
        };

        final String expected = "ShowTrigger(name=Test Trigger Name, id=123456, syncTimeout=5000)";
        assertEquals(expected, showTrigger.toString());
    }

    //------------------------------------ HELPER METHODS ------------------------------------//

    private void setupMockRules() throws Exception {
        when(mockBrokerConnectionFactory.newConnection()).thenReturn(mockConnection);
        when(mockConnection.createChannel()).thenReturn(mockChannel);
        when(mockChannel.exchangeDeclare(anyString(), anyString())).thenReturn(mockExchangeDeclareOk);
        when(mockChannel.queueDeclare()).thenReturn(mockQueueDeclareOk);
        when(mockMessageExchange.getName()).thenReturn("test");
    }

}