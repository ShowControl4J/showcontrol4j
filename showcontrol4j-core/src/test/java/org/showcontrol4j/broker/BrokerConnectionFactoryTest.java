package org.showcontrol4j.broker;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for the {@link BrokerConnectionFactory} class.
 *
 * @author James Hare
 */
public class BrokerConnectionFactoryTest {

  private final String host = "test_host";
  private final String user = "test_user";
  private final String password = "test_password";

  @Mock
  ConnectionFactory mockConnectionFactory;
  @Mock
  Connection mockConnection;

  @Before
  public void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testBuilder() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .withCredentials(user, password)
        .build();
    assertThat(testBrokerConnectionFactory, instanceOf(BrokerConnectionFactory.class));
  }

  @Test
  public void testCredentialsSetInConnectionFactory() throws Exception {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .withCredentials(user, password)
        .build();

    Field factoryField = testBrokerConnectionFactory.getClass().getDeclaredField("connectionFactory");
    factoryField.setAccessible(true);
    ConnectionFactory connectionFactory = (ConnectionFactory) factoryField.get(testBrokerConnectionFactory);

    assertEquals(user, connectionFactory.getUsername());
    assertEquals(password, connectionFactory.getPassword());
  }

  @Test
  public void testNewConnection() throws Exception {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .withCredentials(user, password)
        .build();

    Field connectionFactoryField = testBrokerConnectionFactory.getClass().getDeclaredField("connectionFactory");
    connectionFactoryField.setAccessible(true);
    connectionFactoryField.set(testBrokerConnectionFactory, mockConnectionFactory);

    when(mockConnectionFactory.newConnection()).thenReturn(mockConnection);
    Connection testConnection = testBrokerConnectionFactory.newConnection();

    assertThat(testConnection, instanceOf(Connection.class));
  }

}
