package com.medvision.auth;

import com.medvision.common.error.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String dummyHash;
    private final LoginAttemptLimiter limiter;

    public AuthService(UserRepository users, PasswordEncoder encoder, LoginAttemptLimiter limiter) {
        this.users = users;
        this.encoder = encoder;
        this.limiter = limiter;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        validatePasswordBytes(request.password());
        String email = normalizeEmail(request.email());

        if (users.existsByEmail(email)) {
            throw emailTaken();
        }

        User user = new User(
                request.name(),
                email,
                encoder.encode(request.password())
        );

        try {
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            // PostgreSQL unique violation, including concurrent registrations.
            if (isUniqueViolation(ex)) {
                throw emailTaken();
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request, String remoteAddress) {
        limiter.check(normalizeEmail(request.email()), remoteAddress);
        validatePasswordBytes(request.password());

        User user = users.findByEmail(normalizeEmail(request.email()))
                .orElse(null);

        String hash = user == null ? dummyHash : user.getPasswordHash();
        boolean valid = encoder.matches(request.password(), hash);

        if (user == null || !valid) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "Email yoki parol noto'g'ri."
            );
        }

        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(String userId) {
        return users.findById(UUID.fromString(userId))
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "Foydalanuvchi topilmadi."
                ));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validatePasswordBytes(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_TOO_LONG",
                    "Parol UTF-8 formatida 72 baytdan oshmasligi kerak."
            );
        }
    }

    private static boolean isUniqueViolation(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql
                    && "23505".equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static ApiException emailTaken() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_EXISTS",
                "Bu email bilan foydalanuvchi mavjud."
        );
    }
}
