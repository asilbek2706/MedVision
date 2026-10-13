package com.medvision.auth;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {

    @GetMapping("/api/auth/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(
                token.getHeaderName(),
                token.getParameterName(),
                token.getToken()
        );
    }

    public record CsrfResponse(
            String headerName,
            String parameterName,
            String token
    ) {
    }
}