package org.sharingbridge.notification.web;

import java.util.Map;
import org.sharingbridge.notification.config.DataAccessProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final DataAccessProperties dataAccess;
    private final String databaseUrl;

    public HealthController(
            DataAccessProperties dataAccess,
            @Value("${DATABASE_URL:}") String databaseUrl) {
        this.dataAccess = dataAccess;
        this.databaseUrl = firstNonBlank(databaseUrl, System.getenv("DATABASE_URL"));
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "notification-service",
                "config", Map.of(
                        "database_url_set", databaseUrl != null && !databaseUrl.isBlank(),
                        "webhook_secret_set",
                        System.getenv("WEBHOOK_SECRET") != null
                                && !System.getenv("WEBHOOK_SECRET").isBlank(),
                        "data_access", dataAccess.toPublicConfig()));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }
}
