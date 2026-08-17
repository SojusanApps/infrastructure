# infrastructure

The infrastructure shared between the Sojusan apps — currently Keycloak, the identity
provider used for authentication across all Sojusan Apps applications. Consuming apps
authenticate against the shared `SojusanApps` realm over the network rather than joining
any Docker network this repo defines.

Also included is a custom Keycloak theme (login, account console, and email) matching the
Sojusan Apps branding, and a small custom extension enforcing case-insensitive uniqueness
on the `nickname` user profile attribute.

## Dev tools

For local development, the dev stack additionally brings up:

- **smtp4dev** — a fake SMTP server so Keycloak can send emails (verification, password
  reset) locally without touching a real mail provider. Web UI at `localhost:5050`.
- **A Postgres database dedicated to Keycloak** — used only by Keycloak itself, not shared
  with any consuming app's own database.

See the `justfile` for the available commands to bring the stack up.
