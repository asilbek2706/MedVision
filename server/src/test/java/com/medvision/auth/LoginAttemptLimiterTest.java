package com.medvision.auth;

import com.medvision.common.error.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {

    @Test
    void emailLimitAppliesAcrossDifferentAddresses() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(() -> 0L);

        for (int i = 0; i < 10; i++) {
            limiter.check("user@example.test", "ip-" + i);
        }

        ApiException error = assertThrows(
                ApiException.class,
                () -> limiter.check("user@example.test", "another-ip")
        );

        assertEquals(429, error.getStatus().value());
        assertEquals("LOGIN_RATE_LIMITED", error.getCode());
    }

    @Test
    void addressLimitAppliesAcrossDifferentEmails() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(() -> 0L);

        for (int i = 0; i < 100; i++) {
            limiter.check("user-" + i + "@example.test", "same-ip");
        }

        assertThrows(
                ApiException.class,
                () -> limiter.check("new@example.test", "same-ip")
        );
    }

    @Test
    void limitExpiresAfterWindow() {
        AtomicLong now = new AtomicLong();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(now::get);

        for (int i = 0; i < 10; i++) {
            limiter.check("user@example.test", "same-ip");
        }

        now.set(Duration.ofMinutes(15).toNanos() - 1);

        assertThrows(
                ApiException.class,
                () -> limiter.check("user@example.test", "same-ip")
        );

        now.incrementAndGet();

        assertDoesNotThrow(
                () -> limiter.check("user@example.test", "same-ip")
        );
    }
}