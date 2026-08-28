# Propagate user deletion via a shared, repo-owned RabbitMQ broker, not per-app webhooks

Deleting a user's Keycloak account (self-service or admin-initiated) must let every Consuming App purge that user's own stored data. We considered a Keycloak Event Listener SPI extension pushing HTTP webhooks to a per-client URL, with a Postgres-backed retry table for reliability. We rejected it: it inverts this repo's existing shape, where Consuming Apps reach into shared infra rather than infra reaching into them, and re-implements at-least-once delivery and retry/dead-lettering by hand.

Instead, this repo owns a second piece of shared infrastructure — a RabbitMQ **Deletion Broker** — alongside Keycloak. The same Event Listener SPI extension publishes a **User Deletion Event** to a single fanout exchange on every deletion (self-service and admin-initiated are handled identically). Each Consuming App connects with its own credentials, binds its own durable queue, and acknowledges a message only after it has actually purged the user's data — giving the "not removed from the queue until handled" guarantee natively via broker ack semantics, instead of Keycloak polling for confirmation.

We also considered connecting out to each Consuming App's own broker (e.g. the RabbitMQ already used by `gamelist-backend`'s Celery setup) rather than owning one here. Rejected: this repo would then need to hold and manage connection credentials for every app's private broker, which is worse than the webhook model it replaces.

The message contract is a plain AMQP message with a JSON body (`event`, `schema_version`, `realm`, `sub`, `deleted_at`) — not Celery's native task-message protocol. Emitting real Celery tasks from a Java Keycloak extension would couple this repo to Celery's wire format and assume every Consuming App runs Celery, which isn't guaranteed for apps that aren't `gamelist-backend`. Consuming Apps that use Celery internally bridge the plain message into a task themselves.

RabbitMQ terminates its own TLS (AMQPS) rather than relying on the external reverse proxy, since that proxy is HTTP-only (see [0002](./0002-tls-terminated-by-external-reverse-proxy.md)) and does not do raw TCP passthrough.

Update: this AMQPS listener was later removed in favor of binding directly to the VPN interface — see [0007](./0007-rabbitmq-drops-amqps-for-vpn-only-access.md).
