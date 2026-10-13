package com.medvision.security;

import com.medvision.auth.*;
import com.medvision.image.*;
import com.medvision.analysis.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OwnershipHttpTest {

    @LocalServerPort
    private int port;

    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository users;
    @Autowired private MedicalImageRepository images;
    @Autowired private AnalysisRepository analyses;
    @Autowired private PasswordEncoder encoder;

    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> imageIds = new ArrayList<>();
    private final List<UUID> analysisIds = new ArrayList<>();

    private static final String PASSWORD = "MedVision-Test-2026!";

    @Test
    void authenticationAndOwnershipAreEnforcedOverHttp() throws Exception {
        Browser a = new Browser();
        Browser b = new Browser();

        String emailA = UUID.randomUUID() + "@example.test";
        String emailB = UUID.randomUUID() + "@example.test";

        assertEquals(401, get(a, "/api/images").statusCode());

        String csrfA = csrf(a);
        String csrfB = csrf(b);

        var rejected = post(a, "/api/auth/register",
                registration(emailA), null);
        assertEquals(403, rejected.statusCode());

        UserResponse userA = register(a, emailA, csrfA);
        UserResponse userB = register(b, emailB, csrfB);

        User storedA = users.findById(userA.id()).orElseThrow();
        assertNotEquals(PASSWORD, storedA.getPasswordHash());
        assertTrue(storedA.getPasswordHash().startsWith("$2"));
        assertTrue(encoder.matches(PASSWORD, storedA.getPasswordHash()));

        var duplicate = post(a, "/api/auth/register",
                registration(emailA.toUpperCase(Locale.ROOT)), csrfA);
        assertEquals(409, duplicate.statusCode());

        var wrongKnown = post(a, "/api/auth/login",
                Map.of("email", emailA, "password", "Wrong-Password!"), csrfA);
        var wrongUnknown = post(a, "/api/auth/login",
                Map.of("email", UUID.randomUUID() + "@example.test",
                        "password", "Wrong-Password!"), csrfA);

        assertEquals(401, wrongKnown.statusCode());
        assertEquals(401, wrongUnknown.statusCode());
        assertEquals(
                mapper.readTree(wrongKnown.body()).get("code").asText(),
                mapper.readTree(wrongUnknown.body()).get("code").asText()
        );
        assertEquals(
                mapper.readTree(wrongKnown.body()).get("message").asText(),
                mapper.readTree(wrongUnknown.body()).get("message").asText()
        );

        String beforeLogin = a.sessionId();

        var loginA = post(a, "/api/auth/login",
                Map.of("email", emailA, "password", PASSWORD), csrfA);
        assertEquals(200, loginA.statusCode());
        assertFalse(loginA.body().contains("passwordHash"));
        assertNotEquals(beforeLogin, a.sessionId());

        var loginB = post(b, "/api/auth/login",
                Map.of("email", emailB, "password", PASSWORD), csrfB);
        assertEquals(200, loginB.statusCode());

        csrfA = csrf(a);
        csrfB = csrf(b);

        var meA = get(a, "/api/auth/me");
        assertEquals(200, meA.statusCode());
        assertEquals(userA.id().toString(),
                mapper.readTree(meA.body()).get("id").asText());

        MedicalImage imageA = fixtureImage(userA.id(), "owner-A.png");
        MedicalImage imageB = fixtureImage(userB.id(), "owner-B.png");
        Analysis analysisA = fixtureAnalysis(imageA);
        Analysis analysisB = fixtureAnalysis(imageB);

        assertEquals(200, get(a, "/api/images/" + imageA.getId()).statusCode());
        assertEquals(200, get(b, "/api/images/" + imageB.getId()).statusCode());
        assertEquals(200, get(a, "/api/analyses/" + analysisA.getId()).statusCode());
        assertEquals(200, get(b, "/api/analyses/" + analysisB.getId()).statusCode());

        assertNotFound(get(a, "/api/images/" + imageB.getId()));
        assertNotFound(get(b, "/api/images/" + imageA.getId()));
        assertNotFound(get(a, "/api/analyses/" + analysisB.getId()));
        assertNotFound(get(b, "/api/analyses/" + analysisA.getId()));

        assertNotFound(get(a, "/api/images/" + imageB.getId() + "/analyses"));
        assertNotFound(get(b, "/api/images/" + imageA.getId() + "/analyses"));

        var nested = get(a, "/api/images/" + imageA.getId() + "/analyses");
        assertEquals(200, nested.statusCode());
        assertTrue(nested.body().contains(analysisA.getId().toString()));
        assertFalse(nested.body().contains(analysisB.getId().toString()));

        var imageList = get(a, "/api/images?userId=" + userB.id());
        assertEquals(200, imageList.statusCode());
        assertTrue(imageList.body().contains(imageA.getId().toString()));
        assertFalse(imageList.body().contains(imageB.getId().toString()));
        assertFalse(imageList.body().contains("storageKey"));

        var analysisList = get(a, "/api/analyses?userId=" + userB.id());
        assertEquals(200, analysisList.statusCode());
        assertTrue(analysisList.body().contains(analysisA.getId().toString()));
        assertFalse(analysisList.body().contains(analysisB.getId().toString()));
        assertFalse(analysisList.body().contains("maskKey"));
        assertFalse(analysisList.body().contains("overlayKey"));

        assertNotFound(get(a, "/api/images/" + UUID.randomUUID()));
        assertEquals(400, get(a, "/api/images?size=101").statusCode());
        assertEquals(400, get(a, "/api/analyses?page=-1").statusCode());
        assertEquals(400, get(a, "/api/images/not-a-uuid").statusCode());

        String oldCookie = "MEDVISION_SESSION=" + a.sessionId();

        assertEquals(204, post(a, "/api/auth/logout", null, csrfA).statusCode());
        assertEquals(401, get(a, "/api/auth/me").statusCode());
        assertEquals(401, get(a, "/api/images/" + imageA.getId()).statusCode());

        HttpRequest replay = HttpRequest.newBuilder(uri("/api/auth/me"))
                .timeout(Duration.ofSeconds(15))
                .header("Cookie", oldCookie)
                .GET()
                .build();

        var replayResponse = HttpClient.newHttpClient().send(
                replay, HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(401, replayResponse.statusCode());

        // B's independent session must remain valid.
        assertEquals(200, get(b, "/api/auth/me").statusCode());
    }

    private Map<String, String> registration(String email) {
        return Map.of(
                "name", "Ownership Test",
                "email", email,
                "password", PASSWORD
        );
    }

    private UserResponse register(Browser browser, String email, String token)
            throws Exception {
        var response = post(browser, "/api/auth/register",
                registration(email), token);
        assertEquals(201, response.statusCode(), response.body());

        UserResponse user = mapper.readValue(response.body(), UserResponse.class);
        userIds.add(user.id());
        return user;
    }

    private MedicalImage fixtureImage(UUID userId, String name) {
        MedicalImage image = images.saveAndFlush(new MedicalImage(
                users.findById(userId).orElseThrow(),
                name,
                "ownership-test/" + UUID.randomUUID(),
                "image/png", 100, 10, 10
        ));
        imageIds.add(image.getId());
        return image;
    }

    private Analysis fixtureAnalysis(MedicalImage image) {
        Analysis analysis = analyses.saveAndFlush(new Analysis(
                image, "OTSU", "Test tahlil", Map.of("blurKernelSize", 5, "invert", false)
        ));
        analysisIds.add(analysis.getId());
        return analysis;
    }

    private void assertNotFound(HttpResponse<String> response) throws Exception {
        assertEquals(404, response.statusCode(), response.body());
        assertEquals("NOT_FOUND",
                mapper.readTree(response.body()).get("code").asText());
        assertFalse(response.body().contains("storageKey"));
        assertFalse(response.body().contains("passwordHash"));
    }

    private String csrf(Browser browser) throws Exception {
        var response = get(browser, "/api/auth/csrf");
        assertEquals(200, response.statusCode(), response.body());
        return mapper.readTree(response.body()).get("token").asText();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> get(Browser browser, String path)
            throws Exception {
        return browser.client.send(
                HttpRequest.newBuilder(uri(path))
                        .timeout(Duration.ofSeconds(15))
                        .header("Accept", "application/json")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> post(
            Browser browser, String path, Object body, String token
    ) throws Exception {
        var builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");

        if (token != null) {
            builder.header("X-CSRF-TOKEN", token);
        }

        if (body != null) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(body)
                    ));
        } else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }

        return browser.client.send(
                builder.build(), HttpResponse.BodyHandlers.ofString()
        );
    }

    @AfterEach
    void cleanupOnlyThisTestsFixtures() {
        analyses.deleteAllById(analysisIds);
        images.deleteAllById(imageIds);
        users.deleteAllById(userIds);
    }

    private static final class Browser {
        private final CookieManager cookies =
                new CookieManager(null, CookiePolicy.ACCEPT_ALL);

        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        private String sessionId() {
            return cookies.getCookieStore().getCookies().stream()
                    .filter(cookie -> "MEDVISION_SESSION".equals(cookie.getName()))
                    .map(java.net.HttpCookie::getValue)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Session cookie missing"));
        }
    }
}