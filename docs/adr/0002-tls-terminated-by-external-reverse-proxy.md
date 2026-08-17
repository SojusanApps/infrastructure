# TLS termination is owned by an external reverse proxy, not this compose file

Keycloak runs in production (`start`, not `start-dev`) mode, which requires a real hostname and normally implies TLS. We chose to keep Keycloak plain-HTTP internally (`KC_PROXY_HEADERS=xforwarded`, `KC_HOSTNAME` set to the public domain) and rely on a reverse proxy — managed outside this repo — to terminate TLS and forward to Keycloak over the Shared Network.

The alternative (mounting certs and configuring `KC_HTTPS_CERTIFICATE_FILE`/`KC_HTTPS_CERTIFICATE_KEY_FILE` directly on the Keycloak container) would make this repo own certificate issuance and renewal for every shared service individually, duplicating work the proxy already does once for everything behind it.
