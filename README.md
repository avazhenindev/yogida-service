# Meditation Service

Spring Boot service for the Yogida meditation backend.

## Project Overview

Meditation Service is the backend API for the Yogida platform, providing secure, scalable endpoints for meditation content, user management, and media storage. It powers the mobile and admin applications, handling authentication, media uploads, and business logic.

## Features

- RESTful API for meditation content and categories
- User authentication and authorization
- Media upload and storage (Cloudflare R2)
- Health and metrics endpoints
- CI/CD with GitHub Actions and Docker Compose deployment
- Secure environment and credential management

## Local validation

Run the Maven test/build lifecycle with a reachable PostgreSQL database. The command below assumes a local database named `meditation` and uses test-safe R2 placeholders:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/meditation \
SPRING_DATASOURCE_USERNAME=backend \
SPRING_DATASOURCE_PASSWORD=change-me \
CLOUDFLARE_R2_ACCOUNT_ID=test-account \
CLOUDFLARE_R2_ACCESS_KEY_ID=test-access-key \
CLOUDFLARE_R2_SECRET_ACCESS_KEY=test-secret-key \
CLOUDFLARE_R2_BUCKET=test-bucket \
./mvnw -B verify
```

Build the executable JAR locally:

```bash
./mvnw -B -DskipTests package
```

Run the JAR locally with an environment file:

```bash
cp .env.example .env.local
# Edit .env.local before running.
set -a
source .env.local
set +a
java -jar target/meditation-service-0.0.1-SNAPSHOT.jar
```

Health endpoint after startup:

```bash
curl http://localhost:8080/api/actuator/health
```

## CI/CD approach

This repository uses a simple GitHub Actions JAR-copy deployment flow:

1. `CI` workflow runs `./mvnw verify` on pull requests and pushes to `main`.
2. `Deploy` workflow builds the Spring Boot executable JAR.
3. The deploy job connects to the Linode/Akamai host by SSH.
4. The deploy job copies `app.jar` and `docker-compose.yml` to the host.
5. The host restarts the service with Docker Compose using a stock Java 21 runtime image.

This keeps production secrets on the host and avoids a custom application Docker image or container registry.

## Required GitHub configuration

Create a GitHub Environment named `production` and add an approval rule if desired.

Repository or environment secrets:

| Secret | Purpose |
| --- | --- |
| `LINODE_HOST` | Hostname or IP address of the Linode/Akamai VM. |
| `LINODE_USER` | SSH deploy user on the VM. |
| `LINODE_SSH_KEY` | Private SSH key for the deploy user. |

Optional repository/environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `LINODE_DEPLOY_PATH` | `/home/yogida/meditation-service` | Directory on the VM that contains `docker-compose.yml`, `.env`, `app.jar`, and `app.jar.previous`. |

## First-time Linode/Akamai host setup

Install Docker and the Compose plugin on the VM, then prepare the deployment directory:

```bash
mkdir -p /home/yogida/meditation-service
```

If the directory is created by another user, make sure the SSH deploy user owns it:

```bash
sudo chown -R yogida:yogida /home/yogida/meditation-service
```

Create the production environment file on the VM only:

```bash
cp .env.example /home/yogida/meditation-service/.env
nano /home/yogida/meditation-service/.env
```


The deploy workflow uploads `deploy/docker-compose.prod.yml` as `/home/yogida/meditation-service/docker-compose.yml` and the built Spring Boot JAR as `/home/yogida/meditation-service/app.jar` automatically.

## Security notes

- Do not commit real database passwords, R2 keys, or tokens.
- Rotate any credentials that were previously committed to the repository history.
- Keep production configuration in `/home/yogida/meditation-service/.env` on the VM.
- Prefer a dedicated `deploy` user with only the permissions needed for Docker deployments.
- Protect the `production` GitHub Environment with manual approval before deploying from `main`.

## Rollback

The deploy workflow keeps one previous JAR as `app.jar.previous`. To roll back manually on the VM:

```bash
cd /home/yogida/meditation-service
cp app.jar app.jar.failed
cp app.jar.previous app.jar
docker compose -f docker-compose.yml up -d --force-recreate --remove-orphans
```

## Contribution Guidelines

1. Fork the repository and create your feature branch.
2. Write clear, well-documented code and tests.
3. Run all tests locally before submitting a pull request.
4. Follow the existing code style and conventions.
5. Submit a pull request with a clear description of your changes.


