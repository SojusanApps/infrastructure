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
