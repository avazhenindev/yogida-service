# Meditation Service

Spring Boot service for the Yogida meditation backend.

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

Build the container image locally:

```bash
docker build -t meditation-service:local .
```

Run the container with a local environment file:

```bash
cp .env.example .env.local
# Edit .env.local before running.
docker run --rm --env-file .env.local -p 8080:8080 meditation-service:local
```

Health endpoint after startup:

```bash
curl http://localhost:8080/api/actuator/health
```

## CI/CD approach

This repository uses GitHub Actions with an immutable Docker image deployment flow:

1. `CI` workflow runs `./mvnw verify` on pull requests and pushes to `main`.
2. `Deploy` workflow runs tests, builds a Docker image, and pushes it to GitHub Container Registry (GHCR).
3. The deploy job connects to the Linode/Akamai host by SSH.
4. The host pulls the exact commit-tagged image and restarts the service with Docker Compose.

This keeps production secrets on the host, avoids copying JARs manually, and enables rollback by redeploying a previous image tag.

## Required GitHub configuration

Create a GitHub Environment named `production` and add an approval rule if desired.

Repository or environment secrets:

| Secret | Purpose |
| --- | --- |
| `LINODE_HOST` | Hostname or IP address of the Linode/Akamai VM. |
| `LINODE_USER` | SSH deploy user on the VM. |
| `LINODE_SSH_KEY` | Private SSH key for the deploy user. |
| `LINODE_KNOWN_HOSTS` | SSH known_hosts entry for the VM. Generate with `ssh-keyscan`. |

Optional repository/environment variable:

| Variable | Default | Purpose |
| --- | --- | --- |
| `LINODE_DEPLOY_PATH` | `/home/yogida/meditation-service` | Directory on the VM that contains `docker-compose.yml`, `.env`, and `.deployment.env`. |

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

Generate the host fingerprint for `LINODE_KNOWN_HOSTS`:

```bash
ssh-keyscan -H your-linode-host.example.com
```

The deploy workflow uploads `deploy/docker-compose.prod.yml` as `/home/yogida/meditation-service/docker-compose.yml` automatically.

## Security notes

- Do not commit real database passwords, R2 keys, or tokens.
- Rotate any credentials that were previously committed to the repository history.
- Keep production configuration in `/home/yogida/meditation-service/.env` on the VM.
- Prefer a dedicated `deploy` user with only the permissions needed for Docker deployments.
- Protect the `production` GitHub Environment with manual approval before deploying from `main`.

## Rollback

Images are tagged with the commit SHA. To roll back manually on the VM:

```bash
cd /home/yogida/meditation-service
printf 'APP_IMAGE=%s\n' 'ghcr.io/<owner>/<repo>:<previous-sha>' > .deployment.env
docker compose --env-file .deployment.env -f docker-compose.yml pull app
docker compose --env-file .deployment.env -f docker-compose.yml up -d
```


