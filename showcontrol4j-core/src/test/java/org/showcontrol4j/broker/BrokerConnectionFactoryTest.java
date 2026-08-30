package org.showcontrol4j.broker;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Tests for the {@link BrokerConnectionFactory} class.
 *
 * @author James Hare
 */
public class BrokerConnectionFactoryTest {

  private final String host = "test_host";
  private final int port = 1884;
  private final String user = "test_user";
  private final String password = "test_password";
  private final String clientIdentifier = "test-client";

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
  public void testNewConnection_appliesHostAndIdentifier() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .build();

    final Mqtt3Client client = testBrokerConnectionFactory.newConnection(clientIdentifier);
    final Mqtt3ClientConfig config = client.getConfig();

    assertThat(client, instanceOf(Mqtt3Client.class));
    assertEquals(clientIdentifier, config.getClientIdentifier().orElseThrow().toString());
    assertEquals(host, config.getServerHost());
  }

  @Test
  public void testNewConnection_defaultsToStandardMqttPort() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .build();

    final Mqtt3Client client = testBrokerConnectionFactory.newConnection(clientIdentifier);

    assertEquals(1883, client.getConfig().getServerPort());
  }

  @Test
  public void testNewConnection_appliesCustomPort() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .port(port)
        .build();

    final Mqtt3Client client = testBrokerConnectionFactory.newConnection(clientIdentifier);

    assertEquals(port, client.getConfig().getServerPort());
  }

  @Test
  public void testNewConnection_withoutCredentials_noSimpleAuth() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .build();

    final Mqtt3Client client = testBrokerConnectionFactory.newConnection(clientIdentifier);

    assertTrue(client.getConfig().getSimpleAuth().isEmpty());
  }

  @Test
  public void testNewConnection_appliesCredentials() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .withCredentials(user, password)
        .build();

    final Mqtt3Client client = testBrokerConnectionFactory.newConnection(clientIdentifier);
    final Mqtt3ClientConfig config = client.getConfig();

    assertTrue(config.getSimpleAuth().isPresent());
    assertEquals(user, config.getSimpleAuth().orElseThrow().getUsername().toString());
  }

  @Test
  public void testNewConnection_newInstanceEachCall() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .build();

    final Mqtt3Client first = testBrokerConnectionFactory.newConnection(clientIdentifier);
    final Mqtt3Client second = testBrokerConnectionFactory.newConnection(clientIdentifier);

    assertThat(first, instanceOf(Mqtt3Client.class));
    assertThat(second, instanceOf(Mqtt3Client.class));
  }

  @Test
  public void testNewConnection_withoutTls_noSslConfig() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .build();

    final Mqtt3Client client = testBrokerConnectionFactory.newConnection(clientIdentifier);

    assertTrue(client.getConfig().getSslConfig().isEmpty());
  }

  @Test
  public void testNewConnection_tlsWithDefaultConfig_appliesSslConfig() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .tls()
        .build();

    final Mqtt3Client client = testBrokerConnectionFactory.newConnection(clientIdentifier);

    assertTrue(client.getConfig().getSslConfig().isPresent());
  }

  @Test
  public void testNewConnection_tlsWithTrustedCertificate_appliesSslConfig() throws Exception {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .tls(testCertificateFile())
        .build();

    final Mqtt3Client client = testBrokerConnectionFactory.newConnection(clientIdentifier);
    final Mqtt3ClientConfig config = client.getConfig();

    assertTrue(config.getSslConfig().isPresent());
    assertTrue(config.getSslConfig().orElseThrow().getTrustManagerFactory().isPresent());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNewConnection_tlsWithMissingCertificateFile_throwsIllegalArgumentException() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .tls(new File("does-not-exist.pem"))
        .build();

    testBrokerConnectionFactory.newConnection(clientIdentifier);
  }

  @Test
  public void testNewConnection_automaticReconnectEnabled() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .build();

    final Mqtt3Client client = testBrokerConnectionFactory.newConnection(clientIdentifier);

    assertTrue(client.getConfig().getAutomaticReconnect().isPresent());
  }

  @Test
  public void testNewConnectionWithListener_automaticReconnectEnabled() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .build();

    final Mqtt3Client client = testBrokerConnectionFactory.newConnection(clientIdentifier, context -> { });

    assertTrue(client.getConfig().getAutomaticReconnect().isPresent());
  }

  @Test
  public void testNewConnectionWithListener_registersDisconnectedListener() {
    final BrokerConnectionFactory testBrokerConnectionFactory = new BrokerConnectionFactory.Builder()
        .host(host)
        .build();
    final MqttClientDisconnectedListener disconnectedListener = context -> { };

    final Mqtt3Client client = testBrokerConnectionFactory.newConnection(clientIdentifier, disconnectedListener);

    assertTrue(client.getConfig().getDisconnectedListeners().contains(disconnectedListener));
  }

  //------------------------------------ HELPER METHODS ------------------------------------//

  private File testCertificateFile() throws URISyntaxException {
    final URL certificateUrl = getClass().getClassLoader().getResource("test-broker-ca.pem");
    assertThat(certificateUrl, org.hamcrest.CoreMatchers.notNullValue());
    return new File(certificateUrl.toURI());
  }

}
