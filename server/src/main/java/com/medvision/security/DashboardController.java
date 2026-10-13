package com.medvision.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final OwnedResourceService resources;

    public DashboardController(OwnedResourceService resources) {
        this.resources = resources;
    }

    @GetMapping("/stats")
    public DashboardStatsResponse stats(Authentication authentication) {
        UUID ownerId = UUID.fromString(authentication.getName());
        return resources.dashboardStats(ownerId);
    }
}
