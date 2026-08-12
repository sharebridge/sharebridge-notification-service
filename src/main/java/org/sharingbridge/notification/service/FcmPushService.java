package org.sharingbridge.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FcmPushService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushService.class);

    private final FirebaseMessaging messaging;

    public FcmPushService() {
        this.messaging = initMessaging();
    }

    /** Test seam. */
    FcmPushService(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    private static FirebaseMessaging initMessaging() {
        try {
            String pathEnv = blankToNull(System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH"));
            String jsonEnv = blankToNull(System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON"));
            if (pathEnv == null && jsonEnv == null) {
                log.warn("FIREBASE_SERVICE_ACCOUNT_PATH/JSON unset — push delivery disabled");
                return null;
            }
            GoogleCredentials credentials = pathEnv != null
                    ? GoogleCredentials.fromStream(Files.newInputStream(Path.of(pathEnv)))
                    : GoogleCredentials.fromStream(
                            new ByteArrayInputStream(jsonEnv.getBytes(StandardCharsets.UTF_8)));
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(
                        FirebaseOptions.builder().setCredentials(credentials).build());
            }
            return FirebaseMessaging.getInstance();
        } catch (Exception ex) {
            log.error("Firebase Admin init failed — push delivery disabled", ex);
            return null;
        }
    }

    public PushResult sendConnectionReady(
            List<String> tokens, String orderCode, String title, String body)
            throws FirebaseMessagingException {
        if (messaging == null || tokens == null || tokens.isEmpty()) {
            return new PushResult(0, 0, true);
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", "connection_ready");
        data.put("order_code", orderCode == null ? "" : orderCode);

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title == null || title.isBlank()
                                ? "Order " + orderCode + " ready"
                                : title)
                        .setBody(body == null || body.isBlank()
                                ? "Open SharingBridge → Actions to view your connection."
                                : body)
                        .build())
                .putAllData(data)
                .build();

        BatchResponse response = messaging.sendEachForMulticast(message);
        return new PushResult(response.getSuccessCount(), response.getFailureCount(), false);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record PushResult(int sent, int failed, boolean skipped) {}
}
