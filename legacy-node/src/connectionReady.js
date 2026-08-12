import { sendConnectionReadyPush } from "./fcm.js";

export async function lookupFcmTokens(pool, userIds) {
  if (!pool || !Array.isArray(userIds) || userIds.length === 0) {
    return [];
  }
  const unique = [
    ...new Set(
      userIds
        .map((id) => (typeof id === "string" ? id.trim() : ""))
        .filter(Boolean)
    )
  ];
  if (unique.length === 0) {
    return [];
  }
  const result = await pool.query(
    `SELECT fcm_token FROM device_tokens WHERE user_id = ANY($1::text[])`,
    [unique]
  );
  return [
    ...new Set(
      result.rows
        .map((row) => row.fcm_token)
        .filter((token) => typeof token === "string" && token.trim())
    )
  ];
}

export async function handleConnectionReady({ payload, pool, messaging }) {
  if (payload?.type !== "connection_ready") {
    const error = new Error("type must be connection_ready.");
    error.status = 400;
    error.code = "invalid_type";
    throw error;
  }
  const orderCode = String(payload.order_code ?? "").trim();
  if (!orderCode) {
    const error = new Error("order_code is required.");
    error.status = 400;
    error.code = "invalid_order_code";
    throw error;
  }

  const userIds = Array.isArray(payload.recipient_user_ids)
    ? payload.recipient_user_ids
    : [];
  const tokens = await lookupFcmTokens(pool, userIds);
  const push = await sendConnectionReadyPush({
    messaging,
    tokens,
    orderCode,
    title: payload.subject,
    body: payload.text
  });

  return {
    order_code: orderCode,
    recipient_user_ids: userIds.length,
    fcm_tokens: tokens.length,
    push_sent: push.sent ?? 0,
    push_failed: push.failed ?? 0,
    push_skipped: Boolean(push.skipped)
  };
}
