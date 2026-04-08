# SafeVault Backend Server Deployment

## 1. Build Docker Image

```bash
cd safevault-backend
chmod +x scripts/deploy/build-image.sh
./scripts/deploy/build-image.sh safevault-backend latest
```

## 2. Prepare Production Environment File

```bash
cp scripts/deploy/.env.prod.example scripts/deploy/.env.prod
```

Then edit `scripts/deploy/.env.prod` and set strong values for all secrets.

## 3. Deploy to Server

```bash
chmod +x scripts/deploy/deploy-server.sh
./scripts/deploy/deploy-server.sh
```

## 4. Verify

```bash
docker compose --env-file scripts/deploy/.env.prod -f docker-compose.yml ps
curl -k https://<server-ip>:8080/api/swagger-ui.html
```

## 5. Upgrade Workflow

```bash
# build new image
./scripts/deploy/build-image.sh safevault-backend v3.7.1

# update BACKEND_IMAGE in scripts/deploy/.env.prod
# BACKEND_IMAGE=safevault-backend:v3.7.1

# rolling restart with compose
./scripts/deploy/deploy-server.sh
```

## Notes

- This deployment keeps monolith runtime behavior unchanged.
- PostgreSQL and Redis run as sidecar containers in the same compose stack.
- For production hardening, place reverse proxy (Nginx/Caddy) before backend.
- Unified compose file: `docker-compose.yml` is used for production deployment.
- `docker-compose.prod.yml` is retained only as historical compatibility backup.
