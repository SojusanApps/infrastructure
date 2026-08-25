# Consolidate onto one Shared Broker with a Private Vhost per app, not one broker per app

`gamelist-backend` ran its own standalone RabbitMQ instance in its own repo, for its own Celery queues, separate from the broker introduced in [0003](./0003-shared-rabbitmq-broker-for-user-deletion-propagation.md) for deletion propagation. We consolidated both onto a single Shared Broker instance owned by this repo, rather than keeping one broker per Consuming App.

Each Consuming App that needs broker-backed infrastructure of its own gets a dedicated RabbitMQ vhost — a fully separate namespace, not just permission-scoped resource names on a shared vhost. That matters: an app's internal queues need to be genuinely invisible to every other Consuming App, not merely inaccessible by regex. The deletion broadcast (the fanout exchange Keycloak publishes to, and each app's deletion queue) stays on its own vhost, untouched by this change, so Keycloak's publisher credentials never need reaching into any app's private vhost.

Within its own Private Vhost, an app gets full configure/write/read rights and self-manages its own topology — e.g. Celery auto-declaring its `celery` exchange/queue and pidbox reply queues on worker startup, the same way it always has. This repo declares only the vhost and the app's credentials, not its internal queue names, so adding an internal task queue doesn't require a PR here.

This is now the standing pattern for any future Consuming App wanting broker-backed infrastructure: one Shared Broker, one Private Vhost per app, plus the single shared vhost for the deletion broadcast that only Keycloak publishes to.
