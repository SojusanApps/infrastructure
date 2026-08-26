# Infrastructure

Shared infrastructure that runs once and is used by multiple Sojusan apps — starting with Keycloak as the shared identity provider. This repo owns the lifecycle of shared services; app repos consume them.

## Language

**Consuming App**:
An application, typically deployed on its own separate machine, that depends on a service defined in this repo (e.g. it authenticates against the shared Keycloak realm) purely over the network — via a public hostname, never via Docker networking. It never joins any network this repo defines.
_Avoid_: Client app, tenant

**Realm**:
Keycloak's isolated space containing a set of users, clients, and roles shared across consuming apps (e.g. `SojusanApps`). Owned by this repo, referenced by consuming apps by name.
_Avoid_: Tenant, workspace

**Shared Network**:
The external Docker network (`sojusan-apps-shared-network`) that connects this repo's public-facing services to the Reverse Proxy running on the _same host_. Declared `external: true` here — this repo does not own its creation or lifecycle. Docker networks are host-local, so this only ever joins things co-located on this machine — never Consuming Apps on other machines.
_Avoid_: App network, public network

**Reverse Proxy**:
The TLS-terminating proxy running on the same host as this repo's services, forwarding traffic to them over the Shared Network. Lives outside this repo (this repo only assumes its existence); Consuming Apps reach it over the real network via its public hostname, not via Docker.
_Avoid_: Load balancer, ingress (unless that's literally what's deployed)

**Bootstrap Admin**:
The Keycloak admin account credentials used only to create the first admin user on a fresh database. Irrelevant once a persistent admin exists in the realm.
_Avoid_: Root user, superuser

**Dev Stack / Production Stack**:
The two `docker compose` configurations for a given service — a base file (production behavior: `start` mode, no shortcuts) plus a `docker-compose.dev.yml` override (`start-dev`, local-friendly defaults) layered on top for local work.
_Avoid_: Local environment, staging (staging isn't defined/in scope here)

**Shared Broker**:
The RabbitMQ instance owned by this repo, alongside Keycloak. Its default vhost hosts the deletion broadcast (see User Deletion Event); beyond that, it hosts a Private Vhost per Consuming App that wants broker-backed infrastructure of its own.
_Avoid_: Deletion Broker, message queue, event bus, RabbitMQ instance

**User Deletion Event**:
The message broadcast on the Shared Broker's default vhost when a user's Keycloak account is deleted (self-service or admin-initiated). Delivered at-least-once; a Consuming App acknowledges it only after fully purging its own data for that user.
_Avoid_: Webhook payload

**Private Vhost**:
A Consuming App's own RabbitMQ vhost on the Shared Broker — a fully isolated namespace the app has complete configure/write/read control over, invisible to every other Consuming App and unrelated to the deletion broadcast. The app manages its own exchanges/queues inside it (e.g. Celery's own task queues); this repo declares only the vhost and the app's credentials, never the app's internal queue topology.
_Avoid_: Tenant, namespace, private queue

**Username**:
The unique login identifier for a Keycloak account. Sent to Consuming Apps as the `preferred_username` claim, but not intended for display — see Nickname for the public-facing identity.
_Avoid_: Login, handle

**Nickname**:
The unique, required display name attached to a Keycloak account, separate from Username. Shown publicly to other users across every Consuming App.
_Avoid_: Display name, username, handle
