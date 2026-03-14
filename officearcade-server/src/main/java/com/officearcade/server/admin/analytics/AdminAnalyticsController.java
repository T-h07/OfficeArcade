package com.officearcade.server.admin.analytics;

import com.officearcade.server.admin.analytics.dto.AdminAnalyticsDashboardResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @GetMapping("/dashboard")
    public AdminAnalyticsDashboardResponse getDashboard(
            @RequestParam(name = "range", defaultValue = "7d") String range,
            @RequestParam(name = "departmentId", required = false) String departmentId
    ) {
        return adminAnalyticsService.getDashboard(range, departmentId);
    }
}
