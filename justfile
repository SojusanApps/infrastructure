# The keycloak dev stack: production compose file layered with the dev override
keycloak_compose := "docker compose --env-file .env --project-directory . -f keycloak/docker-compose.yml -f keycloak/docker-compose.dev.yml"

# The Deletion Broker (RabbitMQ) dev stack: production compose file layered with the dev override
rabbitmq_compose := "docker compose --env-file .env --project-directory . -f rabbitmq/docker-compose.yml -f rabbitmq/docker-compose.dev.yml"

# The whole production stack: Keycloak, the Deletion Broker (RabbitMQ), and the reverse proxy (nginx) - no dev overrides
prod_compose := "docker compose --env-file .env --project-directory . -f keycloak/docker-compose.yml -f rabbitmq/docker-compose.yml -f nginx/docker-compose.yml"

# Show the list of available commands
help:
    just --list

# Ensure the external Shared Network exists (idempotent) - it's owned outside any one
# stack's lifecycle (see docs/adr/0001-external-shared-network-with-private-db-network.md),
# so `docker compose up` alone won't create it.
shared-network:
    docker network inspect sojusan-apps-shared-network >/dev/null 2>&1 || docker network create sojusan-apps-shared-network

# Start the Postgres database used by Keycloak
db:
    {{ keycloak_compose }} up -d db

# Build the custom Keycloak provider jars (keycloak/extensions) into keycloak/providers
build-providers:
    mvn -q -f keycloak/extensions/unique-attribute-validator/pom.xml clean package
    cp keycloak/extensions/unique-attribute-validator/target/unique-attribute-validator.jar keycloak/providers/
    mvn -q -f keycloak/extensions/deletion-event-publisher/pom.xml clean package
    cp keycloak/extensions/deletion-event-publisher/target/deletion-event-publisher.jar keycloak/providers/

# Start the Deletion Broker (RabbitMQ) that Keycloak publishes user deletion events to
rabbitmq:
    {{ rabbitmq_compose }} up -d rabbitmq

# Start Keycloak in development mode, connected to its own Postgres database and the Deletion Broker
keycloak: build-providers rabbitmq
    {{ keycloak_compose }} up -d keycloak

# Start the SMTP server for development, used by Keycloak to send emails (e.g. for password reset)
smtp4dev:
    {{ keycloak_compose }} up -d smtp4dev

# Start the whole dev stack (database + Deletion Broker + Keycloak)
up: build-providers rabbitmq
    {{ keycloak_compose }} up -d

# Deploy/update the whole production stack in one shot (Keycloak, Deletion Broker, reverse proxy) - run on the VPS
deploy: build-providers shared-network
    {{ prod_compose }} up -d
