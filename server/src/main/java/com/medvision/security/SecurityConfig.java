package com.medvision.security;

import com.medvision.common.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.*;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("http://localhost:3000"));
        cors.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        cors.setAllowedHeaders(List.of(
                "Content-Type", "Accept", "X-CSRF-TOKEN"
        ));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper mapper,
            SecurityContextRepository contextRepository,
            @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsSource))
                .csrf(csrf -> {})
                .securityContext(context -> context
                        .securityContextRepository(contextRepository)
                        .requireExplicitSave(true))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/health",
                                "/api/auth/csrf",
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(mapper, request, response,
                                        401, "UNAUTHORIZED",
                                        "Tizimga kirish talab qilinadi."))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(mapper, request, response,
                                        403, "ACCESS_DENIED",
                                        "Ruxsat yoki CSRF token noto‘g‘ri.")))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("MEDVISION_SESSION")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(204)));

        return http.build();
    }

    private static void writeError(
            ObjectMapper mapper,
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError error = new ApiError(
                Instant.now(),
                status,
                code,
                message,
                request.getRequestURI(),
                List.of()
        );

        response.getWriter().write(mapper.writeValueAsString(error));
    }
}