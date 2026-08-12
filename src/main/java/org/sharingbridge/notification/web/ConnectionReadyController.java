package org.sharingbridge.notification.web;

import java.util.List;
import java.util.Map;
import org.sharingbridge.notification.service.ConnectionReadyService;
import org.sharingbridge.notification.service.NotifyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConnectionReadyController {

    private final ConnectionReadyService service;
    private final String webhookSecret;

    public ConnectionReadyController(
            ConnectionReadyService service,
            @Value("${WEBHOOK_SECRET:}") String webhookSecret) {
        this.service = service;
        this.webhookSecret = firstNonBlank(webhookSecret, System.getenv("WEBHOOK_SECRET"));
    }

    @PostMapping("/internal/connection-ready")
    public Map<String, Object> connectionReady(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String providedSecret,
            @RequestBody ConnectionReadyRequest body) {
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            if (providedSecret == null || !webhookSecret.equals(providedSecret)) {
                throw new NotifyException(HttpStatus.UNAUTHORIZED, "unauthorized", "Invalid webhook secret.");
            }
        }
        return service.handle(body);
    }

    @ExceptionHandler(NotifyException.class)
    public ResponseEntity<Map<String, String>> handleNotify(NotifyException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    public record ConnectionReadyRequest(
            String type,
            String order_code,
            List<String> recipient_user_ids,
            List<String> recipient_emails,
            String subject,
            String text) {}
}
