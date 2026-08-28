# TLS is now terminated by an in-repo reverse proxy — supersedes 0002

Status: supersedes [0002](./0002-tls-terminated-by-external-reverse-proxy.md)

This repo now owns its own nginx service, deployed alongside Keycloak and RabbitMQ, terminating this VPS's real Let's Encrypt certificates directly rather than assuming an operator-managed proxy outside the repo. [0002](./0002-tls-terminated-by-external-reverse-proxy.md)'s premise — that TLS termination and the reverse proxy are owned outside this repo — no longer holds here; a separate, unrelated reverse proxy still exists, but on a different VPS entirely for unrelated services, with no relationship to this repo's Keycloak deployment.

We also use this proxy to enforce a VPN-only boundary: Keycloak's Admin Console gets its own hostname (`KC_HOSTNAME_ADMIN`), served by the proxy only to clients arriving from the WireGuard client subnet (`10.8.0.0/24`, via the already-deployed wg-easy VPN), while the public hostname serves login/token/account traffic to everyone. The proxy has to enforce this itself — Keycloak only uses `KC_HOSTNAME_ADMIN` to generate URLs, it doesn't refuse the `/admin` path based on which hostname the request arrived on — so the public hostname's server block also explicitly blocks `/admin` in addition to the admin hostname's `allow`/`deny` rule.
