# Local dev broker setup

A local Mosquitto broker for developing against, via Docker Compose. It requires
authentication on every listener and offers both a plaintext port (1883) and a
TLS port (8883) - see `.config/mosquitto/mosquitto.conf` for the full config.

This is a **local development setup only**. The certificate is self-signed and
the credentials are whatever you choose below - none of this is suitable for
anything beyond a single trusted machine or home network. See `ROADMAP.md`,
Phase 1, "Mosquitto secured with auth + TLS from day one" for the reasoning.

## One-time setup

Both steps below must be done once, before the first `docker compose up` -
Mosquitto expects the password file and certificates to already exist.

**1. Generate a password**, replacing `<username>` and `<password>`:

```
docker run --rm -v "$(pwd)/.config/mosquitto:/mosquitto/config" eclipse-mosquitto \
    mosquitto_passwd -b -c /mosquitto/config/mosquitto.passwd <username> <password>
```

Run again without `-c` to add additional users to the same file.

**2. Generate a dev certificate** (defaults to `localhost` - pass a hostname/IP
as an argument if show elements/triggers will reach the broker some other way):

```
./generate-dev-certs.sh
```

## Running the broker

```
docker compose up -d
```

## Connecting to it

```java
new BrokerConnectionFactory.Builder()
        .host("localhost")
        .port(8883)
        .withCredentials("<username>", "<password>")
        .tls(new File("setup/.certs/ca.crt"))
        .build();
```

Drop `.port(8883)` and `.tls(...)` to use the plaintext 1883 listener instead -
still requires the same credentials, since `allow_anonymous` is off on both.
