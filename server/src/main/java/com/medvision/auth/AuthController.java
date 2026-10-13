package com.medvision.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final SecurityContextRepository contexts;

    public AuthController(
            AuthService auth,
            SecurityContextRepository contexts
    ) {
        this.auth = auth;
        this.contexts = contexts;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auth.register(request));
    }

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        UserResponse user = auth.login(body, request.getRemoteAddr());

        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        user.id().toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        new ChangeSessionIdAuthenticationStrategy()
                .onAuthentication(authentication, request, response);

        new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository())
                .onAuthentication(authentication, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);

        return user;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return auth.me(authentication.getName());
    }
}
