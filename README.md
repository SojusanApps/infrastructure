# infrastructure

The infrastructure shared between the Sojusan apps — currently Keycloak, the identity
provider used for authentication across all Sojusan Apps applications. Consuming apps
authenticate against the shared `SojusanApps` realm over the network rather than joining
any Docker network this repo defines.

Also included is a custom Keycloak theme (login, account console, and email) matching the
Sojusan Apps branding, and a small custom extension enforcing case-insensitive uniqueness
on the `nickname` user profile attribute.

The second piece of shared infrastructure is the Shared Broker (RabbitMQ). On its default
vhost, whenever a user's Keycloak account is deleted (self-service or admin-initiated), a
Keycloak Event Listener extension broadcasts a User Deletion Event so Consuming Apps can
purge their own stored data for that user — see
[docs/adr/0003-shared-rabbitmq-broker-for-user-deletion-propagation.md](docs/adr/0003-shared-rabbitmq-broker-for-user-deletion-propagation.md).
Beyond that, a Consuming App that wants broker-backed infrastructure of its own (e.g. Celery
task queues) gets its own Private Vhost — a fully isolated namespace it self-manages, invisible
to every other Consuming App — see
[docs/adr/0004-shared-broker-with-private-vhost-per-app.md](docs/adr/0004-shared-broker-with-private-vhost-per-app.md).

## Dev tools

For local development, the dev stack additionally brings up:

- **smtp4dev** — a fake SMTP server so Keycloak can send emails (verification, password
  reset) locally without touching a real mail provider. Web UI at `localhost:5050`.
- **A Postgres database dedicated to Keycloak** — used only by Keycloak itself, not shared
  with any consuming app's own database.

See the `justfile` for the available commands to bring the stack up.

Note: Keycloak's realm import only applies to a *fresh* database - it silently skips realms
that already exist. If you change `keycloak/imports/realms/*.json` (e.g. `eventsListeners`)
against a database that already has that realm, apply the change directly via the Admin
Console/API against the running realm, or start from a fresh database.
