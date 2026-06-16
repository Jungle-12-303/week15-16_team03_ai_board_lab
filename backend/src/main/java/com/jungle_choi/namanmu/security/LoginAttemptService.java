package com.jungle_choi.namanmu.security;

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
public class LoginAttemptService {

    private final ConcurrentMap<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration window;
    private final Duration lockDuration;

    public LoginAttemptService(
            @Value("${app.login-rate-limit.max-failures:5}") int maxFailures,
            @Value("${app.login-rate-limit.window-seconds:600}") long windowSeconds,
            @Value("${app.login-rate-limit.lock-seconds:300}") long lockSeconds) {
        this.maxFailures = Math.max(1, maxFailures);
        this.window = Duration.ofSeconds(Math.max(1, windowSeconds));
        this.lockDuration = Duration.ofSeconds(Math.max(1, lockSeconds));
    }

    public void assertLoginAllowed(String loginKey) {
        String key = normalize(loginKey);
        Instant now = Instant.now();
        LoginAttempt attempt = attempts.get(key);

        if (attempt == null) {
            return;
        }

        if (attempt.isLocked(now)) {
            throw lockedException();
        }

        if (attempt.isWindowExpired(now, window)) {
            attempts.remove(key, attempt);
        }
    }

    public void recordFailure(String loginKey) {
        String key = normalize(loginKey);
        Instant now = Instant.now();

        attempts.compute(key, (ignored, current) -> {
            LoginAttempt base = current;

            if (base == null || base.isWindowExpired(now, window)) {
                base = new LoginAttempt(0, now, null);
            }

            if (base.isLocked(now)) {
                return base;
            }

            int nextFailures = base.failures() + 1;
            Instant lockedUntil = nextFailures >= maxFailures
                    ? now.plus(lockDuration)
                    : null;

            return new LoginAttempt(nextFailures, base.firstFailureAt(), lockedUntil);
        });
    }

    public void recordSuccess(String loginKey) {
        attempts.remove(normalize(loginKey));
    }

    private static String normalize(String loginKey) {
        if (loginKey == null || loginKey.isBlank()) {
            return "anonymous";
        }

        return loginKey.trim().toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException lockedException() {
        return new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many failed login attempts. Try again later.");
    }

    private record LoginAttempt(
            int failures,
            Instant firstFailureAt,
            Instant lockedUntil) {

        private boolean isLocked(Instant now) {
            return lockedUntil != null && lockedUntil.isAfter(now);
        }

        private boolean isWindowExpired(Instant now, Duration window) {
            return firstFailureAt.plus(window).isBefore(now);
        }
    }
}
