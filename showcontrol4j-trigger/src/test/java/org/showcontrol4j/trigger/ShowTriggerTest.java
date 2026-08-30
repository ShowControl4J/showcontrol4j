package org.showcontrol4j.trigger;

import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
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
    private Mqtt3BlockingClient mockBlockingClient;

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
        verify(mockBrokerConnectionFactory, times(1)).newConnection(anyString());
        verify(mockBlockingClient, times(1)).connect();
        assertEquals(name, showTrigger.getName());
        assertEquals(id, showTrigger.getId());
        assertEquals(mockMessageExchange, showTrigger.getMessageExchange());
        assertEquals(mockBrokerConnectionFactory, showTrigger.getBrokerConnectionFactory());
        assertEquals(mockBlockingClient, showTrigger.getClient());
        assertEquals(syncTimeout, showTrigger.getSyncTimeout());
    }

    @Test
    public void testSetClient() throws Exception {
        final ShowTrigger showTrigger = new ShowTrigger(name, id, syncTimeout, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            protected void startListener() {
                // do nothing
            }
        };

        final Mqtt3BlockingClient mockClient2 = mock(Mqtt3BlockingClient.class);
        showTrigger.setClient(mockClient2);
        assertEquals(mockClient2, showTrigger.getClient());
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
        verify(mockBlockingClient.publishWith()).topic("test");
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
        verify(mockBlockingClient.publishWith()).topic("test");
    }

    @Test
    public void testSendShutdownMessage() throws Exception {
        final ShowTrigger showTrigger = new ShowTrigger(name, id, syncTimeout, mockMessageExchange, mockBrokerConnectionFactory) {
            @Override
            protected void startListener() {
                // do nothing
            }
        };

        showTrigger.sendShutdownMessage();
        verify(mockBlockingClient.publishWith()).topic("test");
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

    private void setupMockRules() {
        final Mqtt3Client mockMqttClient = mock(Mqtt3Client.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockBrokerConnectionFactory.newConnection(anyString())).thenReturn(mockMqttClient);
        mockBlockingClient = mockMqttClient.toBlocking();
        when(mockMessageExchange.getName()).thenReturn("test");
    }

}
