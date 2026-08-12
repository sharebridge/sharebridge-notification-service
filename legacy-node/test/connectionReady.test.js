import test from "node:test";
import assert from "node:assert/strict";
import { handleConnectionReady } from "../src/connectionReady.js";

test("handleConnectionReady rejects missing order_code", async () => {
  await assert.rejects(
    () =>
      handleConnectionReady({
        payload: { type: "connection_ready" },
        pool: null,
        messaging: null
      }),
    (error) => error.status === 400
  );
});

test("handleConnectionReady skips push when messaging unset", async () => {
  const result = await handleConnectionReady({
    payload: {
      type: "connection_ready",
      order_code: "SB-7K2M-9F3",
      recipient_user_ids: ["alice"]
    },
    pool: null,
    messaging: null
  });
  assert.equal(result.order_code, "SB-7K2M-9F3");
  assert.equal(result.push_skipped, true);
});
