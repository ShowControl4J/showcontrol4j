package org.showcontrol4j.broker;

import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;

import java.nio.charset.StandardCharsets;

/**
 * Serves as a wrapper for building {@link Mqtt3Client} MQTT client instances against a configured
 * broker host/port and, optionally, credentials. Lombok was not used for this class because we need
 * to exclude the stored configuration from a generated constructor and validate it ourselves.
 *
 * @author James Hare
 */
public class BrokerConnectionFactory {

  private final String host;
  private final int port;
  private final String user;
  private final String password;

  private BrokerConnectionFactory(final Builder builder) {
    host = builder.host;
    port = builder.port;
    user = builder.user;
    password = builder.password;
  }

  /**
   * Builds a new, not-yet-connected {@link Mqtt3Client} for the given client identifier. Callers are
   * responsible for connecting it (via {@code toBlocking().connect()} or {@code toAsync().connect()})
   * before publishing or subscribing.
   *
   * @param clientIdentifier a unique MQTT client identifier for this connection - two simultaneous
   *                          connections to the same broker with the same identifier will conflict.
   * @return a new {@link Mqtt3Client} configured with this factory's broker host/port and credentials.
   */
  public Mqtt3Client newConnection(final String clientIdentifier) {
    final Mqtt3ClientBuilder clientBuilder = Mqtt3Client.builder()
            .identifier(clientIdentifier)
            .serverHost(host)
            .serverPort(port);
    if (user != null) {
      clientBuilder.simpleAuth()
              .username(user)
              .password(password.getBytes(StandardCharsets.UTF_8))
              .applySimpleAuth();
    }
    return clientBuilder.build();
  }

  /**
   * Serves as a static builder class to build a {@link BrokerConnectionFactory} object.
   */
  public static class Builder {

    private static final int DEFAULT_PORT = 1883;

    private String host;
    private int port = DEFAULT_PORT;
    private String user;
    private String password;

    /**
     * Constructor
     */
    public Builder() {
    }

    /**
     * Sets the hostname of the {@link BrokerConnectionFactory}.
     *
     * @param host the hostname of the {@link BrokerConnectionFactory}.
     * @return the Builder object.
     */
    public Builder host(final String host) {
      this.host = host;
      return this;
    }

    /**
     * Sets the port of the {@link BrokerConnectionFactory}. Defaults to 1883, the standard
     * unencrypted MQTT port, if not set.
     *
     * @param port the broker port to connect to.
     * @return the Builder object.
     */
    public Builder port(final int port) {
      this.port = port;
      return this;
    }

    /**
     * Sets the credentials used to make a connection to the broker, if necessary.
     *
     * @param user the username used to make a connection to the broker.
     * @param password the password used to make a connection to the broker.
     * @return the Builder object.
     */
    public Builder withCredentials(final String user, final String password) {
      this.user = user;
      this.password = password;
      return this;
    }

    /**
     * Builds the {@link BrokerConnectionFactory} object with the builder.
     *
     * @return the {@link BrokerConnectionFactory} object.
     */
    public BrokerConnectionFactory build() {
      return new BrokerConnectionFactory(this);
    }

  }

}
