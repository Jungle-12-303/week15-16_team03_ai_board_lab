package com.jungle_choi.namanmu.security;

import com.jungle_choi.namanmu.domain.user.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiRequestRateLimitService {

    private final ConcurrentMap<String, RequestWindow> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final Duration windowDuration;

    public AiRequestRateLimitService(
            @Value("${app.ai-rate-limit.max-requests:20}") int maxRequests,
            @Value("${app.ai-rate-limit.window-seconds:60}") long windowSeconds) {
        this.maxRequests = Math.max(1, maxRequests);
        this.windowDuration = Duration.ofSeconds(Math.max(1, windowSeconds));
    }

    public void assertAllowed(User user, String scope) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String key = "%s:%s".formatted(userKey(user), normalizeScope(scope));
        Instant now = Instant.now();

        windows.compute(key, (ignored, currentWindow) -> {
            RequestWindow window = currentWindow;

            if (window == null || window.isExpired(now, windowDuration)) {
                window = new RequestWindow(now, 0);
            }

            if (window.count() >= maxRequests) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many AI requests. Try again later.");
            }

            return new RequestWindow(window.startedAt(), window.count() + 1);
        });
    }

    private static String userKey(User user) {
        if (user.getId() != null) {
            return String.valueOf(user.getId());
        }

        return user.getEmail();
    }

    private static String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "default";
        }

        return scope.trim().toLowerCase(Locale.ROOT);
    }

    private record RequestWindow(Instant startedAt, int count) {

        private boolean isExpired(Instant now, Duration windowDuration) {
            return !startedAt.plus(windowDuration).isAfter(now);
        }
    }
}
