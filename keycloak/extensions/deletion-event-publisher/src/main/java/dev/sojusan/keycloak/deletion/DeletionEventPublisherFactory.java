package dev.sojusan.keycloak.deletion;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Publishes a User Deletion Event to the shared Deletion Broker whenever a user's Keycloak
 * account is deleted, self-service or admin-initiated. See
 * docs/adr/0003-shared-rabbitmq-broker-for-user-deletion-propagation.md.
 */
public class DeletionEventPublisherFactory implements EventListenerProviderFactory {

    public static final String ID = "deletion-event-publisher";

    private static final Logger log = Logger.getLogger(DeletionEventPublisherFactory.class);

    private String exchange;
    private Connection connection;
    private Channel channel;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new DeletionEventPublisher(session, channel, exchange);
    }

    @Override
    public void init(Config.Scope config) {
        exchange = config.get("exchange", "user-deletion");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.get("host", "localhost"));
        factory.setPort(config.getInt("port", 5672));
        factory.setVirtualHost(config.get("virtualHost", "/"));
        factory.setUsername(config.get("username"));
        factory.setPassword(config.get("password"));
        factory.setAutomaticRecoveryEnabled(true);

        try {
            connection = factory.newConnection("keycloak-deletion-event-publisher");
            channel = connection.createChannel();
            channel.confirmSelect();
        } catch (Exception e) {
            // Deletion is not blocked on the Deletion Broker being reachable (see the ADR) - log
            // and keep Keycloak usable; onEvent() below no-ops with an error log while channel is null.
            log.error("Failed to connect to the Deletion Broker; user deletion events will not be published", e);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No cross-factory wiring needed.
    }

    @Override
    public void close() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("Error closing Deletion Broker connection", e);
        }
    }
}
