# sharingbridge-notification-service

> FCM push on connection-ready webhooks — **Spring Boot 3 / Java 21**

## Status

**Current runtime:** Spring Boot (Docker on Render).  
**Legacy:** Node.js MVP under [`legacy-node/`](./legacy-node/) for rollback reference.

| Method | Path | Notes |
|--------|------|--------|
| GET | `/health` | Render health check |
| POST | `/internal/connection-ready` | Header `X-Webhook-Secret` when `WEBHOOK_SECRET` set |

## Run locally

Requires **JDK 21** + Maven.

```bash
cp .env.example .env
# Export vars into the shell (Spring does not load .env automatically), then:
mvn test
mvn spring-boot:run
```

Health: `GET http://localhost:8093/health`

## Environment

See [environment-variables.md](https://github.com/sharingbridge/sharingbridge/blob/main/configuration/environment-variables.md#sharingbridge-notification-service).

Shared DB knobs (same names as user-service): `DB_POOL_*`, `DB_RETRY_*`, `DB_SUPABASE_POOL_6543_4TR_5432_4SESN` (`5432` \| `6543`).

## Deploy (Render)

`runtime: docker` — see `Dockerfile` and `render.yaml`. Clear any leftover Node build/start commands.

Integration-service:

```text
CONNECTION_NOTIFY_WEBHOOK_URL=https://<notification-host>/internal/connection-ready
CONNECTION_NOTIFY_WEBHOOK_SECRET=<same as WEBHOOK_SECRET>
```

## Payload

```json
{
  "type": "connection_ready",
  "order_code": "SB-7K2M-9F3",
  "recipient_user_ids": ["user-id-1"],
  "recipient_emails": ["user@example.com"],
  "subject": "SharingBridge — order SB-7K2M-9F3 connection ready",
  "text": "Order SB-7K2M-9F3 — a connection is ready..."
}
```

Part of [SharingBridge](https://github.com/sharingbridge/sharingbridge).
