#!/bin/sh
# Renders definitions.json from its template by substituting user passwords sourced from
# .env, since RabbitMQ's definitions loader (unlike Keycloak's realm import) has no built-in
# ${VAR} substitution. Then hands off to the image's own entrypoint.
set -e

awk \
  -v admin_pass="$RABBITMQ_ADMIN_PASSWORD" \
  -v kc_pass="$RABBITMQ_KEYCLOAK_PUBLISHER_PASSWORD" \
  -v gl_pass="$RABBITMQ_GAMELIST_BACKEND_PASSWORD" \
  '{
    gsub(/__RABBITMQ_ADMIN_PASSWORD__/, admin_pass)
    gsub(/__RABBITMQ_KEYCLOAK_PUBLISHER_PASSWORD__/, kc_pass)
    gsub(/__RABBITMQ_GAMELIST_BACKEND_PASSWORD__/, gl_pass)
    print
  }' /etc/rabbitmq/definitions.json.template > /etc/rabbitmq/definitions.json

chmod 644 /etc/rabbitmq/definitions.json

exec docker-entrypoint.sh "$@"
