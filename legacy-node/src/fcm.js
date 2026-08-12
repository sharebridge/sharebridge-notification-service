import admin from "firebase-admin";
import { readFileSync } from "node:fs";

let messagingInstance = null;

export function initFirebaseAdmin() {
  if (messagingInstance) {
    return messagingInstance;
  }
  const jsonPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH?.trim();
  const jsonInline = process.env.FIREBASE_SERVICE_ACCOUNT_JSON?.trim();
  if (!jsonPath && !jsonInline) {
    console.warn(
      "[fcm] FIREBASE_SERVICE_ACCOUNT_PATH unset — push delivery disabled"
    );
    return null;
  }
  const credential = jsonPath
    ? admin.credential.cert(JSON.parse(readFileSync(jsonPath, "utf8")))
    : admin.credential.cert(JSON.parse(jsonInline));
  if (!admin.apps.length) {
    admin.initializeApp({ credential });
  }
  messagingInstance = admin.messaging();
  return messagingInstance;
}

export async function sendConnectionReadyPush({
  messaging,
  tokens,
  orderCode,
  title,
  body
}) {
  if (!messaging || !Array.isArray(tokens) || tokens.length === 0) {
    return { sent: 0, skipped: true };
  }
  const response = await messaging.sendEachForMulticast({
    tokens,
    notification: {
      title: title || `Order ${orderCode} ready`,
      body:
        body ||
        "Open SharingBridge → Actions to view your connection."
    },
    data: {
      type: "connection_ready",
      order_code: String(orderCode ?? "")
    }
  });
  return {
    sent: response.successCount,
    failed: response.failureCount
  };
}
