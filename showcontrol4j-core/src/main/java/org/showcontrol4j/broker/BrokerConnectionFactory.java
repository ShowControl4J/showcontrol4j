package org.showcontrol4j.broker;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Serves as a wrapper for the {@link ConnectionFactory} RabbitMQ object. Lombok was not used for
 * this class because we need to exclude the connectionFactory member and initialize it in the constructor.
 *
 * @author James Hare
 */
public class BrokerConnectionFactory {

  private final ConnectionFactory connectionFactory;

  private BrokerConnectionFactory(final Builder builder) {
    connectionFactory = new ConnectionFactory();
    connectionFactory.setHost(builder.host);
    connectionFactory.setUsername(builder.user);
    connectionFactory.setPassword(builder.password);
  }

  /**
   * Returns a new connection from the Broker Connection Factory.
   *
   * @return {@link Connection} a new connection from the Broker Connection Factory.
   * @throws IOException
   * @throws TimeoutException
   */
  public Connection newConnection() throws IOException, TimeoutException {
    return connectionFactory.newConnection();
  }

  /**
   * Serves as a static builder class to build a {@link BrokerConnectionFactory} object.
   */
  public static class Builder {

    private String host;
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
