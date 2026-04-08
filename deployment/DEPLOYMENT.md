# Deployment Guide

## Goal

Expose the API publicly at:

- `https://eru-api.dk/api/v1`

Expected health endpoint:

- `https://eru-api.dk/api/v1/health`

## Files

- [docker-compose.deploy.yml](/D:/TheFileSTATIONÆR/eru/deployment/docker-compose.deploy.yml)
- [.env.deploy.example](/D:/TheFileSTATIONÆR/eru/deployment/.env.deploy.example)
- [Caddyfile.example](/D:/TheFileSTATIONÆR/eru/deployment/Caddyfile.example)

## 1. DNS

Point `eru-api.dk` to your server's public IP with an `A` record.

If you also want `www.eru-api.dk`, add another `A` record or a `CNAME`.

## 2. Prepare Server

Install:

- Docker
- Docker Compose plugin
- Caddy or another reverse proxy

## 3. Prepare Environment

On the server:

1. Copy `.env.deploy.example` to `.env`
2. Replace placeholder values

Required values:

- `DOCKERHUB_IMAGE`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `CONNECTION_STR`
- `JWT_SECRET`

Recommended database connection string:

```env
CONNECTION_STR=jdbc:postgresql://postgres:5432/
```

## 4. Start Containers

Run:

```bash
docker compose --env-file .env -f deployment/docker-compose.deploy.yml up -d
```

This starts:

- PostgreSQL
- The ERU API container on `localhost:8080`

## 5. Reverse Proxy

If you use Caddy, start with:

```caddy
eru-api.dk {
    reverse_proxy localhost:8080
}
```

That is also available in [Caddyfile.example](/D:/TheFileSTATIONÆR/eru/deployment/Caddyfile.example).

## 6. Verify

Test locally on the server:

```bash
curl http://localhost:8080/api/v1/health
```

Test publicly:

```bash
curl https://eru-api.dk/api/v1/health
```

## 7. JWT Verification

Once the domain responds, test auth with the HTTP files in [src/main/resources/http](/D:/TheFileSTATIONÆR/eru/src/main/resources/http).

Recommended order:

1. `auth.http` login
2. `auth.http` me
3. `content.http` protected create request
