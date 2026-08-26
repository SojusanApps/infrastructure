package dev.sojusan.keycloak.deletion;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

/**
 * Fires on the two events that mean "this user no longer exists in Keycloak" - self-service
 * account deletion and an admin deleting a user - and publishes an identical User Deletion Event
 * for both onto the shared Deletion Broker's fanout exchange.
 */
public class DeletionEventPublisher implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(DeletionEventPublisher.class);

    // Admin event resource path for deleting the user itself, e.g. "users/1b4d1595-...".
    // Sub-resource deletions (role mappings, consents, ...) carry a different ResourceType.
    private static final Pattern USER_RESOURCE_PATH = Pattern.compile("^users/([^/]+)$");

    private final KeycloakSession session;
    private final Channel channel;
    private final String exchange;

    public DeletionEventPublisher(KeycloakSession session, Channel channel, String exchange) {
        this.session = session;
        this.channel = channel;
        this.exchange = exchange;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() != EventType.DELETE_ACCOUNT) {
            return;
        }
        publish(event.getRealmId(), event.getUserId());
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (event.getOperationType() != OperationType.DELETE || event.getResourceType() != ResourceType.USER) {
            return;
        }

        Matcher matcher = USER_RESOURCE_PATH.matcher(event.getResourcePath());
        if (!matcher.matches()) {
            return;
        }

        publish(event.getRealmId(), matcher.group(1));
    }

    private synchronized void publish(String realmId, String userId) {
        if (channel == null) {
            log.errorf("Deletion Broker channel unavailable; dropped deletion event for user %s in realm %s", userId, realmId);
            return;
        }

        String realmName = resolveRealmName(realmId);
        String body = String.format(
                "{\"event\":\"user.deleted\",\"schema_version\":1,\"realm\":\"%s\",\"sub\":\"%s\",\"deleted_at\":\"%s\"}",
                escape(realmName), escape(userId), Instant.now());

        try {
            channel.basicPublish(exchange, "", persistentJson(), body.getBytes(StandardCharsets.UTF_8));
            if (!channel.waitForConfirms(5000)) {
                log.errorf("Deletion Broker did not confirm delivery for user %s in realm %s", userId, realmName);
            }
        } catch (Exception e) {
            log.errorf(e, "Failed to publish deletion event for user %s in realm %s", userId, realmName);
        }
    }

    private String resolveRealmName(String realmId) {
        var realm = session.realms().getRealm(realmId);
        return realm != null ? realm.getName() : realmId;
    }

    private static AMQP.BasicProperties persistentJson() {
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .deliveryMode(2)
                .build();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public void close() {
        // Channel/connection lifecycle is owned by DeletionEventPublisherFactory, shared across events.
    }
}
