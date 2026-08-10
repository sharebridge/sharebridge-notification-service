# sharingbridge-notification-service

> FCM push and transactional notification webhooks for SharingBridge.

## Runtime plan

**Today:** Node.js 20.  
**Next rewrite:** **Spring Boot** (same `POST /internal/connection-ready` + `GET /health` contracts; Docker on Render). Tracked in [STATUS.md](https://github.com/sharingbridge/sharingbridge/blob/main/development/STATUS.md).

**DB access:** Adopt the shared `DB_POOL_*` / `DB_RETRY_*` env contract in the Spring rewrite (reference implementation: user-service / Npgsql) — [Database client pool & retry (standard)](https://github.com/sharingbridge/sharingbridge/blob/main/configuration/environment-variables.md#database-client-pool--retry-standard). Prefer Supabase **session** pooler (`:5432`) for long-lived JVM processes.

## Overview

Receives **connection-ready** events from `sharingbridge-integration-service` and sends **FCM push** to registered device tokens.

**Route:** `POST /internal/connection-ready`

**Health:** `GET /health`

## Environment

Deploy on Render: [environment-variables.md](https://github.com/sharingbridge/sharingbridge/blob/main/configuration/environment-variables.md#sharingbridge-notification-service). **Step-by-step:** [notification-service-local.md](https://github.com/sharingbridge/sharingbridge/blob/main/configuration/notification-service-local.md) · [backend-render.md](https://github.com/sharingbridge/sharingbridge/blob/main/configuration/backend-render.md) § Notification service.

| Variable | Purpose |
|----------|---------|
| `DATABASE_URL` | Same Supabase Postgres as other services (`device_tokens` table) |
| `WEBHOOK_SECRET` | Must match integration-service `CONNECTION_NOTIFY_WEBHOOK_SECRET` |
| `FIREBASE_SERVICE_ACCOUNT_PATH` or `FIREBASE_SERVICE_ACCOUNT_JSON` | Firebase Admin credentials |

Integration-service sets:

```text
CONNECTION_NOTIFY_WEBHOOK_URL=https://<notification-host>/internal/connection-ready
CONNECTION_NOTIFY_WEBHOOK_SECRET=<same as WEBHOOK_SECRET>
```

## Local run

```bash
npm install
npm test
npm start
```

## Payload (from integration-service)

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

Push uses `recipient_user_ids` → `device_tokens` table. Email delivery can be added later (Resend/SendGrid).

---

Part of the [SharingBridge](https://github.com/sharingbridge/sharingbridge) ecosystem.
