# The keycloak dev stack: production compose file layered with the dev override

compose := "docker compose --env-file .env --project-directory . -f keycloak/docker-compose.yml -f keycloak/docker-compose.dev.yml"

# Show the list of available commands
help:
    just --list

# Start the Postgres database used by Keycloak
db:
    {{ compose }} up -d db

# Build the custom Keycloak provider jars (keycloak/extensions) into keycloak/providers
build-providers:
    mvn -q -f keycloak/extensions/unique-attribute-validator/pom.xml clean package
    cp keycloak/extensions/unique-attribute-validator/target/unique-attribute-validator.jar keycloak/providers/

# Start Keycloak in development mode, connected to its own Postgres database
keycloak: build-providers
    {{ compose }} up -d keycloak

# Start the SMTP server for development, used by Keycloak to send emails (e.g. for password reset)
smtp4dev:
    {{ compose }} up -d smtp4dev

# Start the whole dev stack (database + Keycloak)
up: build-providers
    {{ compose }} up -d
