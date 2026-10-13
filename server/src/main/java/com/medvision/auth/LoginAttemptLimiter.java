package com.medvision.auth;

import com.medvision.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

@Component
public class LoginAttemptLimiter {

    private static final long WINDOW =
            Duration.ofMinutes(15).toNanos();
    private static final int MAX_KEYS = 10_000;

    private final Map<String, Bucket> buckets = new HashMap<>();
    private final LongSupplier clock;

    public LoginAttemptLimiter() {
        this(System::nanoTime);
    }

    LoginAttemptLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    public synchronized void check(String email, String address) {
        long now = clock.getAsLong();

        buckets.entrySet().removeIf(
                entry -> now - entry.getValue().startedAt >= WINDOW
        );

        String emailKey = "email:" + email;
        String ipKey = "ip:" + address;

        int newKeys = (buckets.containsKey(emailKey) ? 0 : 1)
                + (buckets.containsKey(ipKey) ? 0 : 1);

        if (buckets.size() + newKeys > MAX_KEYS
                || count(emailKey) >= 10
                || count(ipKey) >= 100) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "LOGIN_RATE_LIMITED",
                    "Login urinishlari ko'p. Keyinroq qayta urinib ko'ring."
            );
        }

        increment(emailKey, now);
        increment(ipKey, now);
    }

    private int count(String key) {
        Bucket bucket = buckets.get(key);
        return bucket == null ? 0 : bucket.attempts;
    }

    private void increment(String key, long now) {
        Bucket bucket = buckets.computeIfAbsent(
                key, ignored -> new Bucket(now)
        );
        bucket.attempts++;
    }

    private static final class Bucket {
        private final long startedAt;
        private int attempts;

        private Bucket(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}