package org.sharingbridge.notification.repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;

public class DeviceTokenRepository {

    private final JdbcTemplate jdbc;

    public DeviceTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> findTokensByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String id : userIds) {
            if (id != null && !id.isBlank()) {
                unique.add(id.trim());
            }
        }
        if (unique.isEmpty()) {
            return List.of();
        }

        StringBuilder placeholders = new StringBuilder();
        List<Object> args = new ArrayList<>();
        int i = 0;
        for (String id : unique) {
            if (i++ > 0) {
                placeholders.append(',');
            }
            placeholders.append('?');
            args.add(id);
        }

        List<String> tokens = jdbc.query(
                "SELECT fcm_token FROM device_tokens WHERE user_id IN (" + placeholders + ")",
                (rs, rowNum) -> rs.getString("fcm_token"),
                args.toArray());

        Set<String> out = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token != null && !token.isBlank()) {
                out.add(token.trim());
            }
        }
        return List.copyOf(out);
    }
}
