package org.sharingbridge.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sharingbridge.notification.config.DataAccessProperties;
import org.sharingbridge.notification.web.ConnectionReadyController.ConnectionReadyRequest;
import org.springframework.http.HttpStatus;

class ConnectionReadyServiceTest {

    @Test
    void rejectsMissingOrderCode() {
        ConnectionReadyService service = new ConnectionReadyService(
                null, new FcmPushService(null), DataAccessProperties.fromEnvironment());
        NotifyException ex = assertThrows(
                NotifyException.class,
                () -> service.handle(new ConnectionReadyRequest(
                        "connection_ready", null, List.of(), List.of(), null, null)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("invalid_order_code", ex.getCode());
    }

    @Test
    void skipsPushWhenMessagingUnset() {
        ConnectionReadyService service = new ConnectionReadyService(
                null, new FcmPushService(null), DataAccessProperties.fromEnvironment());
        Map<String, Object> result = service.handle(new ConnectionReadyRequest(
                "connection_ready",
                "SB-7K2M-9F3",
                List.of("alice"),
                List.of(),
                null,
                null));
        assertEquals("SB-7K2M-9F3", result.get("order_code"));
        assertTrue((Boolean) result.get("push_skipped"));
    }
}
