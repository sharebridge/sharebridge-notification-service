package org.sharingbridge.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sharingbridge.notification.config.DataAccessProperties;
import org.sharingbridge.notification.repository.DeviceTokenRepository;
import org.sharingbridge.notification.web.ConnectionReadyController.ConnectionReadyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ConnectionReadyService {

    private final DeviceTokenRepository tokens;
    private final FcmPushService push;
    private final DataAccessProperties dataAccess;

    public ConnectionReadyService(
            @Autowired(required = false) DeviceTokenRepository tokens,
            FcmPushService push,
            DataAccessProperties dataAccess) {
        this.tokens = tokens;
        this.push = push;
        this.dataAccess = dataAccess;
    }

    public Map<String, Object> handle(ConnectionReadyRequest payload) {
        if (payload == null || !"connection_ready".equals(payload.type())) {
            throw new NotifyException(HttpStatus.BAD_REQUEST, "invalid_type", "type must be connection_ready.");
        }
        String orderCode = payload.order_code() == null ? "" : payload.order_code().trim();
        if (orderCode.isEmpty()) {
            throw new NotifyException(HttpStatus.BAD_REQUEST, "invalid_order_code", "order_code is required.");
        }

        List<String> userIds = payload.recipient_user_ids() == null
                ? List.of()
                : payload.recipient_user_ids();

        List<String> fcmTokens = List.of();
        if (tokens != null) {
            fcmTokens = withRetry(() -> tokens.findTokensByUserIds(userIds));
        }

        FcmPushService.PushResult result;
        try {
            result = push.sendConnectionReady(fcmTokens, orderCode, payload.subject(), payload.text());
        } catch (FirebaseMessagingException ex) {
            throw new NotifyException(HttpStatus.INTERNAL_SERVER_ERROR, "notify_error", ex.getMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("order_code", orderCode);
        body.put("recipient_user_ids", userIds.size());
        body.put("fcm_tokens", fcmTokens.size());
        body.put("push_sent", result.sent());
        body.put("push_failed", result.failed());
        body.put("push_skipped", result.skipped());
        return body;
    }

    private <T> T withRetry(java.util.concurrent.Callable<T> action) {
        int max = dataAccess.getRetryMaxAttempts();
        int base = dataAccess.getRetryBaseDelayMs();
        Exception last = null;
        for (int attempt = 1; attempt <= max; attempt++) {
            try {
                return action.call();
            } catch (Exception ex) {
                last = ex;
                if (attempt >= max) {
                    break;
                }
                long delay = (long) base * attempt * attempt;
                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new NotifyException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "database_unavailable",
                last == null ? "Database unavailable." : last.getMessage());
    }
}
