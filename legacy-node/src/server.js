import "dotenv/config";
import { createServer } from "node:http";
import { pathToFileURL } from "node:url";
import pg from "pg";
import { handleConnectionReady } from "./connectionReady.js";
import { initFirebaseAdmin } from "./fcm.js";

const DEFAULT_PORT = Number(process.env.PORT || 8092);

function sendJson(res, statusCode, body) {
  res.writeHead(statusCode, { "content-type": "application/json" });
  res.end(JSON.stringify(body));
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let rawBody = "";
    req.on("data", (chunk) => {
      rawBody += chunk;
    });
    req.on("end", () => {
      try {
        resolve(JSON.parse(rawBody || "{}"));
      } catch {
        reject(new Error("invalid_json"));
      }
    });
    req.on("error", reject);
  });
}

function verifyWebhookSecret(req) {
  const expected = process.env.WEBHOOK_SECRET?.trim();
  if (!expected) {
    return true;
  }
  const provided = req.headers["x-webhook-secret"];
  return typeof provided === "string" && provided === expected;
}

export function createNotificationServer({ pool, messaging = null } = {}) {
  return createServer(async (req, res) => {
    if (req.method === "GET" && req.url === "/health") {
      return sendJson(res, 200, { status: "ok" });
    }

    if (
      req.method === "POST" &&
      req.url === "/internal/connection-ready"
    ) {
      if (!verifyWebhookSecret(req)) {
        return sendJson(res, 401, {
          code: "unauthorized",
          message: "Invalid webhook secret."
        });
      }
      try {
        const payload = await readJsonBody(req);
        const result = await handleConnectionReady({
          payload,
          pool,
          messaging
        });
        return sendJson(res, 200, result);
      } catch (error) {
        if (error?.message === "invalid_json") {
          return sendJson(res, 400, {
            code: "invalid_json",
            message: "Request body must be valid JSON."
          });
        }
        return sendJson(res, error?.status || 500, {
          code: error?.code || "notify_error",
          message: error?.message || "Notification failed."
        });
      }
    }

    return sendJson(res, 404, {
      code: "not_found",
      message: "Route not found."
    });
  });
}

const isMainModule =
  process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href;

if (isMainModule) {
  const databaseUrl = process.env.DATABASE_URL?.trim();
  if (!databaseUrl) {
    console.error("DATABASE_URL is required.");
    process.exit(1);
  }
  const pool = new pg.Pool({ connectionString: databaseUrl });
  const messaging = initFirebaseAdmin();
  const server = createNotificationServer({ pool, messaging });
  server.listen(DEFAULT_PORT, () => {
    console.log(`Notification service listening on ${DEFAULT_PORT}`);
  });
}
