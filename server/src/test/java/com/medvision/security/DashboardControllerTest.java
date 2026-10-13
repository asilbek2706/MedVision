package com.medvision.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardControllerTest {

    @Test
    void returnsStatisticsForAuthenticatedUser() {
        OwnedResourceService resources = mock(OwnedResourceService.class);
        DashboardController controller = new DashboardController(resources);

        UUID userId = UUID.randomUUID();

        when(resources.dashboardStats(userId))
                .thenReturn(new DashboardStatsResponse(12, 9, 2));

        var authentication = new UsernamePasswordAuthenticationToken(
                userId.toString(), null
        );

        DashboardStatsResponse response = controller.stats(authentication);

        assertEquals(12, response.totalImages());
        assertEquals(9, response.completedAnalyses());
        assertEquals(2, response.failedAnalyses());

        verify(resources).dashboardStats(userId);
        verifyNoMoreInteractions(resources);
    }
}
