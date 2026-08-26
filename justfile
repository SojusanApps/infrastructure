# The keycloak dev stack: production compose file layered with the dev override
keycloak_compose := "docker compose --env-file .env --project-directory . -f keycloak/docker-compose.yml -f keycloak/docker-compose.dev.yml"

# The Deletion Broker (RabbitMQ) dev stack: production compose file layered with the dev override
rabbitmq_compose := "docker compose --env-file .env --project-directory . -f rabbitmq/docker-compose.yml -f rabbitmq/docker-compose.dev.yml"

# Show the list of available commands
help:
    just --list

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
