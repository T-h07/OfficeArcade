package com.officearcade.server.admin.analytics.dto;

public record AnalyticsDepartmentInsightResponse(
        String departmentId,
        String departmentCode,
        String departmentDisplayName,
        boolean departmentActive,
        boolean unassignedBucket,
        int userCount,
        int activeUsersInRange,
        int matchParticipationsInRange,
        double averageRespect,
        double averageKarma
) {
}
